package com.chargealert.app.domain

/**
 * User-configurable alert preferences. Persisted via UserPreferencesRepository.
 */
data class AlertSettings(
    val alertEnabled: Boolean = false,
    val threshold: Int = 100,
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val notificationEnabled: Boolean = true,
    val selectedSound: String = DEFAULT_SOUND
) {
    companion object {
        /** Sentinel meaning "use the system default notification sound", resolved at play time. */
        const val DEFAULT_SOUND = "default"
    }
}
