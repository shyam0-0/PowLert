package com.chargealert.app.domain

/**
 * Decides WHETHER to alert. Delegates the raw threshold check to
 * ThresholdEvaluator and adds the "exactly once per charging session" rule.
 * No Android dependencies -- unit testable in isolation. Dispatching the
 * actual notification/sound/vibration is handled separately by
 * alert.BatteryAlertManager.
 */
object AlertEngine {
    fun shouldAlert(
        percentage: Int,
        isCharging: Boolean,
        threshold: Int,
        alreadyAlertedThisSession: Boolean
    ): Boolean {
        if (alreadyAlertedThisSession) return false
        return ThresholdEvaluator.isThresholdReached(percentage, isCharging, threshold)
    }
}
