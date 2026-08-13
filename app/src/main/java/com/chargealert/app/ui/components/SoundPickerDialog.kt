package com.chargealert.app.ui.components

import android.content.Context
import android.media.RingtoneManager
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.chargealert.app.alert.SoundPreviewPlayer
import com.chargealert.app.domain.AlertSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class SoundOption(val title: String, val id: String)

/**
 * Lists Android's system notification sounds plus (if set) the user's
 * custom picked file. "Add custom sound" hands off to the caller, which owns
 * the ActivityResultLauncher (must live in an Activity, not this dialog).
 */
@Composable
fun SoundPickerDialog(
    currentSelection: String,
    onDismiss: () -> Unit,
    onSoundSelected: (String) -> Unit,
    onAddCustomSound: () -> Unit
) {
    val context = LocalContext.current
    var options by remember { mutableStateOf<List<SoundOption>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        options = withContext(Dispatchers.IO) { loadNotificationSounds(context, currentSelection) }
        isLoading = false
    }

    // Never leave a preview playing after the dialog closes.
    DisposableEffect(Unit) {
        onDispose { SoundPreviewPlayer.stop() }
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
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(onClick = onAddCustomSound)
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text(
                                text = "Add custom sound",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(start = 12.dp)
                            )
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    }
                    items(options, key = { it.id }) { option ->
                        SoundOptionRow(
                            option = option,
                            selected = option.id == currentSelection,
                            onSelect = {
                                onSoundSelected(option.id)
                                onDismiss()
                            },
                            onPreview = { SoundPreviewPlayer.preview(context, option.id) }
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
private fun SoundOptionRow(option: SoundOption, selected: Boolean, onSelect: () -> Unit, onPreview: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect)
            .padding(PaddingValues(vertical = 2.dp)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onSelect)
        Text(
            text = option.title,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(start = 4.dp).weight(1f)
        )
        IconButton(onClick = onPreview) {
            Icon(Icons.Filled.PlayArrow, contentDescription = "Preview ${option.title}")
        }
    }
}

private fun loadNotificationSounds(context: Context, currentSelection: String): List<SoundOption> {
    val result = mutableListOf(SoundOption("Default notification sound", AlertSettings.DEFAULT_SOUND))

    val manager = RingtoneManager(context).apply {
        setType(RingtoneManager.TYPE_NOTIFICATION)
    }

    val cursor = manager.cursor
    val systemUris = mutableSetOf(AlertSettings.DEFAULT_SOUND)
    while (cursor.moveToNext()) {
        val title = cursor.getString(RingtoneManager.TITLE_COLUMN_INDEX)
        val uri = manager.getRingtoneUri(cursor.position).toString()
        systemUris += uri
        result += SoundOption(title, uri)
    }

    if (currentSelection !in systemUris) {
        val customTitle = runCatching {
            RingtoneManager.getRingtone(context, android.net.Uri.parse(currentSelection))?.getTitle(context)
        }.getOrNull() ?: "Your custom sound"
        result.add(0, SoundOption(customTitle, currentSelection))
    }

    return result
}
