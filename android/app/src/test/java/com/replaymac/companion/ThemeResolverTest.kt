package com.replaymac.companion

import androidx.compose.ui.graphics.Color
import com.replaymac.companion.data.model.CompanionTheme
import com.replaymac.companion.data.model.CompanionThemePalette
import com.replaymac.companion.ui.theme.SurfaceStyle
import com.replaymac.companion.ui.theme.ThemeHex
import com.replaymac.companion.ui.theme.ThemePalette
import com.replaymac.companion.ui.theme.ThemeResolver
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeResolverTest {

    private val mocha = CompanionTheme(
        atmosphere = "catppuccin_mocha",
        atmosphereName = "Catppuccin Mocha",
        colorScheme = "dark",
        accentMode = "atmosphere",
        surfaceStyle = "tinted_glass",
        dualTone = true,
        dark = CompanionThemePalette("#1E1E2E", "#313244", "#CBA6F7", "#B4BEFE"),
        light = CompanionThemePalette("#EFF1F5", "#CCD0DA", "#CBA6F7", "#B4BEFE")
    )

    // ==================== Resolver ====================

    @Test
    fun matchMacOffGivesTheDefaultLook() {
        assertEquals(ThemePalette.Default, ThemeResolver.resolve(mocha, matchMac = false))
    }

    @Test
    fun noThemeOrNoDarkPaletteGivesTheDefaultLook() {
        assertEquals(ThemePalette.Default, ThemeResolver.resolve(null, matchMac = true))
        assertEquals(ThemePalette.Default, ThemeResolver.resolve(mocha.copy(dark = null), matchMac = true))
    }

    @Test
    fun catppuccinUsesTheSentColours() {
        val palette = ThemeResolver.resolve(mocha, matchMac = true)

        assertEquals(Color(0xFF1E1E2E), palette.canvas)
        assertEquals(Color(0xFF313244), palette.card)
        assertEquals(Color(0xFFCBA6F7), palette.accent)
        assertEquals(Color(0xFFB4BEFE), palette.accentSecondary)
        assertTrue(palette.useGradient)
        assertEquals(SurfaceStyle.TintedGlass, palette.surfaceStyle)
        assertEquals("Catppuccin Mocha", palette.atmosphereName)
    }

    @Test
    fun nativeKeepsTheCompanionSurfacesWithTheMacsAccent() {
        val native = mocha.copy(
            atmosphere = "native",
            atmosphereName = "Native",
            surfaceStyle = "glass",
            dark = CompanionThemePalette("#1E1E1E", "#2A2A2C", "#FF6B2C", "#FF6B2C")
        )
        val palette = ThemeResolver.resolve(native, matchMac = true)

        assertEquals(ThemePalette.DefaultCanvas, palette.canvas)
        assertEquals(ThemePalette.DefaultCard, palette.card)
        assertEquals(Color(0xFFFF6B2C), palette.accent)
        assertEquals(SurfaceStyle.Glass, palette.surfaceStyle)
    }

    @Test
    fun aBadHexFallsBackForThatColourOnly() {
        val broken = mocha.copy(dark = mocha.dark!!.copy(canvas = "not-a-colour"))
        val palette = ThemeResolver.resolve(broken, matchMac = true)

        assertEquals(ThemePalette.DefaultCanvas, palette.canvas)
        assertEquals(Color(0xFF313244), palette.card)
        assertEquals(Color(0xFFCBA6F7), palette.accent)
    }

    @Test
    fun aBadSecondaryAccentFollowsThePrimaryAndDropsTheGradient() {
        val broken = mocha.copy(dark = mocha.dark!!.copy(accentSecondary = "#12"))
        val palette = ThemeResolver.resolve(broken, matchMac = true)

        assertEquals(Color(0xFFCBA6F7), palette.accentSecondary)
        assertFalse(palette.useGradient)
    }

    @Test
    fun dualToneOffMeansNoGradient() {
        assertFalse(ThemeResolver.resolve(mocha.copy(dualTone = false), matchMac = true).useGradient)
    }

    @Test
    fun dualToneWithEqualAccentsMeansNoGradient() {
        val single = mocha.copy(dark = mocha.dark!!.copy(accentSecondary = "#cba6f7"))
        assertFalse(ThemeResolver.resolve(single, matchMac = true).useGradient)
    }

    @Test
    fun anUnknownSurfaceStyleIsGlassAndABlankNameIsNull() {
        val palette = ThemeResolver.resolve(mocha.copy(surfaceStyle = "frosted", atmosphereName = " "), matchMac = true)
        assertEquals(SurfaceStyle.Glass, palette.surfaceStyle)
        assertNull(palette.atmosphereName)
    }

    // ==================== Contrast ====================

    @Test
    fun accentsGetTheLabelThatReads() {
        assertEquals(Color.White, ThemeResolver.onAccent(Color(0xFF0A84FF)))
        assertEquals(Color.White, ThemeResolver.onAccent(Color(0xFFF43F5E)))
        assertEquals(ThemePalette.DarkOnAccent, ThemeResolver.onAccent(Color(0xFFF5C2E7)))
        assertEquals(ThemePalette.DarkOnAccent, ThemeResolver.onAccent(Color(0xFF88C0D0)))
    }

    @Test
    fun aGradientNeedsWhiteToReadOnBothStops() {
        // White reads on the blue but not on the pale pink, so the label goes dark.
        assertEquals(ThemePalette.DarkOnAccent, ThemeResolver.onAccent(Color(0xFF0A84FF), Color(0xFFF5C2E7)))

        val gradient = mocha.copy(dark = CompanionThemePalette("#110D1C", "#1B152C", "#F43F5E", "#88C0D0"))
        assertEquals(ThemePalette.DarkOnAccent, ThemeResolver.resolve(gradient, matchMac = true).onAccent)
        // Without dual tone only the first stop is drawn, and white reads on it.
        assertEquals(Color.White, ThemeResolver.resolve(gradient.copy(dualTone = false), matchMac = true).onAccent)
    }

    // ==================== Hex parsing ====================

    @Test
    fun sixDigitHexWithOrWithoutHash() {
        assertEquals(Color(0xFF1E1E2E), ThemeHex.parse("#1E1E2E"))
        assertEquals(Color(0xFF1E1E2E), ThemeHex.parse("1E1E2E"))
        assertEquals(Color(0xFFCBA6F7), ThemeHex.parse("#cba6f7"))
        assertEquals(Color(0xFFCBA6F7), ThemeHex.parse("  #CbA6f7 \n"))
    }

    @Test
    fun eightDigitHexHasAlphaLast() {
        assertEquals(Color(0x80112233), ThemeHex.parse("#11223380"))
        assertEquals(Color(0xFF112233), ThemeHex.parse("#112233ff"))
    }

    @Test
    fun anythingElseIsNull() {
        for (bad in listOf(null, "", "#", "#FFF", "#12345", "#1234567", "#123456789", "#GG0000", "red", "##112233", "0x112233")) {
            assertNull("\"$bad\"", ThemeHex.parse(bad))
        }
    }
}
