package com.chargealert.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.chargealert.app.domain.AlertSettings

@Composable
fun AlertBehaviorSection(
    repeatEnabled: Boolean,
    repeatIntervalMinutes: Int,
    maxRepeats: Int,
    snoozeMinutes: Int,
    onRepeatEnabledChange: (Boolean) -> Unit,
    onRepeatIntervalChange: (Int) -> Unit,
    onMaxRepeatsChange: (Int) -> Unit,
    onSnoozeChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var openPicker by remember { mutableStateOf<Picker?>(null) }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .toggleable(value = repeatEnabled, onValueChange = onRepeatEnabledChange, role = Role.Switch)
                .padding(vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Repeat, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    text = "Repeat alerts",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(start = 16.dp)
                )
            }
            Switch(checked = repeatEnabled, onCheckedChange = null)
        }

        if (repeatEnabled) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            ValueRow("Every", "$repeatIntervalMinutes min") { openPicker = Picker.Interval }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            ValueRow("Maximum repeats", "$maxRepeats") { openPicker = Picker.MaxRepeats }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            ValueRow("Snooze", "$snoozeMinutes min") { openPicker = Picker.Snooze }
        }
    }

    when (openPicker) {
        Picker.Interval -> OptionPickerDialog(
            title = "Repeat every",
            options = AlertSettings.REPEAT_INTERVAL_OPTIONS_MINUTES,
            selected = repeatIntervalMinutes,
            labelFor = { "$it min" },
            onDismiss = { openPicker = null },
            onSelect = onRepeatIntervalChange
        )
        Picker.MaxRepeats -> OptionPickerDialog(
            title = "Maximum repeats",
            options = AlertSettings.MAX_REPEATS_OPTIONS,
            selected = maxRepeats,
            labelFor = { "$it" },
            onDismiss = { openPicker = null },
            onSelect = onMaxRepeatsChange
        )
        Picker.Snooze -> OptionPickerDialog(
            title = "Snooze duration",
            options = AlertSettings.SNOOZE_OPTIONS_MINUTES,
            selected = snoozeMinutes,
            labelFor = { "$it min" },
            onDismiss = { openPicker = null },
            onSelect = onSnoozeChange
        )
        null -> Unit
    }
}

private enum class Picker { Interval, MaxRepeats, Snooze }

@Composable
private fun ValueRow(label: String, value: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp, horizontal = 0.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyLarge)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Icon(
                Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
