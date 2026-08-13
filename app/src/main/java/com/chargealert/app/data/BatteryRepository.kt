package com.chargealert.app.data

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.core.content.ContextCompat
import com.chargealert.app.domain.BatteryState
import com.chargealert.app.domain.BatteryStatus
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Reads battery/charging state from ACTION_BATTERY_CHANGED.
 *
 * ACTION_BATTERY_CHANGED is a sticky broadcast: registering a receiver for it
 * with a null BroadcastReceiver returns the last broadcast immediately, which
 * is enough for a one-shot read (used by the UI). The service uses the Flow
 * variant, which keeps a real receiver registered for live updates.
 */
class BatteryRepository(private val context: Context) {

    fun getCurrentBatteryState(): BatteryState {
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val intent = context.registerReceiver(null, filter)
        return intent?.toBatteryState() ?: BatteryState(
            percentage = 0,
            isCharging = false,
            status = BatteryStatus.UNKNOWN
        )
    }

    fun batteryStateUpdates(): Flow<BatteryState> = callbackFlow {
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val receiver = object : android.content.BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                intent?.let { trySend(it.toBatteryState()) }
            }
        }
        ContextCompat.registerReceiver(
            context,
            receiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        trySend(getCurrentBatteryState())
        awaitClose { context.unregisterReceiver(receiver) }
    }

    private fun Intent.toBatteryState(): BatteryState {
        val level = getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        val percentage = if (level >= 0 && scale > 0) (level * 100) / scale else 0

        val statusExtra = getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        val isCharging = statusExtra == BatteryManager.BATTERY_STATUS_CHARGING ||
            statusExtra == BatteryManager.BATTERY_STATUS_FULL

        val status = when (statusExtra) {
            BatteryManager.BATTERY_STATUS_CHARGING -> BatteryStatus.CHARGING
            BatteryManager.BATTERY_STATUS_DISCHARGING -> BatteryStatus.DISCHARGING
            BatteryManager.BATTERY_STATUS_FULL -> BatteryStatus.FULL
            BatteryManager.BATTERY_STATUS_NOT_CHARGING -> BatteryStatus.NOT_CHARGING
            else -> BatteryStatus.UNKNOWN
        }

        return BatteryState(
            percentage = percentage,
            isCharging = isCharging,
            status = status
        )
    }
}
