package com.chargealert.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

@Composable
fun MonitoringRow(
    monitoringActive: Boolean,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val indicatorColor = if (monitoringActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline

    Row(
        modifier = modifier
            .fillMaxWidth()
            .toggleable(value = monitoringActive, onValueChange = onToggle, role = Role.Switch),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                androidx.compose.foundation.layout.Box(
                    modifier = Modifier.size(8.dp).background(indicatorColor, CircleShape)
                )
                Text(
                    text = if (monitoringActive) "  Monitoring active" else "  Monitoring disabled",
                    style = MaterialTheme.typography.titleMedium
                )
            }
            Text(
                text = if (monitoringActive) {
                    "Watching your battery in the background."
                } else {
                    "Turn on to get alerted when charging finishes."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(checked = monitoringActive, onCheckedChange = null)
    }
}
