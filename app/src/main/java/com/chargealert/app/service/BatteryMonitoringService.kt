package com.chargealert.app.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.chargealert.app.ChargeAlertApplication
import com.chargealert.app.MainActivity
import com.chargealert.app.R
import com.chargealert.app.alert.BatteryAlertManager
import com.chargealert.app.data.BatteryRepository
import com.chargealert.app.data.UserPreferencesRepository
import com.chargealert.app.domain.AlertSettings
import com.chargealert.app.domain.BatteryState
import com.chargealert.app.domain.ChargingSessionState
import com.chargealert.app.domain.RepeatAlertEngine
import com.chargealert.app.domain.ThresholdEvaluator
import com.chargealert.app.domain.Transition
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

/**
 * Foreground service that stays alive for as long as the alert feature is
 * enabled (not only while charging). See plan.md section 15.2 for why the
 * session-scoped start/stop trigger was replaced with this model.
 *
 * The service does NOT stop when charging stops -- only when the user
 * disables the alert. It coordinates monitoring and delegates the alert
 * decision to RepeatAlertEngine and alert dispatch to BatteryAlertManager;
 * it does not contain alert logic itself.
 *
 * Repeat/snooze scheduling (Phase 4) is a plain coroutine `delay()` inside
 * this service's own long-lived scope -- no WorkManager/AlarmManager. The
 * service is already expected to run continuously while monitoring is
 * enabled, so that's the smallest correct mechanism; see plan.md Phase 4
 * section 14. [repeatJob] is always cancelled before a new one is scheduled,
 * so a settings change or a duplicate battery event can never create a
 * second concurrent timer.
 */
class BatteryMonitoringService : Service() {

    private val job = SupervisorJob()
    private val scope = CoroutineScope(job)

    private lateinit var batteryRepository: BatteryRepository
    private lateinit var preferencesRepository: UserPreferencesRepository
    private lateinit var app: ChargeAlertApplication

    private var sessionState: ChargingSessionState = ChargingSessionState.NotCharging
    private var latestSettings: AlertSettings = AlertSettings()
    private var latestBatteryState: BatteryState = BatteryState(0, false, com.chargealert.app.domain.BatteryStatus.UNKNOWN)
    private var repeatJob: Job? = null
    private var isObserving = false

    override fun onCreate() {
        super.onCreate()
        app = application as ChargeAlertApplication
        batteryRepository = app.batteryRepository
        preferencesRepository = app.preferencesRepository
        Log.d(TAG, "Foreground service created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        ServiceCompat.startForeground(
            this,
            MONITORING_NOTIFICATION_ID,
            buildMonitoringNotification(idle = true),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        )

        when (intent?.action) {
            ACTION_STOP_ALERT -> handleStop()
            ACTION_SNOOZE_ALERT -> handleSnooze()
        }

        // onStartCommand can be re-invoked (e.g. after a START_STICKY restart,
        // or a STOP/SNOOZE action intent); only subscribe once per living
        // service instance.
        if (!isObserving) {
            observeBatteryState()
            isObserving = true
        }

        return START_STICKY
    }

    private fun observeBatteryState() {
        combine(
            batteryRepository.batteryStateUpdates(),
            preferencesRepository.settings
        ) { batteryState, settings -> batteryState to settings }
            .onEach { (batteryState, settings) -> onBatteryStateChanged(batteryState, settings) }
            .launchIn(scope)
    }

    private fun onBatteryStateChanged(state: BatteryState, settings: AlertSettings) {
        latestBatteryState = state
        latestSettings = settings

        val thresholdReached = ThresholdEvaluator.isThresholdReached(state.percentage, state.isCharging, settings.threshold)
        Log.d(
            TAG,
            "Battery update: percentage=${state.percentage} charging=${state.isCharging} " +
                "threshold=${settings.threshold} thresholdReached=$thresholdReached"
        )

        val transition = RepeatAlertEngine.onBatteryUpdate(sessionState, state.isCharging, thresholdReached, settings.repeatRule)
        applyTransition(transition, state.percentage)
    }

    private fun handleStop() {
        val transition = RepeatAlertEngine.onStop(sessionState)
        applyTransition(transition, latestBatteryState.percentage)
        if (transition.newState == ChargingSessionState.Acknowledged) {
            BatteryAlertManager.dismissAlertNotification(this)
            Log.d(TAG, "Alert acknowledged (STOP)")
        }
    }

    private fun handleSnooze() {
        val transition = RepeatAlertEngine.onSnooze(sessionState, latestSettings.repeatRule)
        applyTransition(transition, latestBatteryState.percentage)
        if (transition.scheduleDelayMinutes != null) {
            BatteryAlertManager.dismissAlertNotification(this)
            Log.d(TAG, "Alert snoozed for ${transition.scheduleDelayMinutes} min")
        }
    }

    private fun onRepeatTimerFired() {
        val transition = RepeatAlertEngine.onRepeatTimerFired(sessionState, latestSettings.repeatRule)
        applyTransition(transition, latestBatteryState.percentage)
    }

    private fun applyTransition(transition: Transition, batteryPercentage: Int) {
        val previousState = sessionState
        sessionState = transition.newState
        app.updateSessionState(sessionState)

        if (previousState != sessionState) {
            Log.d(TAG, "Session state changed: $previousState -> $sessionState")
        }

        if (transition.cancelSchedule) {
            repeatJob?.cancel()
            repeatJob = null
        }

        if (transition.fireAlert) {
            val repeatCount = transition.firingRepeatCount
            Log.d(TAG, "Threshold reached: percentage=$batteryPercentage threshold=${latestSettings.threshold}")
            BatteryAlertManager.triggerAlert(this, latestSettings, batteryPercentage, repeatCount)
            Log.d(TAG, "Alert triggered (repeatCount=$repeatCount)")
        }

        transition.scheduleDelayMinutes?.let { minutes ->
            scheduleRepeat(minutes)
        }

        if (sessionState == ChargingSessionState.NotCharging && previousState != ChargingSessionState.NotCharging) {
            // Disconnect: drop any stale STOP/SNOOZE affordance from the last alert.
            BatteryAlertManager.dismissAlertNotification(this)
            Log.d(TAG, "Charging session reset")
        }

        updateMonitoringNotification(latestBatteryState)
    }

    private fun scheduleRepeat(delayMinutes: Int) {
        repeatJob?.cancel()
        repeatJob = scope.launch {
            kotlinx.coroutines.delay(delayMinutes * 60_000L)
            onRepeatTimerFired()
        }
    }

    private fun updateMonitoringNotification(state: BatteryState) {
        val notification = buildMonitoringNotification(idle = !state.isCharging, batteryState = state)
        val manager = getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager
        manager.notify(MONITORING_NOTIFICATION_ID, notification)
    }

    private fun buildMonitoringNotification(idle: Boolean, batteryState: BatteryState? = null): Notification {
        val openAppIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        val title = if (idle) {
            getString(R.string.notification_idle_title)
        } else {
            getString(R.string.notification_charging_title)
        }

        val text = if (batteryState != null && !idle) {
            "${batteryState.percentage}% · ${batteryState.status}"
        } else {
            getString(R.string.notification_idle_text)
        }

        return NotificationCompat.Builder(this, ChargeAlertApplication.MONITORING_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(openAppIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()
    }

    override fun onDestroy() {
        Log.d(TAG, "Foreground service stopped")
        repeatJob?.cancel()
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?) = null

    companion object {
        private const val TAG = "BatteryMonitoringSvc"
        private const val MONITORING_NOTIFICATION_ID = 1001

        private const val ACTION_STOP_ALERT = "com.chargealert.app.action.STOP_ALERT"
        private const val ACTION_SNOOZE_ALERT = "com.chargealert.app.action.SNOOZE_ALERT"

        fun start(context: Context) {
            val intent = Intent(context, BatteryMonitoringService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, BatteryMonitoringService::class.java))
        }

        /** Sent directly by the in-app fallback UI (same path as the notification action). */
        fun sendStopAlert(context: Context) {
            actionIntent(context, ACTION_STOP_ALERT)
        }

        fun sendSnoozeAlert(context: Context) {
            actionIntent(context, ACTION_SNOOZE_ALERT)
        }

        fun stopAlertPendingIntent(context: Context): PendingIntent = actionPendingIntent(context, ACTION_STOP_ALERT, 1)

        fun snoozeAlertPendingIntent(context: Context): PendingIntent = actionPendingIntent(context, ACTION_SNOOZE_ALERT, 2)

        private fun actionIntent(context: Context, action: String) {
            val intent = Intent(context, BatteryMonitoringService::class.java).setAction(action)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        private fun actionPendingIntent(context: Context, action: String, requestCode: Int): PendingIntent {
            val intent = Intent(context, BatteryMonitoringService::class.java).setAction(action)
            return PendingIntent.getForegroundService(context, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        }
    }
}
