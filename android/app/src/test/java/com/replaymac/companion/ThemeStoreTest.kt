package com.replaymac.companion

import com.replaymac.companion.data.model.CompanionTheme
import com.replaymac.companion.data.model.CompanionThemePalette
import com.replaymac.companion.data.model.RememberedMac
import com.replaymac.companion.data.repository.ThemeStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeStoreTest {

    private val prefs = FakeSharedPreferences()
    private val store = ThemeStore(prefs)

    private val mocha = CompanionTheme(
        atmosphere = "catppuccin_mocha",
        atmosphereName = "Catppuccin Mocha",
        colorScheme = "dark",
        accentMode = "custom",
        surfaceStyle = "tinted_glass",
        dualTone = true,
        dark = CompanionThemePalette("#1E1E2E", "#313244", "#CBA6F7", "#B4BEFE"),
        light = CompanionThemePalette("#EFF1F5", "#CCD0DA", "#CBA6F7", "#B4BEFE")
    )
    private val ocean = CompanionTheme(
        atmosphere = "deep_ocean",
        atmosphereName = "Deep Ocean",
        surfaceStyle = "matte",
        dark = CompanionThemePalette("#0A101D", "#121C30", "#38BDF8", "#6366F1")
    )

    @Test
    fun savesAndLoadsEveryFieldPerMac() {
        store.save(mocha, "mac-1")
        assertEquals(mocha, store.load("mac-1"))
        assertNull(store.load("mac-2"))
    }

    @Test
    fun twoMacsKeepTheirOwnThemes() {
        store.save(mocha, "mac-1")
        store.save(ocean, "mac-2")

        assertEquals(mocha, store.load("mac-1"))
        assertEquals(ocean, store.load("mac-2"))
    }

    @Test
    fun clearForgetsOnlyThatMac() {
        store.save(mocha, "mac-1")
        store.save(ocean, "mac-2")

        store.clear("mac-1")

        assertNull(store.load("mac-1"))
        assertEquals(ocean, store.load("mac-2"))
    }

    @Test
    fun aMacIsKeyedByServerIdOrElseItsName() {
        assertEquals("server-1", ThemeStore.macKey(RememberedMac("server-1", "Studio", null, null, false)))
        assertEquals("Studio", ThemeStore.macKey(RememberedMac(null, "Studio", null, null, false)))
    }

    @Test
    fun matchMacIsOnByDefaultAndPersists() {
        assertTrue(store.matchMac)

        store.matchMac = false

        assertFalse(ThemeStore(prefs).matchMac)
    }

    @Test
    fun corruptJsonLoadsAsNull() {
        prefs.edit().putString(ThemeStore.KEY_THEME_PREFIX + "mac-1", "{not json").apply()
        assertNull(store.load("mac-1"))
    }
}
