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
    val selectedSound: String = DEFAULT_SOUND,
    val repeatEnabled: Boolean = false,
    val repeatIntervalMinutes: Int = 5,
    val maxRepeats: Int = 3,
    val snoozeMinutes: Int = 5
) {
    val repeatRule: RepeatRule
        get() = RepeatRule(repeatEnabled, repeatIntervalMinutes, maxRepeats, snoozeMinutes)

    companion object {
        /** Sentinel meaning "use the system default notification sound", resolved at play time. */
        const val DEFAULT_SOUND = "default"

        val REPEAT_INTERVAL_OPTIONS_MINUTES = listOf(1, 5, 10, 15, 30)
        val MAX_REPEATS_OPTIONS = listOf(1, 2, 3, 5, 10)
        val SNOOZE_OPTIONS_MINUTES = listOf(1, 5, 10, 15, 30)
    }
}
