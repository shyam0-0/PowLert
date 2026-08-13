package com.chargealert.app.domain

/**
 * Pure threshold decision logic, isolated from Android framework types so it
 * can be unit tested without a device/emulator.
 *
 * STATUS_FULL is deliberately not part of this decision — see plan.md
 * section 10 for why percentage + charging state is the sole authoritative
 * trigger.
 */
object ThresholdEvaluator {
    fun isThresholdReached(percentage: Int, isCharging: Boolean, threshold: Int): Boolean {
        return isCharging && percentage >= threshold
    }
}
