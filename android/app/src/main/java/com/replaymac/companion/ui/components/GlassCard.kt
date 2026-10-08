package com.replaymac.companion.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.replaymac.companion.ui.theme.AppColors
import com.replaymac.companion.ui.theme.AppRadii

/**
 * A quiet translucent panel with a hairline rim that catches a little more
 * light along the top edge, close to macOS's regular glass in dark mode.
 */
fun Modifier.glass(
    shape: Shape,
    tint: Color = AppColors.SurfaceGlass,
    rimAlpha: Float = 1f,
    borderWidth: Dp = 0.75.dp
): Modifier = this
    .clip(shape)
    .background(tint, shape)
    .border(
        width = borderWidth,
        brush = Brush.verticalGradient(
            listOf(Color.White.copy(alpha = 0.16f * rimAlpha), Color.White.copy(alpha = 0.06f * rimAlpha))
        ),
        shape = shape
    )

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(AppRadii.Large.dp),
    tint: Color = AppColors.SurfaceGlass,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .glass(shape, tint)
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(color = Color.White),
                        onClick = onClick
                    )
                } else {
                    Modifier
                }
            ),
        content = content
    )
}
