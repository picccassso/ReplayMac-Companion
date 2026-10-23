package com.replaymac.companion

import androidx.compose.ui.graphics.Color
import com.replaymac.companion.data.demo.DemoAtmospheres
import com.replaymac.companion.data.demo.DemoCompanionSession
import com.replaymac.companion.data.model.CompanionServerEvent
import com.replaymac.companion.data.model.CompanionTheme
import com.replaymac.companion.data.model.CompanionThemePalette
import com.replaymac.companion.data.model.RememberedMac
import com.replaymac.companion.data.repository.ThemeController
import com.replaymac.companion.data.repository.ThemeStore
import com.replaymac.companion.ui.theme.ThemePalette
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneOffset

/** The theme the app wears as the repository drives it through a connection's life. */
@OptIn(ExperimentalCoroutinesApi::class)
class ThemeControllerTest {

    private val prefs = FakeSharedPreferences()
    private val store = ThemeStore(prefs)

    private val studio = RememberedMac("server-1", "Studio", networkEndpoint = null, usbPort = null, lastUsedUsb = false)
    private val studioKey = ThemeStore.macKey(studio)

    private fun theme(atmosphere: String, accent: String) = CompanionTheme(
        atmosphere = atmosphere,
        atmosphereName = atmosphere,
        surfaceStyle = "tinted_glass",
        dualTone = false,
        dark = CompanionThemePalette("#1E1E2E", "#313244", accent, accent)
    )

    private val mocha = theme("catppuccin_mocha", "#CBA6F7")
    private val ocean = theme("deep_ocean", "#38BDF8")

    @Test
    fun theRememberedMacsThemeAppliesBeforeAnySnapshot() {
        store.save(mocha, studioKey)
        val controller = ThemeController(store)

        controller.showRemembered(studio)

        assertEquals(Color(0xFFCBA6F7), controller.palette.value.accent)
    }

    @Test
    fun withNoRememberedMacTheAppLooksAsItAlwaysHas() {
        val controller = ThemeController(store)
        controller.showRemembered(null)
        assertEquals(ThemePalette.Default, controller.palette.value)
    }

    @Test
    fun aSnapshotWithAThemeReplacesTheShownOneAndIsStored() {
        store.save(mocha, studioKey)
        val controller = ThemeController(store)
        controller.showRemembered(studio)

        controller.onMacSnapshot(ocean, studioKey)

        assertEquals(Color(0xFF38BDF8), controller.palette.value.accent)
        assertEquals(ocean, store.load(studioKey))
    }

    @Test
    fun aSnapshotWithoutAThemeShowsTheDefaultButKeepsTheStoredOne() {
        val controller = ThemeController(store)
        controller.onMacSnapshot(mocha, studioKey)

        controller.onMacSnapshot(null, studioKey)

        assertEquals(ThemePalette.Default, controller.palette.value)
        assertEquals(mocha, store.load(studioKey))
    }

    @Test
    fun forgettingAMacClearsItsTheme() {
        val controller = ThemeController(store)
        controller.onMacSnapshot(mocha, studioKey)

        controller.forget(studio)
        controller.showRemembered(studio)

        assertNull(store.load(studioKey))
        assertEquals(ThemePalette.Default, controller.palette.value)
    }

    @Test
    fun turningMatchMacOffShowsTheDefaultAtOnceAndPersists() {
        val controller = ThemeController(store)
        controller.onMacSnapshot(mocha, studioKey)

        controller.setMatchMac(false)
        assertEquals(ThemePalette.Default, controller.palette.value)
        assertEquals(false, ThemeController(ThemeStore(prefs)).matchMac.value)

        controller.setMatchMac(true)
        assertEquals(Color(0xFFCBA6F7), controller.palette.value.accent)
    }

    @Test
    fun pickingADemoAtmosphereRethemesTheAppWithoutStoringAnything() = runTest {
        val session = DemoCompanionSession(scope = backgroundScope, clock = { 1_760_000_000L }, zone = { ZoneOffset.UTC })
        val controller = ThemeController(store)
        controller.setMatchMac(false) // the demo shows its theme whatever the toggle says
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            session.events.collect { event ->
                if (event is CompanionServerEvent.StateChanged) controller.onDemoSnapshot(event.snapshot.theme)
            }
        }

        session.start()
        assertEquals(Color(0xFF0A84FF), controller.palette.value.accent)
        assertEquals("Native", controller.palette.value.atmosphereName)

        assertTrue(session.selectAtmosphere("arena_boost"))
        val arena = controller.palette.value
        assertEquals(Color(0xFF110D1C), arena.canvas)
        assertEquals(Color(0xFFF43F5E), arena.accent)
        assertEquals(Color(0xFFA855F7), arena.accentSecondary)
        assertTrue(arena.useGradient)

        assertTrue(prefs.all.keys.none { it.startsWith(ThemeStore.KEY_THEME_PREFIX) })
    }

    @Test
    fun theDemoStartsOverOnNativeEachTime() = runTest {
        val session = DemoCompanionSession(scope = backgroundScope, clock = { 1_760_000_000L }, zone = { ZoneOffset.UTC })
        session.start()
        session.selectAtmosphere("deep_ocean")
        session.stop()

        session.start()

        assertEquals(DemoAtmospheres.DEFAULT_ID, session.currentState?.toSnapshot()?.theme?.atmosphere)
    }
}
