package com.chargealert.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.chargealert.app.ChargeAlertApplication
import com.chargealert.app.service.BatteryMonitoringService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * ACTION_BOOT_COMPLETED is exempt from both the implicit-broadcast manifest
 * restrictions and the foreground-service background-start restrictions
 * (plan.md section 15.2), so this is a safe way to resume monitoring after a
 * reboot -- PROVIDED the app has been launched at least once since install.
 *
 * Known Android platform limitation (not fixable by redesign): apps that have
 * never been launched, or that were force-stopped, are in a "stopped" state
 * in which the system withholds BOOT_COMPLETED entirely. If a user enables
 * the alert, then force-stops the app (or reboots before ever opening it
 * again after install), this receiver will not fire on the next boot. This
 * is standard Android behavior since Android 3.1 and applies to every app.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val app = context.applicationContext as ChargeAlertApplication
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val alertEnabled = app.preferencesRepository.settings.first().alertEnabled
                if (alertEnabled) {
                    BatteryMonitoringService.start(context)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
