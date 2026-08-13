package com.chargealert.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * In-app STOP/SNOOZE fallback (plan.md Phase 4 section 18): the notification
 * action buttons may be unavailable if POST_NOTIFICATIONS is denied, so this
 * gives an always-available way to acknowledge an active alert. Sends the
 * exact same intents as the notification actions -- no separate logic path.
 */
@Composable
fun AlertActiveBanner(
    repeatCount: Int,
    onStop: () -> Unit,
    onSnooze: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Text(
                text = if (repeatCount == 0) "Battery alert active" else "Battery alert active (reminder $repeatCount)",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onSnooze) { Text("Snooze") }
                OutlinedButton(onClick = onStop, modifier = Modifier.padding(start = 8.dp)) { Text("Stop") }
            }
        }
    }
}
