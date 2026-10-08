package com.replaymac.companion.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.replaymac.companion.ui.theme.AppColors
import java.util.Locale
import kotlin.math.min

/** Matches the desktop's MenuBarState.formattedDuration: "00:12", or "01:02:03" past an hour. */
fun formatDurationSeconds(seconds: Double): String {
    val totalSec = seconds.toInt().coerceAtLeast(0)
    val hours = totalSec / 3600
    val mins = (totalSec % 3600) / 60
    val secs = totalSec % 60
    return if (hours > 0) {
        String.format(Locale.US, "%02d:%02d:%02d", hours, mins, secs)
    } else {
        String.format(Locale.US, "%02d:%02d", mins, secs)
    }
}

/** Matches the desktop's meter suffix: "Filling…" until the window is full, then "Ready". */
fun meterFillSuffix(availableSeconds: Double, capSeconds: Int): String =
    if (availableSeconds < capSeconds) "Filling…" else "Ready"

@Composable
fun ProgressMeter(
    title: String,
    availableSeconds: Double,
    capSeconds: Int,
    detail: String,
    modifier: Modifier = Modifier
) {
    val safeCap = capSeconds.coerceAtLeast(1)
    val fraction by animateFloatAsState(
        targetValue = (availableSeconds / safeCap).toFloat().coerceIn(0f, 1f),
        animationSpec = tween(400),
        label = "meterProgress"
    )
    val suffix = meterFillSuffix(availableSeconds, safeCap)
    val isReady = suffix == "Ready"
    val barColor by animateColorAsState(if (isReady) AppColors.Success else AppColors.Accent, label = "meterColor")
    val digits = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum")

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(title, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium), color = AppColors.TextPrimary)
            Spacer(Modifier.weight(1f))
            Text(
                text = "${formatDurationSeconds(availableSeconds)} / ${formatDurationSeconds(safeCap.toDouble())}",
                style = digits,
                color = AppColors.TextPrimary
            )
            Text(
                text = "  $suffix",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = if (isReady) AppColors.Success else AppColors.TextSecondary
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(CircleShape)
                .background(AppColors.TextPrimary.copy(alpha = 0.1f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fraction)
                    .clip(CircleShape)
                    .background(Brush.horizontalGradient(listOf(barColor.copy(alpha = 0.75f), barColor)))
            )
        }
        Text(detail, style = MaterialTheme.typography.labelSmall, color = AppColors.TextSecondary)
    }
}

@Composable
fun DualProgressMeters(
    bufferedSeconds: Double,
    bufferDurationSeconds: Int,
    bufferMemoryFormatted: String,
    longBufferEnabled: Boolean,
    extendedBufferElapsedSeconds: Double,
    longBufferDurationSeconds: Int,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        ProgressMeter(
            title = "Quick replay",
            availableSeconds = min(bufferedSeconds, bufferDurationSeconds.toDouble()),
            capSeconds = bufferDurationSeconds,
            detail = "$bufferMemoryFormatted in memory"
        )
        if (longBufferEnabled) {
            ProgressMeter(
                title = "Extended replay",
                availableSeconds = min(extendedBufferElapsedSeconds, longBufferDurationSeconds.toDouble()),
                capSeconds = longBufferDurationSeconds,
                detail = "Kept on disk"
            )
        }
    }
}
