package com.chargealert.app.alert

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.chargealert.app.ChargeAlertApplication
import com.chargealert.app.MainActivity
import com.chargealert.app.R
import com.chargealert.app.domain.AlertSettings
import com.chargealert.app.service.BatteryMonitoringService

/**
 * Dispatches the actual alert mechanisms (notification/sound/vibration)
 * once RepeatAlertEngine has decided an alert should fire. Each mechanism is
 * independently gated by AlertSettings; a mechanism that is off is simply
 * skipped, never treated as an error.
 */
object BatteryAlertManager {

    private const val TAG = "BatteryAlertManager"
    const val ALERT_NOTIFICATION_ID = 2001

    /**
     * [repeatCount] 0 = initial alert, >0 = a repeat. [includeActions] is
     * false for Test Alert, which has no real session for STOP/SNOOZE to act on.
     */
    fun triggerAlert(
        context: Context,
        settings: AlertSettings,
        batteryPercentage: Int,
        repeatCount: Int = 0,
        includeActions: Boolean = true
    ) {
        if (settings.notificationEnabled) {
            postNotification(context, batteryPercentage, repeatCount, includeActions)
        }
        if (settings.soundEnabled) {
            playSound(context, settings.selectedSound)
        }
        if (settings.vibrationEnabled) {
            vibrate(context)
        }
    }

    fun dismissAlertNotification(context: Context) {
        NotificationManagerCompat.from(context).cancel(ALERT_NOTIFICATION_ID)
    }

    private fun postNotification(context: Context, batteryPercentage: Int, repeatCount: Int, includeActions: Boolean) {
        val hasPermission = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

        if (!hasPermission) {
            Log.d(TAG, "Notification skipped: POST_NOTIFICATIONS not granted")
            return
        }

        val openAppIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        val text = if (repeatCount == 0) {
            context.getString(R.string.alert_notification_text, batteryPercentage)
        } else {
            context.getString(R.string.alert_notification_text_repeat, batteryPercentage, repeatCount)
        }

        val builder = NotificationCompat.Builder(context, ChargeAlertApplication.ALERT_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.alert_notification_title))
            .setContentText(text)
            .setContentIntent(openAppIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        if (includeActions) {
            builder.addAction(
                0,
                context.getString(R.string.alert_action_stop),
                BatteryMonitoringService.stopAlertPendingIntent(context)
            )
            builder.addAction(
                0,
                context.getString(R.string.alert_action_snooze),
                BatteryMonitoringService.snoozeAlertPendingIntent(context)
            )
        }

        NotificationManagerCompat.from(context).notify(ALERT_NOTIFICATION_ID, builder.build())
        Log.d(TAG, "Notification triggered (repeatCount=$repeatCount)")
    }

    private fun playSound(context: Context, selectedSound: String) {
        val defaultUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val uri = if (selectedSound == AlertSettings.DEFAULT_SOUND) defaultUri else Uri.parse(selectedSound)

        val played = tryPlay(context, uri)
        if (!played && uri != defaultUri) {
            Log.w(TAG, "Selected sound unavailable, falling back to default")
            tryPlay(context, defaultUri)
        }
    }

    /** Returns true if playback was successfully started. Never throws. */
    private fun tryPlay(context: Context, uri: Uri): Boolean {
        return try {
            val ringtone = RingtoneManager.getRingtone(context, uri) ?: return false
            ringtone.play()
            Log.d(TAG, "Sound triggered")
            true
        } catch (e: Exception) {
            Log.w(TAG, "Sound playback failed for $uri", e)
            false
        }
    }

    private fun vibrate(context: Context) {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            manager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        if (!vibrator.hasVibrator()) {
            Log.d(TAG, "Vibration skipped: no vibrator on this device")
            return
        }

        vibrator.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE))
        Log.d(TAG, "Vibration triggered")
    }
}
