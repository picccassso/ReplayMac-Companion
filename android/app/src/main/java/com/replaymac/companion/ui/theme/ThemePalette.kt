package com.replaymac.companion.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor

/** How glass panels are drawn, mirroring the Mac's `glassPanel` styles. */
enum class SurfaceStyle(val displayName: String) {
    /** Today's look: white 7% glass over the canvas. */
    Glass("Liquid Glass"),
    /** The card colour with a faint accent wash and an accent rim. */
    TintedGlass("Tinted Glass"),
    /** The opaque card colour with a quiet white rim. */
    Matte("Solid Matte");

    companion object {
        /** From the wire's "glass", "tinted_glass" or "matte"; anything else is [Glass]. */
        fun fromWire(value: String?): SurfaceStyle = when (value) {
            "tinted_glass" -> TintedGlass
            "matte" -> Matte
            else -> Glass
        }
    }
}

/**
 * The theme-driven colours the UI draws with, already resolved from the
 * Mac's theme (see [ThemeResolver]). Text and semantic colours stay in
 * [AppColors] and never change with the theme.
 */
data class ThemePalette(
    /** The window background. */
    val canvas: Color,
    /** Sheets, matte panels and other raised surfaces. */
    val card: Color,
    val accent: Color,
    /** The gradient's second stop; equal to [accent] when there is no gradient. */
    val accentSecondary: Color,
    /** Labels and icons drawn on an accent fill. */
    val onAccent: Color,
    /** Accent fills run from [accent] to [accentSecondary]. */
    val useGradient: Boolean,
    val surfaceStyle: SurfaceStyle,
    /** The Mac's atmosphere, for display; null for today's look. */
    val atmosphereName: String?
) {
    /** What a solid accent control is filled with: the gradient when on, otherwise the plain accent. */
    val accentFill: Brush
        get() = if (useGradient) Brush.horizontalGradient(listOf(accent, accentSecondary)) else SolidColor(accent)

    /** Elevated glass for dialogs and floating panels, which sit over busy content. */
    val surfaceElevated: Color
        get() = card.copy(alpha = 0.95f)

    companion object {
        val DefaultCanvas = Color(0xFF111113)
        val DefaultCard = Color(0xFF1C1C1E)
        val DefaultAccent = Color(0xFF0A84FF)
        /** The dark label used on accents too light for white. */
        val DarkOnAccent = Color(0xFF111113)

        /** The companion's own look, used without a theme or with "Match my Mac" off. */
        val Default = ThemePalette(
            canvas = DefaultCanvas,
            card = DefaultCard,
            accent = DefaultAccent,
            accentSecondary = DefaultAccent,
            onAccent = Color.White,
            useGradient = false,
            surfaceStyle = SurfaceStyle.Glass,
            atmosphereName = null
        )
    }
}
