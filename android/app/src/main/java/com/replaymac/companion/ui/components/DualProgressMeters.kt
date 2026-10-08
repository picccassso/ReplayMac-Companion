package com.replaymac.companion.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.replaymac.companion.ui.theme.AppColors
import java.util.Locale
import kotlin.math.min

fun formatDurationSeconds(seconds: Double): String {
    val totalSec = seconds.toInt().coerceAtLeast(0)
    val hours = totalSec / 3600
    val mins = (totalSec % 3600) / 60
    val secs = totalSec % 60
    return if (hours > 0) {
        String.format(Locale.US, "%d:%02d:%02d", hours, mins, secs)
    } else {
        String.format(Locale.US, "%d:%02d", mins, secs)
    }
}

@Composable
fun ProgressMeter(
    title: String,
    availableSeconds: Double,
    capSeconds: Int,
    detail: String,
    modifier: Modifier = Modifier
) {
    val safeCap = if (capSeconds > 0) capSeconds else 30
    val rawFraction = (availableSeconds / safeCap).toFloat().coerceIn(0f, 1f)
    val animatedFraction by animateFloatAsState(
        targetValue = rawFraction,
        animationSpec = tween(300),
        label = "meterProgress"
    )

    val isReady = availableSeconds >= safeCap
    val progressColor by animateColorAsState(
        targetValue = if (isReady) AppColors.Success else AppColors.Accent,
        label = "meterColor"
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = AppColors.TextPrimary
                )
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${formatDurationSeconds(availableSeconds)} / ${formatDurationSeconds(safeCap.toDouble())}",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = FontFamily.Monospace,
                        color = AppColors.TextSecondary
                    )
                )

                if (isReady) {
                    Text(
                        text = "Ready",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = AppColors.Success
                        )
                    )
                }
            }
        }

        LinearProgressIndicator(
            progress = { animatedFraction },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = progressColor,
            trackColor = AppColors.BackgroundSecondary
        )

        Text(
            text = detail,
            style = MaterialTheme.typography.labelSmall.copy(
                color = AppColors.TextTertiary
            )
        )
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
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        val quickAvailable = min(bufferedSeconds, bufferDurationSeconds.toDouble())
        ProgressMeter(
            title = "Quick replay",
            availableSeconds = quickAvailable,
            capSeconds = bufferDurationSeconds,
            detail = "$bufferMemoryFormatted in memory"
        )

        if (longBufferEnabled) {
            val extAvailable = min(extendedBufferElapsedSeconds, longBufferDurationSeconds.toDouble())
            val diskEstimatedMb = (extAvailable * 2.5).toInt()
            val diskDetail = if (diskEstimatedMb >= 1000) {
                String.format(Locale.US, "%.1f GB on disk · Kept on disk", diskEstimatedMb / 1024.0)
            } else {
                "$diskEstimatedMb MB on disk · Kept on disk"
            }

            ProgressMeter(
                title = "Extended replay",
                availableSeconds = extAvailable,
                capSeconds = longBufferDurationSeconds,
                detail = diskDetail
            )
        }
    }
}
