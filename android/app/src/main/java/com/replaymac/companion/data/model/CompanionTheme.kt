package com.replaymac.companion.data.model

import kotlinx.serialization.Serializable

/**
 * One appearance's colours as the Mac resolved them, as hex strings
 * ("#RRGGBB", or "#RRGGBBAA" with alpha last).
 */
@Serializable
data class CompanionThemePalette(
    val canvas: String = "",
    val card: String = "",
    val accent: String = "",
    val accentSecondary: String = ""
)

/**
 * The Mac's theme, sent as the optional `theme` object of state_snapshot and
 * state_changed. The ids are informational; the phone draws from the resolved
 * hexes, so new atmospheres and the macOS system accent need no phone update.
 * The phone is always dark, so only [dark] is applied; [light] is kept for later.
 */
@Serializable
data class CompanionTheme(
    /** "native", "catppuccin_mocha", … */
    val atmosphere: String = "",
    val atmosphereName: String = "",
    /** "system", "dark" or "light". */
    val colorScheme: String = "",
    /** "atmosphere", "system" or "custom". */
    val accentMode: String = "",
    /** "glass", "tinted_glass" or "matte". */
    val surfaceStyle: String = "",
    /** Whether the Mac draws its accent fills as a two-colour gradient. */
    val dualTone: Boolean = false,
    val dark: CompanionThemePalette? = null,
    val light: CompanionThemePalette? = null
) {
    companion object {
        const val NATIVE_ATMOSPHERE = "native"
    }
}
