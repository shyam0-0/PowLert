package com.chargealert.app.ui

import com.chargealert.app.domain.AlertSettings
import com.chargealert.app.domain.BatteryState
import com.chargealert.app.domain.BatteryStatus
import com.chargealert.app.domain.ChargingSessionState
import com.chargealert.app.domain.ThresholdEvaluator

data class HomeUiState(
    val batteryState: BatteryState,
    val alertSettings: AlertSettings,
    val sessionState: ChargingSessionState,
    val notificationPermissionGranted: Boolean,
    val warningMessage: String?
) {
    val thresholdReached: Boolean
        get() = ThresholdEvaluator.isThresholdReached(
            batteryState.percentage,
            batteryState.isCharging,
            alertSettings.threshold
        )

    val noAlertMechanismEnabled: Boolean
        get() = !alertSettings.notificationEnabled && !alertSettings.soundEnabled && !alertSettings.vibrationEnabled

    /** True while an alert has fired and a repeat is pending -- drives the in-app Stop/Snooze fallback. */
    val isAlertActive: Boolean
        get() = sessionState is ChargingSessionState.WaitingForRepeat

    val activeAlertRepeatCount: Int
        get() = (sessionState as? ChargingSessionState.WaitingForRepeat)?.repeatCount ?: 0

    companion object {
        val Initial = HomeUiState(
            batteryState = BatteryState(percentage = 0, isCharging = false, status = BatteryStatus.UNKNOWN),
            alertSettings = AlertSettings(),
            sessionState = ChargingSessionState.NotCharging,
            notificationPermissionGranted = true,
            warningMessage = null
        )
    }
}
