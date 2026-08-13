package com.chargealert.app.domain

/**
 * A charging alert the user can't perceive is useless. This guards against a
 * settings change that would leave notification, sound, and vibration all
 * disabled at once -- the UI layer uses it to reject such a change rather
 * than silently persisting it.
 */
object AlertMechanismGuard {
    fun wouldDisableAllMechanisms(
        notificationEnabled: Boolean,
        soundEnabled: Boolean,
        vibrationEnabled: Boolean
    ): Boolean {
        return !notificationEnabled && !soundEnabled && !vibrationEnabled
    }
}
