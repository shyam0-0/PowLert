package com.chargealert.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.chargealert.app.domain.BatteryState

/**
 * The single most prominent element on the dashboard: current percentage,
 * charging state, and how close the device is to the configured threshold.
 */
@Composable
fun BatteryHeroCard(
    batteryState: BatteryState,
    threshold: Int,
    thresholdReached: Boolean,
    modifier: Modifier = Modifier
) {
    val statusLabel = when {
        thresholdReached -> "Threshold reached"
        batteryState.isCharging -> "Charging"
        else -> "Not charging"
    }

    val statusColor by animateColorAsState(
        targetValue = if (thresholdReached) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        label = "statusColor"
    )

    val progress by animateFloatAsState(
        targetValue = (batteryState.percentage / 100f).coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 400),
        label = "batteryProgress"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = "Battery ${batteryState.percentage} percent, $statusLabel, alert threshold $threshold percent"
            },
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "BATTERY",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = "${batteryState.percentage}%",
                style = MaterialTheme.typography.displayLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = statusLabel,
                style = MaterialTheme.typography.titleMedium,
                color = statusColor
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp)
            ) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                )

                Text(
                    text = "Alert threshold  $threshold%",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
