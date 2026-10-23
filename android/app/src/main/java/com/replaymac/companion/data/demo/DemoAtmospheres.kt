package com.replaymac.companion.data.demo

import com.replaymac.companion.data.model.CompanionTheme
import com.replaymac.companion.data.model.CompanionThemePalette

/** One of ReplayMac's atmospheres, with the dark colours the demo Mac sends. */
data class DemoAtmosphere(
    val id: String,
    val name: String,
    val darkCanvas: String,
    val darkCard: String,
    val primary: String,
    val secondary: String
) {
    /** The theme a Mac wearing this atmosphere would send, with its own accents and dual tone on. */
    val theme: CompanionTheme
        get() {
            val isNative = id == CompanionTheme.NATIVE_ATMOSPHERE
            return CompanionTheme(
                atmosphere = id,
                atmosphereName = name,
                colorScheme = "dark",
                accentMode = "atmosphere",
                surfaceStyle = if (isNative) "glass" else "tinted_glass",
                dualTone = !isNative,
                dark = CompanionThemePalette(
                    canvas = darkCanvas,
                    card = darkCard,
                    accent = primary,
                    accentSecondary = secondary
                ),
                light = null
            )
        }
}

/**
 * The atmospheres the demo can wear, copied from ReplayMac's ThemeModels.swift.
 * Demo only: a real Mac sends its resolved colours, so the app needs no table.
 */
object DemoAtmospheres {
    const val DEFAULT_ID = CompanionTheme.NATIVE_ATMOSPHERE

    val all: List<DemoAtmosphere> = listOf(
        DemoAtmosphere("native", "Native", "#1E1E1E", "#2A2A2C", "#0A84FF", "#0A84FF"),
        DemoAtmosphere("pitch_black", "Pitch Black", "#07080A", "#111318", "#00E5CC", "#00A3FF"),
        DemoAtmosphere("carbon_studio", "Carbon Studio", "#131315", "#1D1E22", "#FF6B2C", "#FFB800"),
        DemoAtmosphere("deep_ocean", "Deep Ocean", "#0A101D", "#121C30", "#38BDF8", "#6366F1"),
        DemoAtmosphere("arena_boost", "Arena Boost", "#110D1C", "#1B152C", "#F43F5E", "#A855F7"),
        DemoAtmosphere("nordic_frost", "Nordic Frost", "#171C24", "#222936", "#88C0D0", "#5E81AC"),
        DemoAtmosphere("catppuccin_mocha", "Catppuccin Mocha", "#1E1E2E", "#313244", "#CBA6F7", "#B4BEFE"),
        DemoAtmosphere("catppuccin_macchiato", "Catppuccin Macchiato", "#24273A", "#363A4F", "#F5BDE6", "#C6A0F6"),
        DemoAtmosphere("catppuccin_frappe", "Catppuccin Frappé", "#303446", "#414559", "#BABBF1", "#85C1DC")
    )

    fun find(id: String): DemoAtmosphere? = all.firstOrNull { it.id == id }
}
