package com.chargealert.app.ui

import android.media.RingtoneManager
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
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
import com.chargealert.app.ui.components.AlertActiveBanner
import com.chargealert.app.ui.components.AlertBehaviorSection
import com.chargealert.app.ui.components.AlertMethodsSection
import com.chargealert.app.ui.components.BatteryHero
import com.chargealert.app.ui.components.SoundPickerDialog
import com.chargealert.app.ui.components.SoundSelectionRow
import com.chargealert.app.ui.components.TestAlertSection
import com.chargealert.app.ui.components.ThresholdSection
import com.chargealert.app.ui.components.WarningBanner
import com.chargealert.app.ui.components.MonitoringRow
import com.chargealert.app.ui.theme.SectionLabelStyle
import com.chargealert.app.ui.theme.Spacing
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
    onAddCustomSound: () -> Unit,
    onRepeatEnabledChange: (Boolean) -> Unit,
    onRepeatIntervalChange: (Int) -> Unit,
    onMaxRepeatsChange: (Int) -> Unit,
    onSnoozeChange: (Int) -> Unit,
    onTestAlert: () -> Unit,
    onStopAlert: () -> Unit,
    onSnoozeAlert: () -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onDismissWarning: () -> Unit
) {
    var showSoundPicker by remember { mutableStateOf(false) }

    if (showSoundPicker) {
        SoundPickerDialog(
            currentSelection = uiState.alertSettings.selectedSound,
            onDismiss = { showSoundPicker = false },
            onSoundSelected = onSelectedSoundChange,
            onAddCustomSound = {
                showSoundPicker = false
                onAddCustomSound()
            }
        )
    }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(
                    horizontal = Spacing.screenHorizontal,
                    vertical = Spacing.screenTop
                ),
            verticalArrangement = Arrangement.spacedBy(Spacing.sectionGap)
        ) {
            Text(text = "ChargeAlert", style = MaterialTheme.typography.titleLarge)

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

            if (uiState.isAlertActive) {
                AlertActiveBanner(
                    repeatCount = uiState.activeAlertRepeatCount,
                    onStop = onStopAlert,
                    onSnooze = onSnoozeAlert
                )
            }

            BatteryHero(
                batteryState = uiState.batteryState,
                threshold = uiState.alertSettings.threshold,
                thresholdReached = uiState.thresholdReached
            )

            Section(title = "MONITORING") {
                MonitoringRow(
                    monitoringActive = uiState.alertSettings.alertEnabled,
                    onToggle = onMonitoringToggle
                )
            }

            Section(title = "ALERT ME AT") {
                ThresholdSection(
                    threshold = uiState.alertSettings.threshold,
                    onThresholdChangeFinished = onThresholdChangeFinished
                )
            }

            Section(title = "ALERT WITH") {
                Column {
                    AlertMethodsSection(
                        notificationEnabled = uiState.alertSettings.notificationEnabled,
                        soundEnabled = uiState.alertSettings.soundEnabled,
                        vibrationEnabled = uiState.alertSettings.vibrationEnabled,
                        notificationPermissionGranted = uiState.notificationPermissionGranted,
                        onNotificationChange = onNotificationEnabledChange,
                        onSoundChange = onSoundEnabledChange,
                        onVibrationChange = onVibrationEnabledChange
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    SoundSelectionLabelRow(
                        selectedSoundId = uiState.alertSettings.selectedSound,
                        onClick = { showSoundPicker = true }
                    )
                }
            }

            Section(title = "ALERT BEHAVIOR") {
                AlertBehaviorSection(
                    repeatEnabled = uiState.alertSettings.repeatEnabled,
                    repeatIntervalMinutes = uiState.alertSettings.repeatIntervalMinutes,
                    maxRepeats = uiState.alertSettings.maxRepeats,
                    snoozeMinutes = uiState.alertSettings.snoozeMinutes,
                    onRepeatEnabledChange = onRepeatEnabledChange,
                    onRepeatIntervalChange = onRepeatIntervalChange,
                    onMaxRepeatsChange = onMaxRepeatsChange,
                    onSnoozeChange = onSnoozeChange
                )
            }

            TestAlertSection(onTestAlert = onTestAlert)
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column {
        Text(
            text = title,
            style = SectionLabelStyle,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        content()
    }
}

@Composable
private fun SoundSelectionLabelRow(selectedSoundId: String, onClick: () -> Unit) {
    val context = LocalContext.current
    var label by remember(selectedSoundId) { mutableStateOf("Default notification sound") }

    LaunchedEffect(selectedSoundId) {
        label = if (selectedSoundId == AlertSettings.DEFAULT_SOUND) {
            "Default notification sound"
        } else {
            withContext(Dispatchers.IO) {
                runCatching {
                    RingtoneManager.getRingtone(context, Uri.parse(selectedSoundId))?.getTitle(context)
                }.getOrNull() ?: "Custom sound"
            }
        }
    }

    SoundSelectionRow(selectedSoundLabel = label, onClick = onClick)
}
