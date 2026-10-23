package com.replaymac.companion.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color

/** The resolved theme colours, read by components through [AppTheme.palette]. */
val LocalThemePalette = compositionLocalOf { ThemePalette.Default }

object AppTheme {
    val palette: ThemePalette
        @Composable get() = LocalThemePalette.current
}

/** How long a live theme change crossfades; Android's animator scale of 0 makes it instant. */
private const val THEME_CROSSFADE_MILLIS = 350

@Composable
fun ReplayMacCompanionTheme(
    palette: ThemePalette = ThemePalette.Default,
    content: @Composable () -> Unit
) {
    val animated = animatedPalette(palette)
    val colorScheme = darkColorScheme(
        primary = animated.accent,
        onPrimary = animated.onAccent,
        primaryContainer = animated.accentSecondary,
        onPrimaryContainer = AppColors.TextPrimary,
        secondary = animated.accentSecondary,
        onSecondary = animated.onAccent,
        error = AppColors.Danger,
        onError = AppColors.TextPrimary,
        errorContainer = AppColors.DangerContainer,
        onErrorContainer = AppColors.Danger,
        background = animated.canvas,
        onBackground = AppColors.TextPrimary,
        surface = animated.card,
        onSurface = AppColors.TextPrimary,
        surfaceVariant = animated.card,
        onSurfaceVariant = AppColors.TextSecondary,
        surfaceContainerHigh = animated.card,
        outline = AppColors.GlassBorder
    )
    CompositionLocalProvider(LocalThemePalette provides animated) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}

/** [palette] with each colour crossfading to its new value. */
@Composable
private fun animatedPalette(palette: ThemePalette): ThemePalette {
    @Composable
    fun animate(target: Color, label: String): Color {
        val value by animateColorAsState(target, tween(THEME_CROSSFADE_MILLIS), label = label)
        return value
    }
    return palette.copy(
        canvas = animate(palette.canvas, "themeCanvas"),
        card = animate(palette.card, "themeCard"),
        accent = animate(palette.accent, "themeAccent"),
        accentSecondary = animate(palette.accentSecondary, "themeAccentSecondary"),
        onAccent = animate(palette.onAccent, "themeOnAccent")
    )
}
