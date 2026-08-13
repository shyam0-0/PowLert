package com.chargealert.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.chargealert.app.ui.HomeScreen
import com.chargealert.app.ui.HomeViewModel
import com.chargealert.app.ui.theme.ChargeAlertTheme

/**
 * Hosts the dashboard. All state/business logic lives in HomeViewModel; this
 * activity only wires Android-specific plumbing (permission requests,
 * the system file picker, lifecycle-driven permission re-checks) into it.
 */
class MainActivity : ComponentActivity() {

    private lateinit var viewModel: HomeViewModel

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            viewModel.onNotificationPermissionStateChanged(granted)
        }

    private val customSoundLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) {
                // Persist read access across reboots/process death -- we only
                // store the URI string, not a copy of the file (plan.md Phase 4
                // section 8).
                contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                viewModel.onCustomSoundPicked(uri)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            ChargeAlertTheme {
                viewModel = viewModel()
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()

                // Re-check permission whenever the activity resumes, so returning
                // from system settings (or a permission dialog) reflects the
                // current state without re-prompting.
                val lifecycleOwner = LocalLifecycleOwner.current
                DisposableEffect(lifecycleOwner) {
                    val observer = LifecycleEventObserver { _, event ->
                        if (event == Lifecycle.Event.ON_RESUME) {
                            viewModel.onNotificationPermissionStateChanged(hasNotificationPermission())
                        }
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
                }

                HomeScreen(
                    uiState = uiState,
                    onMonitoringToggle = { enabled ->
                        if (enabled) requestNotificationPermissionIfNeeded()
                        viewModel.onMonitoringToggle(enabled)
                    },
                    onThresholdChangeFinished = viewModel::onThresholdChange,
                    onNotificationEnabledChange = viewModel::onNotificationEnabledChange,
                    onSoundEnabledChange = viewModel::onSoundEnabledChange,
                    onVibrationEnabledChange = viewModel::onVibrationEnabledChange,
                    onSelectedSoundChange = viewModel::onSelectedSoundChange,
                    onAddCustomSound = { customSoundLauncher.launch(arrayOf("audio/*")) },
                    onRepeatEnabledChange = viewModel::onRepeatEnabledChange,
                    onRepeatIntervalChange = viewModel::onRepeatIntervalChange,
                    onMaxRepeatsChange = viewModel::onMaxRepeatsChange,
                    onSnoozeChange = viewModel::onSnoozeChange,
                    onTestAlert = viewModel::onTestAlert,
                    onStopAlert = viewModel::onStopAlert,
                    onSnoozeAlert = viewModel::onSnoozeAlert,
                    onRequestNotificationPermission = { requestNotificationPermissionIfNeeded(force = true) },
                    onDismissWarning = viewModel::dismissWarning
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (::viewModel.isInitialized) {
            viewModel.onNotificationPermissionStateChanged(hasNotificationPermission())
        }
    }

    private fun hasNotificationPermission(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestNotificationPermissionIfNeeded(force: Boolean = false) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        if (hasNotificationPermission()) return
        if (force || !alreadyRequestedThisSession) {
            alreadyRequestedThisSession = true
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private var alreadyRequestedThisSession = false
}
