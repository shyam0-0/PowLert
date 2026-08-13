package com.chargealert.app.ui

import com.chargealert.app.domain.AlertSettings
import com.chargealert.app.domain.BatteryState
import com.chargealert.app.domain.BatteryStatus
import com.chargealert.app.domain.ThresholdEvaluator

data class HomeUiState(
    val batteryState: BatteryState,
    val alertSettings: AlertSettings,
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

    companion object {
        val Initial = HomeUiState(
            batteryState = BatteryState(percentage = 0, isCharging = false, status = BatteryStatus.UNKNOWN),
            alertSettings = AlertSettings(),
            notificationPermissionGranted = true,
            warningMessage = null
        )
    }
}
