package com.replaymac.companion

import com.replaymac.companion.data.model.CompanionAudioApp
import com.replaymac.companion.data.model.CompanionClipSummary
import com.replaymac.companion.data.model.CompanionDisplayInfo
import com.replaymac.companion.data.model.CompanionProfileSummary
import com.replaymac.companion.data.model.CompanionStateSnapshot
import com.replaymac.companion.ui.components.TileTone
import com.replaymac.companion.ui.components.clipDetailLine
import com.replaymac.companion.ui.components.meterFillSuffix
import com.replaymac.companion.ui.screens.ControlTileSpec
import com.replaymac.companion.ui.screens.HomeControl
import com.replaymac.companion.ui.screens.HomeControlLabels
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeControlLabelsTest {

    private val idle = CompanionStateSnapshot()
    private val buffering = CompanionStateSnapshot(isRecording = true, bufferedSeconds = 12.0, recordingElapsedSeconds = 12.0)

    private fun CompanionStateSnapshot.tile(control: HomeControl): ControlTileSpec =
        HomeControlLabels.tiles(this).single { it.control == control }

    @Test
    fun gridListsAllSixActionsInDesktopOrder() {
        assertEquals(
            listOf(
                HomeControl.SaveReplay,
                HomeControl.SaveExtendedReplay,
                HomeControl.ReplayBuffer,
                HomeControl.Session,
                HomeControl.Microphone,
                HomeControl.SystemAudio
            ),
            HomeControlLabels.tiles(idle).map { it.control }
        )
    }

    @Test
    fun saveTileIsOnlyTappableOnceTheBufferHasFootage() {
        assertFalse(idle.tile(HomeControl.SaveReplay).isEnabled)
        assertEquals("Buffer stopped", idle.tile(HomeControl.SaveReplay).subtitle)

        val justStarted = buffering.copy(bufferedSeconds = 0.4)
        assertFalse(justStarted.tile(HomeControl.SaveReplay).isEnabled)
        assertEquals("Buffering…", justStarted.tile(HomeControl.SaveReplay).subtitle)

        assertTrue(buffering.tile(HomeControl.SaveReplay).isEnabled)
        assertEquals("Ready", buffering.tile(HomeControl.SaveReplay).subtitle)
        assertEquals("Save Last 30s", buffering.tile(HomeControl.SaveReplay).title)
    }

    @Test
    fun savingShowsASpinnerAndBlocksSessionToggle() {
        val saving = buffering.copy(saveStatus = "saving")
        val save = saving.tile(HomeControl.SaveReplay)
        assertTrue(save.isLoading)
        assertFalse(save.isEnabled)
        assertEquals("Saving…", save.subtitle)
        assertFalse(saving.tile(HomeControl.Session).isEnabled)
    }

    @Test
    fun extendedReplayExplainsWhyItIsUnavailable() {
        val off = buffering.copy(longBufferEnabled = false)
        assertFalse(off.tile(HomeControl.SaveExtendedReplay).isEnabled)
        assertEquals("Turned off", off.tile(HomeControl.SaveExtendedReplay).subtitle)

        val on = buffering.copy(longBufferEnabled = true)
        assertTrue(on.tile(HomeControl.SaveExtendedReplay).isEnabled)
    }

    @Test
    fun extendedReplayTitleUsesMinutesOnlyForWholeMinutes() {
        assertEquals("Save Last 5m", HomeControlLabels.extendedReplayTitle(300))
        assertEquals("Save Last 90s", HomeControlLabels.extendedReplayTitle(90))
        assertEquals("Save Last 45s", HomeControlLabels.extendedReplayTitle(45))
    }

    @Test
    fun compactTitlesAreProvidedForLongLabels() {
        assertEquals("Save 5m", HomeControlLabels.extendedReplayCompactTitle(300))
        assertEquals("Save 90s", HomeControlLabels.extendedReplayCompactTitle(90))
        assertEquals("Save 45s", HomeControlLabels.extendedReplayCompactTitle(45))

        val readyTile = buffering.tile(HomeControl.SaveReplay)
        assertEquals("Save 30s", readyTile.compactTitle)

        val idleSession = idle.tile(HomeControl.Session)
        assertEquals("Record Session", idleSession.compactTitle)

        val recording = idle.copy(isSessionRecording = true, sessionElapsedSeconds = 75.0)
        val recSession = recording.tile(HomeControl.Session)
        assertEquals("Stop Session", recSession.compactTitle)
    }

    @Test
    fun sessionTileTurnsLiveAndShowsElapsedTimeWhileRecording() {
        val recording = idle.copy(isSessionRecording = true, sessionElapsedSeconds = 75.0)
        val session = recording.tile(HomeControl.Session)
        assertEquals(TileTone.Live, session.tone)
        assertEquals("Stop & Save Session", session.title)
        assertEquals("01:15", session.subtitle)
    }

    @Test
    fun switchedOffAudioSourceStaysTappableBecauseTheMacEnablesIt() {
        // ReplayMac's toggleMicrophoneMuteFromUI enables and unmutes a disabled mic.
        val mic = idle.copy(isMicrophoneEnabled = false).tile(HomeControl.Microphone)
        assertTrue(mic.isEnabled)
        assertEquals(TileTone.Off, mic.tone)

        val muted = idle.copy(isSystemAudioEnabled = true, isSystemAudioMuted = true).tile(HomeControl.SystemAudio)
        assertEquals(TileTone.Muted, muted.tone)
        assertEquals("Muted", muted.subtitle)
    }

    @Test
    fun statusCardDescribesTheCurrentCapture() {
        assertEquals("Not recording", HomeControlLabels.headline(idle))
        assertTrue(HomeControlLabels.subheadline(idle).contains("last 30 seconds"))

        // Quick buffer only (longBufferEnabled = false): caps at bufferDurationSeconds (30s)
        val runningQuick = buffering.copy(recordingElapsedSeconds = 65.0, capturedDisplayName = "Studio Display")
        assertEquals("Replay buffer running", HomeControlLabels.headline(runningQuick))
        assertEquals("00:30 · Studio Display", HomeControlLabels.subheadline(runningQuick))

        // Extended buffer enabled (longBufferDurationSeconds = 300s): continues past 30s and caps at 5:00
        val runningExtended = runningQuick.copy(longBufferEnabled = true, longBufferDurationSeconds = 300)
        assertEquals("01:05 · Studio Display", HomeControlLabels.subheadline(runningExtended))
        assertEquals(
            "05:00 · Studio Display",
            HomeControlLabels.subheadline(runningExtended.copy(recordingElapsedSeconds = 400.0))
        )

        // Session recording remains uncapped
        val both = runningQuick.copy(isSessionRecording = true, sessionElapsedSeconds = 95.0)
        assertEquals("Session recording", HomeControlLabels.headline(both))
        assertEquals("01:35 · Studio Display", HomeControlLabels.subheadline(both))
    }

    @Test
    fun statusCardFlagsAFallbackDisplayLikeTheDesktop() {
        val fallback = buffering.copy(
            recordingElapsedSeconds = 11.0,
            capturedDisplayName = "Built-in Retina Display",
            isUsingFallbackDisplay = true
        )
        assertEquals("00:11 · Built-in Retina Display (Fallback)", HomeControlLabels.subheadline(fallback))
    }

    @Test
    fun meterReportsReadyOnlyWhenTheWindowIsFull() {
        assertEquals("Filling…", meterFillSuffix(29.9, 30))
        assertEquals("Ready", meterFillSuffix(30.0, 30))
    }

    @Test
    fun clipDetailLineSkipsMissingMetadata() {
        val bare = CompanionClipSummary(id = "1", filename = "clip.mp4", kind = "session")
        assertEquals("Session", clipDetailLine(bare))

        val full = bare.copy(kind = "replay", duration = 30.0, fileSize = 5L * 1024 * 1024)
        assertEquals("Replay · 00:30 · 5.0 MB", clipDetailLine(full))
    }

    @Test
    fun audioTilesDeclareSecondaryMixerActionAndReflectAppMuteSubtitles() {
        val tiles = HomeControlLabels.tiles(idle)
        assertFalse(tiles.single { it.control == HomeControl.SaveReplay }.hasSecondaryAction)
        assertFalse(tiles.single { it.control == HomeControl.SaveExtendedReplay }.hasSecondaryAction)
        assertFalse(tiles.single { it.control == HomeControl.ReplayBuffer }.hasSecondaryAction)
        assertFalse(tiles.single { it.control == HomeControl.Session }.hasSecondaryAction)
        assertTrue(tiles.single { it.control == HomeControl.Microphone }.hasSecondaryAction)
        assertTrue(tiles.single { it.control == HomeControl.SystemAudio }.hasSecondaryAction)

        val withMutedApps = idle.copy(
            isSystemAudioEnabled = true,
            isSystemAudioMuted = false,
            perAppAudioEnabled = false,
            audioApps = listOf(
                CompanionAudioApp("com.spotify.client", "Spotify", isMuted = true),
                CompanionAudioApp("com.hnc.Discord", "Discord", isMuted = true),
                CompanionAudioApp("com.apple.Safari", "Safari", isMuted = false)
            )
        )
        val sysTile = withMutedApps.tile(HomeControl.SystemAudio)
        assertEquals("On · 2 muted", sysTile.subtitle)
        assertEquals(TileTone.Normal, sysTile.tone)

        val selectedAppOnly = withMutedApps.copy(perAppAudioEnabled = true)
        assertEquals("Selected app", selectedAppOnly.tile(HomeControl.SystemAudio).subtitle)
        assertEquals(TileTone.Normal, selectedAppOnly.tile(HomeControl.SystemAudio).tone)
    }

    @Test
    fun setupPillLabelSummarizesActiveDisplayAndProfile() {
        assertNull(HomeControlLabels.setupPillLabel(idle))

        val withBoth = idle.copy(
            displays = listOf(CompanionDisplayInfo("d1", "Studio Display", isActive = true)),
            profiles = listOf(CompanionProfileSummary("p1", "Competitive 120fps", "1440p", isActive = true))
        )
        assertEquals("Studio Display · Competitive 120fps", HomeControlLabels.setupPillLabel(withBoth))

        val displayOnly = idle.copy(
            displays = listOf(CompanionDisplayInfo("d1", "Built-in Retina Display", isActive = true))
        )
        assertEquals("Built-in Retina Display", HomeControlLabels.setupPillLabel(displayOnly))

        val profileOnly = idle.copy(
            profiles = listOf(CompanionProfileSummary("p1", "Streaming", "1080p", isActive = true))
        )
        assertEquals("Streaming", HomeControlLabels.setupPillLabel(profileOnly))

        val noneActive = idle.copy(
            displays = listOf(CompanionDisplayInfo("d1", "Display 1", isActive = false)),
            capturedDisplayName = null
        )
        assertEquals("Display & Profiles", HomeControlLabels.setupPillLabel(noneActive))
    }

    @Test
    fun volumePercentageLabelRoundsAndClampsCleanly() {
        assertEquals("0%", HomeControlLabels.volumePercentageLabel(0.0))
        assertEquals("65%", HomeControlLabels.volumePercentageLabel(0.654))
        assertEquals("100%", HomeControlLabels.volumePercentageLabel(1.0))
        assertEquals("100%", HomeControlLabels.volumePercentageLabel(1.4))
        assertEquals("0%", HomeControlLabels.volumePercentageLabel(-0.3))
    }
}
