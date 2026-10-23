package com.replaymac.companion.ui.theme

import androidx.compose.ui.graphics.Color
import com.replaymac.companion.data.model.CompanionTheme
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * Turns the Mac's theme into the palette the phone draws with. The phone is
 * always dark, so only the theme's dark colours are used, and any colour that
 * fails to parse falls back to the default for that colour alone.
 */
object ThemeResolver {
    /** WCAG's minimum for large text and UI components. */
    const val MIN_LABEL_CONTRAST = 3.0

    fun resolve(theme: CompanionTheme?, matchMac: Boolean): ThemePalette {
        val default = ThemePalette.Default
        val dark = theme?.dark
        if (!matchMac || dark == null) return default

        // "native" keeps the companion's own surfaces with the Mac's accent.
        val isNative = theme.atmosphere == CompanionTheme.NATIVE_ATMOSPHERE
        val canvas = if (isNative) default.canvas else ThemeHex.parse(dark.canvas) ?: default.canvas
        val card = if (isNative) default.card else ThemeHex.parse(dark.card) ?: default.card
        val accent = ThemeHex.parse(dark.accent) ?: default.accent
        val accentSecondary = ThemeHex.parse(dark.accentSecondary) ?: accent
        val useGradient = theme.dualTone && accent != accentSecondary

        return ThemePalette(
            canvas = canvas,
            card = card,
            accent = accent,
            accentSecondary = accentSecondary,
            onAccent = onAccent(accent, if (useGradient) accentSecondary else null),
            useGradient = useGradient,
            surfaceStyle = SurfaceStyle.fromWire(theme.surfaceStyle),
            atmosphereName = theme.atmosphereName.takeIf { it.isNotBlank() }
        )
    }

    /** White unless it falls short of [MIN_LABEL_CONTRAST] on either accent stop, then near-black. */
    fun onAccent(accent: Color, secondary: Color? = null): Color {
        val stops = listOfNotNull(accent, secondary)
        val whiteReads = stops.all { ThemeContrast.ratio(Color.White, it) >= MIN_LABEL_CONTRAST }
        return if (whiteReads) Color.White else ThemePalette.DarkOnAccent
    }
}

/** Parses the Mac's colour strings. */
object ThemeHex {
    private val HEX = Regex("^[0-9a-fA-F]+$")

    /**
     * "#RRGGBB" or "#RRGGBBAA" (alpha last, as the Mac writes it), with or
     * without the "#", any case, surrounding whitespace ignored. Anything else is null.
     */
    fun parse(value: String?): Color? {
        val digits = value?.trim()?.removePrefix("#") ?: return null
        if ((digits.length != 6 && digits.length != 8) || !HEX.matches(digits)) return null
        val v = digits.toLong(16)
        return if (digits.length == 6) {
            Color(red = channel(v shr 16), green = channel(v shr 8), blue = channel(v))
        } else {
            Color(red = channel(v shr 24), green = channel(v shr 16), blue = channel(v shr 8), alpha = channel(v))
        }
    }

    private fun channel(bits: Long): Float = (bits and 0xFF).toFloat() / 255f
}

/** WCAG 2 contrast between sRGB colours. */
object ThemeContrast {
    fun relativeLuminance(color: Color): Double {
        fun linear(c: Float): Double {
            val v = c.toDouble()
            return if (v <= 0.03928) v / 12.92 else ((v + 0.055) / 1.055).pow(2.4)
        }
        return 0.2126 * linear(color.red) + 0.7152 * linear(color.green) + 0.0722 * linear(color.blue)
    }

    fun ratio(a: Color, b: Color): Double {
        val la = relativeLuminance(a)
        val lb = relativeLuminance(b)
        return (max(la, lb) + 0.05) / (min(la, lb) + 0.05)
    }
}
