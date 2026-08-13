package com.chargealert.app.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.chargealert.app.ChargeAlertApplication
import com.chargealert.app.alert.BatteryAlertManager
import com.chargealert.app.domain.AlertMechanismGuard
import com.chargealert.app.service.BatteryMonitoringService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Owns dashboard state and mediates every user action between the UI and
 * UserPreferencesRepository / BatteryMonitoringService. Composables never
 * touch DataStore or the service directly (plan.md Phase 3 section 13).
 */
class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as ChargeAlertApplication
    private val preferencesRepository = app.preferencesRepository
    private val batteryRepository = app.batteryRepository

    private val notificationPermissionGranted = MutableStateFlow(true)
    private val warningMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<HomeUiState> = combine(
        batteryRepository.batteryStateUpdates(),
        preferencesRepository.settings,
        app.sessionState,
        notificationPermissionGranted,
        warningMessage
    ) { battery, settings, sessionState, permissionGranted, warning ->
        HomeUiState(battery, settings, sessionState, permissionGranted, warning)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState.Initial
    )

    fun onNotificationPermissionStateChanged(granted: Boolean) {
        notificationPermissionGranted.value = granted
    }

    fun onMonitoringToggle(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setAlertEnabled(enabled) }
        if (enabled) {
            BatteryMonitoringService.start(app)
        } else {
            BatteryMonitoringService.stop(app)
        }
    }

    fun onThresholdChange(threshold: Int) {
        viewModelScope.launch { preferencesRepository.setThreshold(threshold) }
    }

    fun onNotificationEnabledChange(enabled: Boolean) {
        val settings = uiState.value.alertSettings
        if (AlertMechanismGuard.wouldDisableAllMechanisms(enabled, settings.soundEnabled, settings.vibrationEnabled)) {
            warningMessage.value = LAST_MECHANISM_WARNING
            return
        }
        viewModelScope.launch { preferencesRepository.setNotificationEnabled(enabled) }
    }

    fun onSoundEnabledChange(enabled: Boolean) {
        val settings = uiState.value.alertSettings
        if (AlertMechanismGuard.wouldDisableAllMechanisms(settings.notificationEnabled, enabled, settings.vibrationEnabled)) {
            warningMessage.value = LAST_MECHANISM_WARNING
            return
        }
        viewModelScope.launch { preferencesRepository.setSoundEnabled(enabled) }
    }

    fun onVibrationEnabledChange(enabled: Boolean) {
        val settings = uiState.value.alertSettings
        if (AlertMechanismGuard.wouldDisableAllMechanisms(settings.notificationEnabled, settings.soundEnabled, enabled)) {
            warningMessage.value = LAST_MECHANISM_WARNING
            return
        }
        viewModelScope.launch { preferencesRepository.setVibrationEnabled(enabled) }
    }

    fun onSelectedSoundChange(soundId: String) {
        viewModelScope.launch { preferencesRepository.setSelectedSound(soundId) }
    }

    /** [uri] must already have a persisted read permission grant (see MainActivity's OpenDocument callback). */
    fun onCustomSoundPicked(uri: Uri) {
        onSelectedSoundChange(uri.toString())
    }

    fun onRepeatEnabledChange(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setRepeatEnabled(enabled) }
    }

    fun onRepeatIntervalChange(minutes: Int) {
        viewModelScope.launch { preferencesRepository.setRepeatIntervalMinutes(minutes) }
    }

    fun onMaxRepeatsChange(count: Int) {
        viewModelScope.launch { preferencesRepository.setMaxRepeats(count) }
    }

    fun onSnoozeChange(minutes: Int) {
        viewModelScope.launch { preferencesRepository.setSnoozeMinutes(minutes) }
    }

    fun onTestAlert() {
        val state = uiState.value
        // Reuses the exact same dispatch code as a real alert, but is called
        // directly instead of through RepeatAlertEngine/ChargingSessionState,
        // so it never reads or mutates session-alerted state, never counts
        // toward the repeat cap, and gets no STOP/SNOOZE actions (there is no
        // real sequence for them to act on).
        BatteryAlertManager.triggerAlert(
            context = app,
            settings = state.alertSettings,
            batteryPercentage = state.batteryState.percentage,
            repeatCount = 0,
            includeActions = false
        )
    }

    /** Same intent path the notification's STOP action uses -- the in-app fallback banner is not a separate code path. */
    fun onStopAlert() {
        BatteryMonitoringService.sendStopAlert(app)
    }

    fun onSnoozeAlert() {
        BatteryMonitoringService.sendSnoozeAlert(app)
    }

    fun dismissWarning() {
        warningMessage.value = null
    }

    private companion object {
        const val LAST_MECHANISM_WARNING = "At least one alert method must remain enabled."
    }
}
