package com.chargealert.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.chargealert.app.domain.BatteryState
import com.chargealert.app.ui.theme.HeroPercentageStyle

/**
 * The visual identity of the app: the percentage is the largest thing on
 * screen, no card/border around it, a status pill instead of plain text, and
 * a thin progress track with a marker showing where the threshold sits.
 */
@Composable
fun BatteryHero(
    batteryState: BatteryState,
    threshold: Int,
    thresholdReached: Boolean,
    modifier: Modifier = Modifier
) {
    val accentColor by animateColorAsState(
        targetValue = when {
            thresholdReached -> MaterialTheme.colorScheme.tertiary
            batteryState.isCharging -> MaterialTheme.colorScheme.primary
            else -> MaterialTheme.colorScheme.onSurfaceVariant
        },
        label = "heroAccentColor"
    )

    val progress by animateFloatAsState(
        targetValue = (batteryState.percentage / 100f).coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 500),
        label = "batteryProgress"
    )

    val statusLabel = when {
        thresholdReached -> "Threshold reached"
        batteryState.isCharging -> "Charging"
        else -> "Not charging"
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = "Battery ${batteryState.percentage} percent, $statusLabel, alert threshold $threshold percent"
            },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "${batteryState.percentage}%",
            style = HeroPercentageStyle,
            color = MaterialTheme.colorScheme.onBackground
        )

        Surface(
            shape = RoundedCornerShape(50),
            color = accentColor.copy(alpha = 0.14f),
            modifier = Modifier.padding(top = 10.dp, bottom = 24.dp)
        ) {
            Column {
                androidx.compose.foundation.layout.Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (batteryState.isCharging) {
                        Icon(
                            imageVector = Icons.Filled.Bolt,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.height(16.dp)
                        )
                    }
                    Text(
                        text = statusLabel,
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = accentColor
                    )
                }
            }
        }

        BatteryProgressTrack(
            progress = progress,
            threshold = threshold,
            accentColor = accentColor,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
        )

        Text(
            text = "Alert at $threshold%",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 10.dp)
        )
    }
}

@Composable
private fun BatteryProgressTrack(
    progress: Float,
    threshold: Int,
    accentColor: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val markerColor = MaterialTheme.colorScheme.onSurfaceVariant

    Canvas(
        modifier = modifier.height(20.dp)
    ) {
        val strokeWidth = 6.dp.toPx()
        val y = size.height / 2f

        drawLine(
            color = trackColor,
            start = Offset(0f, y),
            end = Offset(size.width, y),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )

        if (progress > 0f) {
            drawLine(
                color = accentColor,
                start = Offset(0f, y),
                end = Offset(size.width * progress, y),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )
        }

        val thresholdX = size.width * (threshold / 100f).coerceIn(0f, 1f)
        drawLine(
            color = markerColor,
            start = Offset(thresholdX, 0f),
            end = Offset(thresholdX, size.height),
            strokeWidth = 2.dp.toPx(),
            cap = StrokeCap.Round
        )
    }
}
