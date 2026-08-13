package com.chargealert.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import com.chargealert.app.data.BatteryRepository
import com.chargealert.app.data.UserPreferencesRepository

class ChargeAlertApplication : Application() {

    val preferencesRepository by lazy { UserPreferencesRepository(this) }
    val batteryRepository by lazy { BatteryRepository(this) }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    /**
     * Two channels, not one, because they serve different purposes and need
     * different importance: the monitoring channel is a silent, ongoing
     * status notification, while the alert channel is the actual
     * threshold-reached alert and needs to be visible/heads-up. Merging them
     * would force one importance level onto both.
     */
    private fun createNotificationChannels() {
        val manager = getSystemService(NotificationManager::class.java)

        val monitoringChannel = NotificationChannel(
            MONITORING_CHANNEL_ID,
            getString(R.string.monitoring_channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = getString(R.string.monitoring_channel_description)
        }

        val alertChannel = NotificationChannel(
            ALERT_CHANNEL_ID,
            getString(R.string.alert_channel_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = getString(R.string.alert_channel_description)
        }

        manager.createNotificationChannel(monitoringChannel)
        manager.createNotificationChannel(alertChannel)
    }

    companion object {
        const val MONITORING_CHANNEL_ID = "battery_monitoring"
        const val ALERT_CHANNEL_ID = "battery_alerts"
    }
}
