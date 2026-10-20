package com.replaymac.companion.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.replaymac.companion.ui.theme.AppColors

/** Pulses when it goes live, then settles: an endless pulse keeps the screen redrawing for hours in deck mode. */
private const val PULSE_COUNT = 3
private const val PULSE_MILLIS = 1200

@Composable
fun StatusDot(
    isLive: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 10.dp,
    activeColor: Color = AppColors.IndicatorActive,
    idleColor: Color = AppColors.IndicatorIdle
) {
    Box(
        modifier = modifier.size(size * 2),
        contentAlignment = Alignment.Center
    ) {
        if (isLive) {
            val pulse = remember { Animatable(0f) }
            LaunchedEffect(Unit) {
                repeat(PULSE_COUNT) {
                    pulse.snapTo(0f)
                    pulse.animateTo(1f, tween(PULSE_MILLIS, easing = FastOutSlowInEasing))
                }
            }

            // Outer pulsing ring, read in the draw phase so it never recomposes
            Box(
                modifier = Modifier
                    .size(size)
                    .graphicsLayer {
                        val scale = 1f + 1.2f * pulse.value
                        scaleX = scale
                        scaleY = scale
                        alpha = 0.6f * (1f - pulse.value)
                    }
                    .background(activeColor, CircleShape)
            )

            // Inner solid dot
            Box(
                modifier = Modifier
                    .size(size)
                    .background(activeColor, CircleShape)
            )
        } else {
            // Idle dot
            Box(
                modifier = Modifier
                    .size(size)
                    .background(idleColor, CircleShape)
            )
        }
    }
}
