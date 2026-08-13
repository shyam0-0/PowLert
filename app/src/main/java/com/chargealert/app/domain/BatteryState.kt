package com.chargealert.app.domain

/**
 * Snapshot of the device's battery/charging state at a point in time.
 */
data class BatteryState(
    val percentage: Int,
    val isCharging: Boolean,
    val status: BatteryStatus
)

enum class BatteryStatus {
    CHARGING,
    DISCHARGING,
    FULL,
    NOT_CHARGING,
    UNKNOWN
}
