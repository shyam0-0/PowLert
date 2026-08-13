package com.chargealert.app.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.chargealert.app.domain.AlertSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "user_preferences")

/**
 * Persists user-configurable alert settings. Session/alert-trigger state is
 * intentionally NOT stored here — see plan.md section 15.3 and 15.5.
 */
class UserPreferencesRepository(private val context: Context) {

    private object Keys {
        val ALERT_ENABLED = booleanPreferencesKey("alertEnabled")
        val THRESHOLD = intPreferencesKey("threshold")
        val SOUND_ENABLED = booleanPreferencesKey("soundEnabled")
        val VIBRATION_ENABLED = booleanPreferencesKey("vibrationEnabled")
        val NOTIFICATION_ENABLED = booleanPreferencesKey("notificationEnabled")
        val SELECTED_SOUND = stringPreferencesKey("selectedSound")
    }

    val settings: Flow<AlertSettings> = context.dataStore.data.map { prefs ->
        val defaults = AlertSettings()
        AlertSettings(
            alertEnabled = prefs[Keys.ALERT_ENABLED] ?: defaults.alertEnabled,
            threshold = prefs[Keys.THRESHOLD] ?: defaults.threshold,
            soundEnabled = prefs[Keys.SOUND_ENABLED] ?: defaults.soundEnabled,
            vibrationEnabled = prefs[Keys.VIBRATION_ENABLED] ?: defaults.vibrationEnabled,
            notificationEnabled = prefs[Keys.NOTIFICATION_ENABLED] ?: defaults.notificationEnabled,
            selectedSound = prefs[Keys.SELECTED_SOUND] ?: defaults.selectedSound
        )
    }

    suspend fun setAlertEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.ALERT_ENABLED] = enabled }
    }

    suspend fun setThreshold(threshold: Int) {
        context.dataStore.edit { it[Keys.THRESHOLD] = threshold }
    }

    suspend fun setSoundEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.SOUND_ENABLED] = enabled }
    }

    suspend fun setVibrationEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.VIBRATION_ENABLED] = enabled }
    }

    suspend fun setNotificationEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.NOTIFICATION_ENABLED] = enabled }
    }

    suspend fun setSelectedSound(sound: String) {
        context.dataStore.edit { it[Keys.SELECTED_SOUND] = sound }
    }
}
