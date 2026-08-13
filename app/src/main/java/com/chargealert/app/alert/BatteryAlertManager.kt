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

/**
 * Dispatches the actual alert mechanisms (notification/sound/vibration)
 * once AlertEngine has decided an alert should fire. Each mechanism is
 * independently gated by AlertSettings; a mechanism that is off is simply
 * skipped, never treated as an error.
 */
object BatteryAlertManager {

    private const val TAG = "BatteryAlertManager"
    private const val ALERT_NOTIFICATION_ID = 2001

    fun triggerAlert(context: Context, settings: AlertSettings, batteryPercentage: Int) {
        if (settings.notificationEnabled) {
            postNotification(context, batteryPercentage)
        }
        if (settings.soundEnabled) {
            playSound(context, settings.selectedSound)
        }
        if (settings.vibrationEnabled) {
            vibrate(context)
        }
    }

    private fun postNotification(context: Context, batteryPercentage: Int) {
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

        val notification = NotificationCompat.Builder(context, ChargeAlertApplication.ALERT_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.alert_notification_title))
            .setContentText(context.getString(R.string.alert_notification_text, batteryPercentage))
            .setContentIntent(openAppIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        NotificationManagerCompat.from(context).notify(ALERT_NOTIFICATION_ID, notification)
        Log.d(TAG, "Notification triggered")
    }

    private fun playSound(context: Context, selectedSound: String) {
        try {
            val uri = if (selectedSound == AlertSettings.DEFAULT_SOUND) {
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            } else {
                Uri.parse(selectedSound)
            }
            val ringtone = RingtoneManager.getRingtone(context, uri)
            ringtone?.play()
            Log.d(TAG, "Sound triggered")
        } catch (e: Exception) {
            Log.w(TAG, "Sound playback failed", e)
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
