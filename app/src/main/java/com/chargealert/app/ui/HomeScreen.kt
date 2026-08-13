package com.chargealert.app.ui

import android.media.RingtoneManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.chargealert.app.domain.AlertSettings
import com.chargealert.app.ui.components.AlertMethodsCard
import com.chargealert.app.ui.components.BatteryHeroCard
import com.chargealert.app.ui.components.MonitoringCard
import com.chargealert.app.ui.components.SoundPickerDialog
import com.chargealert.app.ui.components.SoundSelectionRow
import com.chargealert.app.ui.components.TestAlertCard
import com.chargealert.app.ui.components.ThresholdCard
import com.chargealert.app.ui.components.WarningBanner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun HomeScreen(
    uiState: HomeUiState,
    onMonitoringToggle: (Boolean) -> Unit,
    onThresholdChangeFinished: (Int) -> Unit,
    onNotificationEnabledChange: (Boolean) -> Unit,
    onSoundEnabledChange: (Boolean) -> Unit,
    onVibrationEnabledChange: (Boolean) -> Unit,
    onSelectedSoundChange: (String) -> Unit,
    onTestAlert: () -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onDismissWarning: () -> Unit
) {
    var showSoundPicker by remember { mutableStateOf(false) }

    if (showSoundPicker) {
        SoundPickerDialog(
            currentSelection = uiState.alertSettings.selectedSound,
            onDismiss = { showSoundPicker = false },
            onSoundSelected = onSelectedSoundChange
        )
    }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(text = "ChargeAlert", style = MaterialTheme.typography.headlineMedium)

            if (uiState.alertSettings.notificationEnabled && !uiState.notificationPermissionGranted) {
                WarningBanner(
                    message = "Notification permission required for alerts to appear. Sound and vibration still work.",
                    actionLabel = "Allow notifications",
                    onAction = onRequestNotificationPermission
                )
            }

            if (uiState.warningMessage != null) {
                WarningBanner(
                    message = uiState.warningMessage,
                    actionLabel = "Dismiss",
                    onAction = onDismissWarning
                )
            } else if (uiState.noAlertMechanismEnabled) {
                WarningBanner(message = "No alert method is enabled. You won't be notified when charging finishes.")
            }

            BatteryHeroCard(
                batteryState = uiState.batteryState,
                threshold = uiState.alertSettings.threshold,
                thresholdReached = uiState.thresholdReached
            )

            SectionLabel("MONITORING")
            MonitoringCard(
                monitoringActive = uiState.alertSettings.alertEnabled,
                onToggle = onMonitoringToggle
            )

            SectionLabel("ALERT")
            ThresholdCard(
                threshold = uiState.alertSettings.threshold,
                onThresholdChangeFinished = onThresholdChangeFinished
            )

            SectionLabel("ALERT METHODS")
            AlertMethodsCard(
                notificationEnabled = uiState.alertSettings.notificationEnabled,
                soundEnabled = uiState.alertSettings.soundEnabled,
                vibrationEnabled = uiState.alertSettings.vibrationEnabled,
                notificationPermissionGranted = uiState.notificationPermissionGranted,
                onNotificationChange = onNotificationEnabledChange,
                onSoundChange = onSoundEnabledChange,
                onVibrationChange = onVibrationEnabledChange
            )

            SoundSelectionLabelRow(
                selectedSoundId = uiState.alertSettings.selectedSound,
                onClick = { showSoundPicker = true }
            )

            TestAlertCard(onTestAlert = onTestAlert)
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun SoundSelectionLabelRow(selectedSoundId: String, onClick: () -> Unit) {
    val context = LocalContext.current
    var label by remember(selectedSoundId) { mutableStateOf("Default notification sound") }

    LaunchedEffect(selectedSoundId) {
        if (selectedSoundId == AlertSettings.DEFAULT_SOUND) {
            label = "Default notification sound"
        } else {
            label = withContext(Dispatchers.IO) {
                runCatching {
                    RingtoneManager.getRingtone(context, android.net.Uri.parse(selectedSoundId))?.getTitle(context)
                }.getOrNull() ?: "Custom sound"
            }
        }
    }

    SoundSelectionRow(selectedSoundLabel = label, onClick = onClick)
}
