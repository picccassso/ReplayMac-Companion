package com.replaymac.companion

import com.replaymac.companion.data.demo.DemoClipKind
import com.replaymac.companion.data.demo.DemoCompanionState
import com.replaymac.companion.data.demo.DemoSaveStatus
import com.replaymac.companion.data.model.CompanionVolumeTargets
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneOffset

class DemoCompanionStateTest {

    private val now = 1_760_000_000L // 2025-10-09 08:53:20 UTC
    private val initial = DemoCompanionState.initial(now, ZoneOffset.UTC)

    /** The buffer restarted from empty, so ticks are visible before it fills. */
    private val freshBuffer = initial.toggleBuffer().toggleBuffer()

    private fun DemoCompanionState.clipIds() = recentClips.map { it.id }

    // ==================== Sample data ====================

    @Test
    fun startsRecordingWithAFullBufferOnTheStudioDisplay() {
        val snapshot = initial.toSnapshot()

        assertTrue(snapshot.isRecording)
        assertFalse(snapshot.isSessionRecording)
        assertEquals(30.0, snapshot.bufferedSeconds, 0.0)
        assertEquals(411_000_000L, snapshot.bufferMemoryBytes)
        assertEquals("411 MB", snapshot.bufferMemoryFormatted)
        assertEquals("Studio Display", snapshot.capturedDisplayName)
        assertEquals("Gaming", snapshot.activeProfileName)
        assertEquals(1, snapshot.mutedAudioAppsCount)
        assertTrue(snapshot.canSaveQuickReplay)
        assertTrue(snapshot.canSaveExtendedReplay)
    }

    @Test
    fun sampleClipsAreNamedFromWhenTheyWereSaved() {
        val clips = initial.recentClips

        assertEquals(listOf("demo-clip-1", "demo-clip-2", "demo-clip-3"), clips.map { it.id })
        assertEquals(listOf("replay", "session", "extended replay"), clips.map { it.kind })
        assertEquals(listOf(now - 300.0, now - 3_600.0, now - 86_400.0), clips.map { it.savedAt })
        assertEquals(
            listOf(
                "Replay 2025-10-09 at 08.48.20.mp4",
                "Session 2025-10-09 at 07.53.20.mp4",
                "Extended Replay 2025-10-08 at 08.53.20.mp4"
            ),
            clips.map { it.filename }
        )
        assertTrue(clips.all { it.hasThumbnail })
    }

    @Test
    fun memoryIsShownInWholeMegabytes() {
        assertEquals("0 MB", DemoCompanionState.formatMemory(0))
        assertEquals("1 MB", DemoCompanionState.formatMemory(1_499_999))
        assertEquals("2 MB", DemoCompanionState.formatMemory(1_500_000))
        assertEquals("41 MB", freshBuffer.tick(3.0).toSnapshot().bufferMemoryFormatted)
    }

    // ==================== Recording ====================

    @Test
    fun tickFillsBuffersButNoFurtherThanTheirLength() {
        val full = initial.tick(5.0)
        assertEquals(759.0, full.recordingElapsedSeconds, 0.0)
        assertEquals(30.0, full.bufferedSeconds, 0.0)
        assertEquals(300.0, full.extendedBufferElapsedSeconds, 0.0)

        val filling = freshBuffer.tick(3.0)
        assertEquals(3.0, filling.bufferedSeconds, 0.0)
        assertEquals(3.0, filling.extendedBufferElapsedSeconds, 0.0)
    }

    @Test
    fun tickRunsTheSessionOnlyWhileOneIsRecording() {
        assertEquals(0.0, initial.tick(4.0).sessionElapsedSeconds, 0.0)
        assertEquals(4.0, initial.toggleSession(now).tick(4.0).sessionElapsedSeconds, 0.0)
    }

    @Test
    fun stoppingTheBufferEmptiesItAndStopsTheClock() {
        val stopped = initial.toggleBuffer()

        assertFalse(stopped.isRecording)
        assertEquals(0.0, stopped.recordingElapsedSeconds, 0.0)
        assertEquals(0.0, stopped.bufferedSeconds, 0.0)
        assertEquals(0.0, stopped.extendedBufferElapsedSeconds, 0.0)
        assertEquals("0 MB", stopped.toSnapshot().bufferMemoryFormatted)
        assertEquals(stopped, stopped.tick(10.0))

        val restarted = stopped.toggleBuffer()
        assertTrue(restarted.isRecording)
        assertEquals(0.0, restarted.bufferedSeconds, 0.0)
    }

    @Test
    fun stoppingASessionSavesItAsTheNewestClip() {
        val stopped = initial.toggleSession(now).tick(65.0).toggleSession(now + 65)

        assertFalse(stopped.isSessionRecording)
        assertEquals(0.0, stopped.sessionElapsedSeconds, 0.0)
        val clip = stopped.recentClips.first()
        assertEquals("demo-clip-4", clip.id)
        assertEquals("session", clip.kind)
        assertEquals(65.0, clip.duration!!, 0.0)
        assertEquals(104_000_000L, clip.fileSize)
        assertEquals("Session 2025-10-09 at 08.54.25.mp4", clip.filename)
    }

    @Test
    fun aSessionShorterThanASecondSavesNothing() {
        val stopped = initial.toggleSession(now).toggleSession(now)

        assertFalse(stopped.isSessionRecording)
        assertEquals(initial.recentClips, stopped.recentClips)
    }

    // ==================== Saving ====================

    @Test
    fun replaySaveGoesFromSavingToSavedWithTheBufferAsAClip() {
        val saving = initial.saveReplay()
        assertEquals(DemoSaveStatus.SAVING, saving.saveStatus)
        assertEquals(initial.recentClips, saving.recentClips)

        val saved = saving.finishSave(now)
        assertEquals(DemoSaveStatus.SAVED, saved.saveStatus)
        val clip = saved.recentClips.first()
        assertEquals("demo-clip-4", clip.id)
        assertEquals("replay", clip.kind)
        assertEquals(30.0, clip.duration!!, 0.0)
        assertEquals(48_000_000L, clip.fileSize)
        assertEquals("Replay 2025-10-09 at 08.53.20.mp4", clip.filename)

        assertEquals(DemoSaveStatus.IDLE, saved.resetSaveStatus().saveStatus)
    }

    @Test
    fun extendedSaveIsAsLongAsTheExtendedBufferHolds() {
        val clip = freshBuffer.tick(120.0).saveExtendedReplay().finishSave(now).recentClips.first()

        assertEquals("extended replay", clip.kind)
        assertEquals(120.0, clip.duration!!, 0.0)
        assertEquals(192_000_000L, clip.fileSize)
        assertTrue(clip.filename.startsWith("Extended Replay "))

        val fullClip = initial.saveExtendedReplay().finishSave(now).recentClips.first()
        assertEquals(300.0, fullClip.duration!!, 0.0)
    }

    @Test
    fun savingFailsWhileTheBufferIsStopped() {
        val stopped = initial.toggleBuffer()

        assertEquals(DemoSaveStatus.FAILED, stopped.saveReplay().saveStatus)
        assertEquals(DemoSaveStatus.FAILED, stopped.saveExtendedReplay().saveStatus)
        assertEquals(initial.recentClips, stopped.saveReplay().finishSave(now).recentClips)
    }

    @Test
    fun aSecondSaveIsIgnoredWhileOneIsInProgress() {
        val saving = initial.saveReplay()

        assertSame(saving, saving.saveExtendedReplay())
        assertEquals("replay", saving.saveExtendedReplay().finishSave(now).recentClips.first().kind)
    }

    @Test
    fun resetLeavesASaveInProgressAlone() {
        val saving = initial.saveReplay()

        assertSame(saving, saving.resetSaveStatus())
        assertEquals(DemoSaveStatus.IDLE, initial.toggleBuffer().saveReplay().resetSaveStatus().saveStatus)
    }

    // ==================== Audio ====================

    @Test
    fun muteTogglesFlipEachSource() {
        val muted = initial.toggleMicrophone().toggleSystemAudio()

        assertTrue(muted.isMicrophoneMuted)
        assertTrue(muted.isSystemAudioMuted)
        assertFalse(muted.toggleMicrophone().isMicrophoneMuted)
    }

    @Test
    fun volumeIsClampedAndUnknownTargetsAreIgnored() {
        assertEquals(1.0, initial.setVolume(CompanionVolumeTargets.MICROPHONE, 1.7).microphoneVolume, 0.0)
        assertEquals(0.0, initial.setVolume(CompanionVolumeTargets.SYSTEM_AUDIO, -0.4).systemAudioVolume, 0.0)
        assertEquals(0.35, initial.setVolume(CompanionVolumeTargets.SYSTEM_AUDIO, 0.35).systemAudioVolume, 0.0)
        assertEquals(initial, initial.setVolume("headphones", 0.2))
    }

    @Test
    fun appsCanBeMutedOneAtATimeAndUnmutedAllAtOnce() {
        val toggled = initial.toggleAppMute("com.apple.Music").toggleAppMute("com.hnc.Discord")
        assertEquals(
            listOf("com.apple.Music"),
            toggled.audioApps.filter { it.isMuted }.map { it.bundleId }
        )

        val allMuted = initial.toggleAppMute("com.apple.Safari").toggleAppMute("com.apple.Music")
        assertTrue(allMuted.unmuteAllApps().audioApps.none { it.isMuted })
    }

    // ==================== Capture setup ====================

    @Test
    fun selectingADisplayMakesItTheOnlyActiveOneAndTheCapturedOne() {
        val snapshot = initial.selectDisplay("demo-display-builtin").toSnapshot()

        assertEquals(listOf("demo-display-builtin"), snapshot.displays.filter { it.isActive }.map { it.id })
        assertEquals("Built-in Retina Display", snapshot.capturedDisplayName)
    }

    @Test
    fun applyingAProfileMakesItTheOnlyActiveOne() {
        val applied = initial.applyProfile("demo-profile-battery")

        assertEquals(listOf("demo-profile-battery"), applied.profiles.filter { it.isActive }.map { it.id })
        assertEquals("Battery Saver", applied.toSnapshot().activeProfileName)
    }

    // ==================== Clips ====================

    @Test
    fun deleteRemovesOnlyThatClip() {
        assertEquals(listOf("demo-clip-1", "demo-clip-3"), initial.deleteClip("demo-clip-2").clipIds())
    }

    @Test
    fun trimAddsAHalfLengthCopyAndKeepsTheOriginal() {
        val trimmed = initial.trimClip("demo-clip-1", now)

        val clip = trimmed.recentClips.first()
        assertEquals("demo-clip-4", clip.id)
        assertEquals("replay", clip.kind)
        assertEquals("Replay 2025-10-09 at 08.48.20 (Trimmed).mp4", clip.filename)
        assertEquals(15.0, clip.duration!!, 0.0)
        assertEquals(24_000_000L, clip.fileSize)
        assertEquals(now.toDouble(), clip.savedAt, 0.0)
        assertTrue(trimmed.clipIds().contains("demo-clip-1"))

        // 15 / 2 rounds half up, the same as on iOS.
        assertEquals(8.0, trimmed.trimClip("demo-clip-4", now).recentClips.first().duration!!, 0.0)
    }

    @Test
    fun trimmingAOneSecondClipKeepsASecond() {
        val oneSecond = initial.toggleSession(now).tick(1.0).toggleSession(now)

        val trimmed = oneSecond.trimClip("demo-clip-4", now).recentClips.first()
        assertEquals(1.0, trimmed.duration!!, 0.0)
    }

    @Test
    fun copyAndRevealAreRecordedWithoutChangingWhatTheAppSees() {
        val acted = initial.copyClip("demo-clip-2").revealClip("demo-clip-3")

        assertEquals("demo-clip-2", acted.lastCopiedClipId)
        assertEquals("demo-clip-3", acted.lastRevealedClipId)
        assertEquals(initial.toSnapshot(), acted.toSnapshot())
    }

    @Test
    fun unknownIdsLeaveTheStateUnchanged() {
        val unknown = "not-a-real-id"
        listOf<(DemoCompanionState) -> DemoCompanionState>(
            { it.deleteClip(unknown) },
            { it.trimClip(unknown, now) },
            { it.copyClip(unknown) },
            { it.revealClip(unknown) },
            { it.toggleAppMute(unknown) },
            { it.selectDisplay(unknown) },
            { it.applyProfile(unknown) }
        ).forEach { rule -> assertEquals(initial, rule(initial)) }
        assertNull(initial.copyClip(unknown).lastCopiedClipId)
    }

    @Test
    fun theClipListKeepsOnlyTheTenNewest() {
        var state = initial
        repeat(8) { state = state.trimClip("demo-clip-1", now) }

        assertEquals(DemoCompanionState.MAX_RECENT_CLIPS, state.recentClips.size)
        assertEquals("demo-clip-11", state.recentClips.first().id)
        assertFalse(state.clipIds().contains("demo-clip-3"))
        assertTrue(state.clipIds().contains("demo-clip-2"))
    }

    @Test
    fun newClipIdsKeepCountingAfterDeletes() {
        val state = initial.deleteClip("demo-clip-1").saveReplay().finishSave(now)

        assertEquals("demo-clip-4", state.recentClips.first().id)
        assertEquals(DemoClipKind.REPLAY.wireName, state.recentClips.first().kind)
    }
}
