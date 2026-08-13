package com.chargealert.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

@Composable
fun AlertMethodsCard(
    notificationEnabled: Boolean,
    soundEnabled: Boolean,
    vibrationEnabled: Boolean,
    notificationPermissionGranted: Boolean,
    onNotificationChange: (Boolean) -> Unit,
    onSoundChange: (Boolean) -> Unit,
    onVibrationChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column {
            AlertMethodRow(
                emoji = "🔔",
                label = "Notification",
                subtitle = if (!notificationPermissionGranted) "Permission required" else null,
                checked = notificationEnabled,
                onCheckedChange = onNotificationChange
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            AlertMethodRow(
                emoji = "🔊",
                label = "Sound",
                subtitle = null,
                checked = soundEnabled,
                onCheckedChange = onSoundChange
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            AlertMethodRow(
                emoji = "📳",
                label = "Vibration",
                subtitle = null,
                checked = vibrationEnabled,
                onCheckedChange = onVibrationChange
            )
        }
    }
}

@Composable
private fun AlertMethodRow(
    emoji: String,
    label: String,
    subtitle: String?,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(value = checked, onValueChange = onCheckedChange, role = Role.Switch)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = emoji, style = MaterialTheme.typography.titleLarge)
            Column(modifier = Modifier.padding(start = 12.dp)) {
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
