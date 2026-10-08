package com.replaymac.companion.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = AppColors.Accent,
    onPrimary = AppColors.TextPrimary,
    primaryContainer = AppColors.AccentSecondary,
    onPrimaryContainer = AppColors.TextPrimary,
    secondary = AppColors.AccentVariant,
    onSecondary = AppColors.BackgroundPrimary,
    error = AppColors.Danger,
    onError = AppColors.TextPrimary,
    errorContainer = AppColors.DangerContainer,
    onErrorContainer = AppColors.Danger,
    background = AppColors.BackgroundPrimary,
    onBackground = AppColors.TextPrimary,
    surface = AppColors.SurfaceGlass,
    onSurface = AppColors.TextPrimary,
    surfaceVariant = AppColors.BackgroundSecondary,
    onSurfaceVariant = AppColors.TextSecondary,
    outline = AppColors.GlassBorder
)

@Composable
fun ReplayMacCompanionTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
