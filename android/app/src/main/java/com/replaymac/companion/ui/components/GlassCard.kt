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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.replaymac.companion.ui.theme.AppColors
import com.replaymac.companion.ui.theme.AppRadii
import com.replaymac.companion.ui.theme.AppTheme
import com.replaymac.companion.ui.theme.SurfaceStyle

/**
 * A glass panel in the theme's surface style, mirroring the Mac's
 * `glassPanel`. Leave [tint] unspecified for the default panel; an explicit
 * tint (a green success wash, a red warning) keeps today's tinted glass in
 * every style.
 */
@Composable
fun Modifier.glass(
    shape: Shape,
    tint: Color = Color.Unspecified,
    rimAlpha: Float = 1f,
    borderWidth: Dp = 0.75.dp
): Modifier {
    if (tint.isSpecified) return glass(shape, SolidColor(tint), rimAlpha, borderWidth)
    val palette = AppTheme.palette
    return when (palette.surfaceStyle) {
        SurfaceStyle.Glass -> glass(shape, SolidColor(AppColors.SurfaceGlass), rimAlpha, borderWidth)
        SurfaceStyle.TintedGlass -> this
            .clip(shape)
            .background(
                Brush.linearGradient(listOf(palette.card.copy(alpha = 0.65f), palette.accent.copy(alpha = 0.08f))),
                shape
            )
            .border(0.75.dp, palette.accent.copy(alpha = 0.24f * rimAlpha), shape)
        SurfaceStyle.Matte -> this
            .clip(shape)
            .background(palette.card, shape)
            .border(1.dp, Color.White.copy(alpha = 0.09f * rimAlpha), shape)
    }
}

/**
 * A quiet translucent panel filled with [fill], with a hairline rim that
 * catches a little more light along the top edge, close to macOS's regular
 * glass in dark mode.
 */
fun Modifier.glass(
    shape: Shape,
    fill: Brush,
    rimAlpha: Float = 1f,
    borderWidth: Dp = 0.75.dp
): Modifier = this
    .clip(shape)
    .background(fill, shape)
    .border(
        width = borderWidth,
        brush = Brush.verticalGradient(
            listOf(Color.White.copy(alpha = 0.16f * rimAlpha), Color.White.copy(alpha = 0.06f * rimAlpha))
        ),
        shape = shape
    )

/** A solid accent control's fill: the theme's accent, or its gradient when dual tone is on. */
@Composable
fun Modifier.accentFill(shape: Shape, alpha: Float = 1f): Modifier =
    background(AppTheme.palette.accentFill, shape, alpha)

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(AppRadii.Large.dp),
    /** Unspecified for the theme's default panel. */
    tint: Color = Color.Unspecified,
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
