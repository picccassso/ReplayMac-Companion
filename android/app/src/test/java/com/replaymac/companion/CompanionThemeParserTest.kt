package com.replaymac.companion

import com.replaymac.companion.data.model.CompanionServerEvent
import com.replaymac.companion.data.model.CompanionStateSnapshot
import com.replaymac.companion.data.model.CompanionTelemetryTick
import com.replaymac.companion.data.model.CompanionTheme
import com.replaymac.companion.data.model.CompanionThemePalette
import com.replaymac.companion.data.network.CompanionProtocolParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CompanionThemeParserTest {

    private val mochaJson = """
        {
          "atmosphere": "catppuccin_mocha",
          "atmosphereName": "Catppuccin Mocha",
          "colorScheme": "dark",
          "accentMode": "custom",
          "surfaceStyle": "tinted_glass",
          "dualTone": true,
          "dark":  { "canvas": "#1E1E2E", "card": "#313244", "accent": "#CBA6F7", "accentSecondary": "#B4BEFE" },
          "light": { "canvas": "#EFF1F5", "card": "#CCD0DA", "accent": "#CBA6F7", "accentSecondary": "#B4BEFE" }
        }
    """.trimIndent()

    private fun snapshotJson(type: String, theme: String?): String {
        val themeField = theme?.let { ""","theme": $it""" } ?: ""
        return """{"type":"$type","isRecording":true,"bufferedSeconds":12.5,"capturedDisplayName":"Studio Display"$themeField}"""
    }

    private fun parseSnapshot(json: String): CompanionStateSnapshot =
        when (val event = CompanionProtocolParser.parse(json)) {
            is CompanionServerEvent.Snapshot -> event.snapshot
            is CompanionServerEvent.StateChanged -> event.snapshot
            else -> throw AssertionError("Expected a snapshot, got $event")
        }

    private fun assertRestOfSnapshotIntact(snapshot: CompanionStateSnapshot) {
        assertTrue(snapshot.isRecording)
        assertEquals(12.5, snapshot.bufferedSeconds, 0.0)
        assertEquals("Studio Display", snapshot.capturedDisplayName)
    }

    @Test
    fun aSnapshotWithAThemeDecodesEveryField() {
        val snapshot = parseSnapshot(snapshotJson("state_snapshot", mochaJson))

        assertEquals(
            CompanionTheme(
                atmosphere = "catppuccin_mocha",
                atmosphereName = "Catppuccin Mocha",
                colorScheme = "dark",
                accentMode = "custom",
                surfaceStyle = "tinted_glass",
                dualTone = true,
                dark = CompanionThemePalette("#1E1E2E", "#313244", "#CBA6F7", "#B4BEFE"),
                light = CompanionThemePalette("#EFF1F5", "#CCD0DA", "#CBA6F7", "#B4BEFE")
            ),
            snapshot.theme
        )
        assertRestOfSnapshotIntact(snapshot)
    }

    @Test
    fun stateChangedCarriesTheThemeToo() {
        val snapshot = parseSnapshot(snapshotJson("state_changed", mochaJson))
        assertEquals("catppuccin_mocha", snapshot.theme?.atmosphere)
    }

    @Test
    fun aSnapshotWithoutAThemeFromAnOlderMacHasNone() {
        val snapshot = parseSnapshot(snapshotJson("state_snapshot", null))
        assertNull(snapshot.theme)
        assertRestOfSnapshotIntact(snapshot)
    }

    @Test
    fun aThemeThatIsNotAnObjectIsIgnoredAndTheSnapshotStillParses() {
        for (bad in listOf("\"catppuccin_mocha\"", "42", "true", "[1,2]", "null")) {
            val snapshot = parseSnapshot(snapshotJson("state_snapshot", bad))
            assertNull("theme $bad", snapshot.theme)
            assertRestOfSnapshotIntact(snapshot)
        }
    }

    @Test
    fun missingOrWronglyTypedThemeFieldsTakeTheirDefaults() {
        val theme = """{"atmosphere":"deep_ocean","dualTone":"yes","surfaceStyle":7,"dark":"#000000","light":{"canvas":"#FFFFFF","card":5}}"""
        val snapshot = parseSnapshot(snapshotJson("state_snapshot", theme))

        val parsed = snapshot.theme!!
        assertEquals("deep_ocean", parsed.atmosphere)
        assertEquals("", parsed.atmosphereName)
        assertEquals("", parsed.surfaceStyle)
        assertFalse(parsed.dualTone)
        assertNull(parsed.dark)
        assertEquals(CompanionThemePalette(canvas = "#FFFFFF"), parsed.light)
        assertRestOfSnapshotIntact(snapshot)
    }

    @Test
    fun aTelemetryTickKeepsTheCurrentTheme() {
        val snapshot = parseSnapshot(snapshotJson("state_snapshot", mochaJson))
        val tick = CompanionTelemetryTick(recordingElapsedSeconds = 99.0, bufferedSeconds = 30.0, bufferMemoryFormatted = "411 MB")

        val merged = snapshot.applying(tick)

        assertEquals(snapshot.theme, merged.theme)
        assertEquals(99.0, merged.recordingElapsedSeconds, 0.0)
        assertEquals(30.0, merged.bufferedSeconds, 0.0)
        assertEquals("411 MB", merged.bufferMemoryFormatted)
        assertEquals("Studio Display", merged.capturedDisplayName)
    }
}
