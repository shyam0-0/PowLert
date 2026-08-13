package com.chargealert.app.ui

import android.app.Application
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
        notificationPermissionGranted,
        warningMessage
    ) { battery, settings, permissionGranted, warning ->
        HomeUiState(battery, settings, permissionGranted, warning)
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

    fun onTestAlert() {
        val state = uiState.value
        // Reuses the exact same dispatch code as a real alert, but is called
        // directly instead of through AlertEngine/ChargingSessionState, so it
        // never reads or mutates session-alerted state.
        BatteryAlertManager.triggerAlert(app, state.alertSettings, state.batteryState.percentage)
    }

    fun dismissWarning() {
        warningMessage.value = null
    }

    private companion object {
        const val LAST_MECHANISM_WARNING = "At least one alert method must remain enabled."
    }
}
