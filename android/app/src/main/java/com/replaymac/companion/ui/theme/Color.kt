package com.replaymac.companion.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * The colours that never change with the Mac's theme: Apple's label greys,
 * the semantic red, green and orange, and translucent white fills for glass.
 * Canvas, card and accent come from the theme instead (see [AppTheme.palette]).
 */
object AppColors {
    val Danger = Color(0xFFFF453A)
    val DangerContainer = Color(0x29FF453A)

    val Success = Color(0xFF30D158)
    val SuccessContainer = Color(0x2930D158)

    val Warning = Color(0xFFFF9F0A)

    /**
     * White 7%: Liquid Glass panels' tint, and a plain fill for small wells
     * (icon backgrounds, slider tracks). Panels themselves take the theme's
     * surface style through `Modifier.glass`.
     */
    val SurfaceGlass = Color(0x12FFFFFF)
    /** Quieter fill for controls that sit inside a glass panel. */
    val SurfaceGlassSubtle = Color(0x0AFFFFFF)

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
