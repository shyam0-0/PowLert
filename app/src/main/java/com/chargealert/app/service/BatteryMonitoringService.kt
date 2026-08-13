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
import com.chargealert.app.domain.AlertEngine
import com.chargealert.app.domain.AlertSettings
import com.chargealert.app.domain.BatteryState
import com.chargealert.app.domain.ChargingSessionState
import com.chargealert.app.domain.ThresholdEvaluator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/**
 * Foreground service that stays alive for as long as the alert feature is
 * enabled (not only while charging). See plan.md section 15.2 for why the
 * session-scoped start/stop trigger was replaced with this model.
 *
 * The service does NOT stop when charging stops -- only when the user
 * disables the alert. It coordinates monitoring and delegates the alert
 * decision to AlertEngine and alert dispatch to BatteryAlertManager; it does
 * not contain alert logic itself.
 */
class BatteryMonitoringService : Service() {

    private val job = SupervisorJob()
    private val scope = CoroutineScope(job)

    private lateinit var batteryRepository: BatteryRepository
    private lateinit var preferencesRepository: UserPreferencesRepository

    private var sessionState: ChargingSessionState = ChargingSessionState.NotCharging
    private var isObserving = false

    override fun onCreate() {
        super.onCreate()
        val app = application as ChargeAlertApplication
        batteryRepository = app.batteryRepository
        preferencesRepository = app.preferencesRepository
        Log.d(TAG, "Foreground service created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            buildNotification(idle = true),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        )

        // onStartCommand can be re-invoked (e.g. after a START_STICKY restart);
        // only subscribe once per living service instance.
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
        val thresholdReached = ThresholdEvaluator.isThresholdReached(
            percentage = state.percentage,
            isCharging = state.isCharging,
            threshold = settings.threshold
        )

        Log.d(
            TAG,
            "Battery update: percentage=${state.percentage} charging=${state.isCharging} " +
                "threshold=${settings.threshold} thresholdReached=$thresholdReached"
        )

        updateSessionState(state, settings)
        updateNotification(state)
    }

    private fun updateSessionState(state: BatteryState, settings: AlertSettings) {
        val previousState = sessionState
        val alreadyAlertedThisSession = previousState is ChargingSessionState.AlertTriggered ||
            previousState is ChargingSessionState.WaitingForDisconnect

        val shouldAlert = AlertEngine.shouldAlert(
            percentage = state.percentage,
            isCharging = state.isCharging,
            threshold = settings.threshold,
            alreadyAlertedThisSession = alreadyAlertedThisSession
        )

        sessionState = when {
            !state.isCharging -> ChargingSessionState.NotCharging
            shouldAlert -> ChargingSessionState.AlertTriggered
            alreadyAlertedThisSession -> ChargingSessionState.WaitingForDisconnect
            else -> ChargingSessionState.Charging
        }

        if (previousState == sessionState) return
        Log.d(TAG, "Session state changed: $previousState -> $sessionState")

        when {
            !state.isCharging -> Log.d(TAG, "Charging session reset")
            shouldAlert -> {
                Log.d(TAG, "Threshold reached: percentage=${state.percentage} threshold=${settings.threshold}")
                BatteryAlertManager.triggerAlert(this, settings, state.percentage)
                Log.d(TAG, "Alert triggered")
            }
            alreadyAlertedThisSession -> Log.d(TAG, "Alert already triggered for current session")
        }
    }

    private fun updateNotification(state: BatteryState) {
        val notification = buildNotification(idle = !state.isCharging, batteryState = state)
        val manager = getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager
        manager.notify(NOTIFICATION_ID, notification)
    }

    private fun buildNotification(idle: Boolean, batteryState: BatteryState? = null): Notification {
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
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?) = null

    companion object {
        private const val TAG = "BatteryMonitoringSvc"
        private const val NOTIFICATION_ID = 1001

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
    }
}
