package com.chargealert.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

@Composable
fun AlertMethodsSection(
    notificationEnabled: Boolean,
    soundEnabled: Boolean,
    vibrationEnabled: Boolean,
    notificationPermissionGranted: Boolean,
    onNotificationChange: (Boolean) -> Unit,
    onSoundChange: (Boolean) -> Unit,
    onVibrationChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        AlertMethodRow(
            icon = Icons.Filled.Notifications,
            label = "Notification",
            subtitle = if (!notificationPermissionGranted) "Permission required" else null,
            checked = notificationEnabled,
            onCheckedChange = onNotificationChange
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        AlertMethodRow(
            icon = Icons.AutoMirrored.Filled.VolumeUp,
            label = "Sound",
            subtitle = null,
            checked = soundEnabled,
            onCheckedChange = onSoundChange
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        AlertMethodRow(
            icon = Icons.Filled.Vibration,
            label = "Vibration",
            subtitle = null,
            checked = vibrationEnabled,
            onCheckedChange = onVibrationChange
        )
    }
}

@Composable
private fun AlertMethodRow(
    icon: ImageVector,
    label: String,
    subtitle: String?,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(value = checked, onValueChange = onCheckedChange, role = Role.Switch)
            .padding(vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Column(modifier = Modifier.padding(start = 16.dp)) {
                Text(text = label, style = MaterialTheme.typography.bodyLarge)
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
        Switch(checked = checked, onCheckedChange = null)
    }
}
