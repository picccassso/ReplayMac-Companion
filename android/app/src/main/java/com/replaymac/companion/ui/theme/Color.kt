package com.replaymac.companion.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Mirrors the macOS dark appearance ReplayMac uses on the desktop: system
 * blue accent, Apple's label greys, and translucent white fills for glass.
 */
object AppColors {
    val Accent = Color(0xFF0A84FF)
    val AccentSecondary = Color(0xFF0071E3)
    val AccentVariant = Color(0xFF64D2FF)

    val Danger = Color(0xFFFF453A)
    val DangerContainer = Color(0x29FF453A)

    val Success = Color(0xFF30D158)
    val SuccessContainer = Color(0x2930D158)

    val Warning = Color(0xFFFF9F0A)

    val BackgroundPrimary = Color(0xFF111113)
    val BackgroundSecondary = Color(0xFF1C1C1E)

    /** Base tint for floating glass panels, layered over the ambient backdrop. */
    val SurfaceGlass = Color(0x12FFFFFF)
    /** Quieter fill for controls that sit inside a glass panel. */
    val SurfaceGlassSubtle = Color(0x0AFFFFFF)
    /** Near-opaque glass for sheets and dialogs, which float over busy content. */
    val SurfaceElevated = Color(0xF21E1E20)

    val GlassBorder = Color(0x1FFFFFFF)
    val GlassBorderHighlight = Color(0x40FFFFFF)
    val Separator = Color(0x1AFFFFFF)

    val TextPrimary = Color(0xFFF5F5F7)
    val TextSecondary = Color(0x99EBEBF5)
    val TextTertiary = Color(0x4DEBEBF5)

    val IndicatorActive = Danger
    val IndicatorIdle = Color(0xFF636366)
}

/** Corner radii shared with the desktop's AppTheme (8 / 12 / 18), scaled for touch. */
object AppRadii {
    const val Small = 10
    const val Medium = 14
    const val Large = 22
}
