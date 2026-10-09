package com.replaymac.companion

import com.replaymac.companion.data.model.CompanionClipSummary
import com.replaymac.companion.data.model.CompanionStateSnapshot
import com.replaymac.companion.ui.components.TileTone
import com.replaymac.companion.ui.components.clipDetailLine
import com.replaymac.companion.ui.components.meterFillSuffix
import com.replaymac.companion.ui.screens.ControlTileSpec
import com.replaymac.companion.ui.screens.HomeControl
import com.replaymac.companion.ui.screens.HomeControlLabels
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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

        val running = buffering.copy(recordingElapsedSeconds = 65.0, capturedDisplayName = "Studio Display")
        assertEquals("Replay buffer running", HomeControlLabels.headline(running))
        assertEquals("01:05 · Studio Display", HomeControlLabels.subheadline(running))

        val both = running.copy(isSessionRecording = true, sessionElapsedSeconds = 5.0)
        assertEquals("Session recording", HomeControlLabels.headline(both))
        assertEquals("00:05 · Studio Display", HomeControlLabels.subheadline(both))
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
}
