package com.chargealert.app.ui.components

import android.content.Context
import android.media.RingtoneManager
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.chargealert.app.domain.AlertSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class SoundOption(val title: String, val id: String)

/**
 * Lists Android's system notification sounds (RingtoneManager), not custom
 * audio files -- per Phase 3 scope, no local file import / audio library.
 */
@Composable
fun SoundPickerDialog(
    currentSelection: String,
    onDismiss: () -> Unit,
    onSoundSelected: (String) -> Unit
) {
    val context = LocalContext.current
    var options by remember { mutableStateOf<List<SoundOption>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        options = withContext(Dispatchers.IO) { loadNotificationSounds(context) }
        isLoading = false
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Alert sound") },
        text = {
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(modifier = Modifier.heightIn(max = 360.dp)) {
                    items(options, key = { it.id }) { option ->
                        SoundOptionRow(
                            option = option,
                            selected = option.id == currentSelection,
                            onSelect = {
                                onSoundSelected(option.id)
                                onDismiss()
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@Composable
private fun SoundOptionRow(option: SoundOption, selected: Boolean, onSelect: () -> Unit) {
    androidx.compose.foundation.layout.Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect)
            .padding(PaddingValues(vertical = 4.dp)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onSelect)
        Text(
            text = option.title,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(start = 4.dp)
        )
    }
}

private fun loadNotificationSounds(context: Context): List<SoundOption> {
    val result = mutableListOf(SoundOption("Default notification sound", AlertSettings.DEFAULT_SOUND))

    val manager = RingtoneManager(context).apply {
        setType(RingtoneManager.TYPE_NOTIFICATION)
    }

    val cursor = manager.cursor
    while (cursor.moveToNext()) {
        val title = cursor.getString(RingtoneManager.TITLE_COLUMN_INDEX)
        val uri = manager.getRingtoneUri(cursor.position)
        result += SoundOption(title, uri.toString())
    }

    return result
}
