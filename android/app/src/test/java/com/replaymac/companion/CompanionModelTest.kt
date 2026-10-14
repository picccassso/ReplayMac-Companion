package com.replaymac.companion

import com.replaymac.companion.data.model.CompanionActions
import com.replaymac.companion.data.model.CompanionAudioApp
import com.replaymac.companion.data.model.CompanionClientMessage
import com.replaymac.companion.data.model.CompanionClipSummary
import com.replaymac.companion.data.model.CompanionDisplayInfo
import com.replaymac.companion.data.model.CompanionProfileSummary
import com.replaymac.companion.data.model.CompanionStateSnapshot
import com.replaymac.companion.data.network.CompanionProtocolParser
import com.replaymac.companion.ui.components.CompanionHaptics
import com.replaymac.companion.ui.components.SaveHapticOutcome
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CompanionModelTest {

    @Test
    fun testHelloMessageEncoding() {
        val msg = CompanionClientMessage.hello(
            deviceId = "test-device-123",
            deviceName = "Pixel 9 Pro",
            clientVersion = "1.0.0"
        )
        val encoded = CompanionProtocolParser.encodeClientMessage(msg)
        assertTrue(encoded.contains("\"type\":\"hello\""))
        assertTrue(encoded.contains("\"deviceId\":\"test-device-123\""))
        assertTrue(encoded.contains("\"deviceName\":\"Pixel 9 Pro\""))
        assertTrue(encoded.contains("\"protocolVersion\":2"))
    }

    @Test
    fun authProofEncodesProofOrLeavesItOut() {
        val withProof = CompanionProtocolParser.encodeClientMessage(CompanionClientMessage.authProof("cHJvb2Y="))
        assertTrue(withProof.contains("\"type\":\"auth_proof\""))
        assertTrue(withProof.contains("\"proof\":\"cHJvb2Y=\""))

        val withoutProof = CompanionProtocolParser.encodeClientMessage(CompanionClientMessage.authProof(null))
        assertTrue(withoutProof.contains("\"type\":\"auth_proof\""))
        assertFalse(withoutProof.contains("\"proof\":\""))
    }

    @Test
    fun testActionMessageEncoding() {
        val msg = CompanionClientMessage.action(CompanionActions.SAVE_REPLAY)
        val encoded = CompanionProtocolParser.encodeClientMessage(msg)
        assertTrue(encoded.contains("\"type\":\"action\""))
        assertTrue(encoded.contains("\"action\":\"saveReplay\""))
    }

    @Test
    fun testClipActionsEncoding() {
        val thumbMsg = CompanionClientMessage.getThumbnail("clip-456")
        val thumbEncoded = CompanionProtocolParser.encodeClientMessage(thumbMsg)
        assertTrue(thumbEncoded.contains("\"clipId\":\"clip-456\""))
        assertTrue(thumbEncoded.contains("\"action\":\"get_thumbnail\""))

        val revealMsg = CompanionClientMessage.revealClip("clip-456")
        val revealEncoded = CompanionProtocolParser.encodeClientMessage(revealMsg)
        assertTrue(revealEncoded.contains("\"type\":\"reveal_clip\""))
        assertTrue(revealEncoded.contains("\"action\":\"reveal_clip\""))
        assertTrue(revealEncoded.contains("\"clipId\":\"clip-456\""))

        val deleteMsg = CompanionClientMessage.deleteClip("clip-456")
        val deleteEncoded = CompanionProtocolParser.encodeClientMessage(deleteMsg)
        assertTrue(deleteEncoded.contains("\"type\":\"delete_clip\""))
        assertTrue(deleteEncoded.contains("\"action\":\"delete_clip\""))
        assertTrue(deleteEncoded.contains("\"clipId\":\"clip-456\""))
    }

    @Test
    fun testSnapshotPreflightLogic() {
        // Idle state: cannot save
        val idle = CompanionStateSnapshot(
            isRecording = false,
            bufferedSeconds = 0.0,
            saveStatus = "idle"
        )
        assertFalse(idle.canSaveQuickReplay)
        assertFalse(idle.canSaveExtendedReplay)
        assertFalse(idle.isSaveInProgress)
        assertFalse(idle.isSaveSuccess)
        assertFalse(idle.isSaveFailed)

        // Recording but under 1s buffered
        val buffering = CompanionStateSnapshot(
            isRecording = true,
            bufferedSeconds = 0.5,
            saveStatus = "idle",
            longBufferEnabled = true
        )
        assertFalse(buffering.canSaveQuickReplay)
        assertTrue(buffering.canSaveExtendedReplay)

        // Ready to save
        val ready = CompanionStateSnapshot(
            isRecording = true,
            bufferedSeconds = 25.0,
            saveStatus = "idle",
            longBufferEnabled = true
        )
        assertTrue(ready.canSaveQuickReplay)
        assertTrue(ready.canSaveExtendedReplay)

        // Saving in progress: saving disabled
        val saving = CompanionStateSnapshot(
            isRecording = true,
            bufferedSeconds = 25.0,
            saveStatus = "saving",
            longBufferEnabled = true
        )
        assertTrue(saving.isSaveInProgress)
        assertFalse(saving.canSaveQuickReplay)
        assertFalse(saving.canSaveExtendedReplay)

        // Save success state
        val saved = CompanionStateSnapshot(
            isRecording = true,
            bufferedSeconds = 25.0,
            saveStatus = "saved"
        )
        assertTrue(saved.isSaveSuccess)
        assertFalse(saved.isSaveInProgress)

        // Save failed state
        val failed = CompanionStateSnapshot(
            isRecording = true,
            bufferedSeconds = 25.0,
            saveStatus = "failed"
        )
        assertTrue(failed.isSaveFailed)
        assertFalse(failed.isSaveInProgress)
    }

    @Test
    fun testTrimAndCopyClipEncoding() {
        val trimEncoded = CompanionProtocolParser.encodeClientMessage(CompanionClientMessage.trimClip("clip-77"))
        assertTrue(trimEncoded.contains("\"type\":\"trim_clip\""))
        assertTrue(trimEncoded.contains("\"action\":\"trim_clip\""))
        assertTrue(trimEncoded.contains("\"clipId\":\"clip-77\""))

        val copyEncoded = CompanionProtocolParser.encodeClientMessage(CompanionClientMessage.copyClip("clip-77"))
        assertTrue(copyEncoded.contains("\"type\":\"copy_clip\""))
        assertTrue(copyEncoded.contains("\"action\":\"copy_clip\""))
        assertTrue(copyEncoded.contains("\"clipId\":\"clip-77\""))
    }

    @Test
    fun testAudioMixerAndCaptureSetupMessageEncoding() {
        val setVol = CompanionProtocolParser.encodeClientMessage(CompanionClientMessage.setVolume("microphone", 0.75))
        assertTrue(setVol.contains("\"type\":\"set_volume\""))
        assertTrue(setVol.contains("\"action\":\"set_volume\""))
        assertTrue(setVol.contains("\"target\":\"microphone\""))
        assertTrue(setVol.contains("\"volume\":0.75"))

        val clampedHigh = CompanionClientMessage.setVolume("systemAudio", 1.5)
        assertEquals(1.0, clampedHigh.volume!!, 0.0001)
        val clampedLow = CompanionClientMessage.setVolume("systemAudio", -0.2)
        assertEquals(0.0, clampedLow.volume!!, 0.0001)

        val toggleMute = CompanionProtocolParser.encodeClientMessage(CompanionClientMessage.toggleAppMute("com.spotify.client"))
        assertTrue(toggleMute.contains("\"type\":\"toggle_app_mute\""))
        assertTrue(toggleMute.contains("\"action\":\"toggle_app_mute\""))
        assertTrue(toggleMute.contains("\"bundleId\":\"com.spotify.client\""))

        val unmuteAll = CompanionProtocolParser.encodeClientMessage(CompanionClientMessage.unmuteAllApps())
        assertTrue(unmuteAll.contains("\"type\":\"unmute_all_apps\""))
        assertTrue(unmuteAll.contains("\"action\":\"unmute_all_apps\""))

        val selectDisplay = CompanionProtocolParser.encodeClientMessage(CompanionClientMessage.selectDisplay("display-2"))
        assertTrue(selectDisplay.contains("\"type\":\"select_display\""))
        assertTrue(selectDisplay.contains("\"action\":\"select_display\""))
        assertTrue(selectDisplay.contains("\"displayId\":\"display-2\""))

        val applyProfile = CompanionProtocolParser.encodeClientMessage(CompanionClientMessage.applyProfile("profile-hdr"))
        assertTrue(applyProfile.contains("\"type\":\"apply_profile\""))
        assertTrue(applyProfile.contains("\"action\":\"apply_profile\""))
        assertTrue(applyProfile.contains("\"profileId\":\"profile-hdr\""))
    }

    @Test
    fun testSnapshotAudioAndSetupComputedProperties() {
        val apps = listOf(
            CompanionAudioApp(bundleId = "a", name = "App A", isMuted = true),
            CompanionAudioApp(bundleId = "b", name = "App B", isMuted = true),
            CompanionAudioApp(bundleId = "c", name = "App C", isMuted = false)
        )
        val base = CompanionStateSnapshot(
            isSystemAudioEnabled = true,
            perAppAudioEnabled = false,
            audioApps = apps,
            capturedDisplayName = "Fallback Display"
        )
        assertEquals(2, base.mutedAudioAppsCount)
        assertEquals("Fallback Display", base.activeDisplayName)
        assertNull(base.activeProfileName)
        assertFalse(base.hasCaptureSetupOptions)

        // Muted app count is 0 when system audio is off or per-app audio mode is enabled
        assertEquals(0, base.copy(isSystemAudioEnabled = false).mutedAudioAppsCount)
        assertEquals(0, base.copy(perAppAudioEnabled = true).mutedAudioAppsCount)

        val withSetup = base.copy(
            displays = listOf(
                CompanionDisplayInfo(id = "d1", name = "Main Display", isActive = false),
                CompanionDisplayInfo(id = "d2", name = "Studio Display", isActive = true)
            ),
            profiles = listOf(
                CompanionProfileSummary(id = "p1", name = "60fps High", summary = "1080p", isActive = true)
            )
        )
        assertTrue(withSetup.hasCaptureSetupOptions)
        assertEquals("Studio Display", withSetup.activeDisplayName)
        assertEquals("60fps High", withSetup.activeProfileName)
    }

    @Test
    fun testSaveHapticTransitions() {
        assertEquals(SaveHapticOutcome.SUCCESS, CompanionHaptics.saveTransitionOutcome("saving", "saved"))
        assertEquals(SaveHapticOutcome.SUCCESS, CompanionHaptics.saveTransitionOutcome("BUSY", "Saved"))
        assertEquals(SaveHapticOutcome.FAILURE, CompanionHaptics.saveTransitionOutcome("saving", "failed"))
        assertEquals(SaveHapticOutcome.FAILURE, CompanionHaptics.saveTransitionOutcome("busy", "FAILED"))

        assertNull(CompanionHaptics.saveTransitionOutcome("idle", "saving"))
        assertNull(CompanionHaptics.saveTransitionOutcome("idle", "saved"))
        assertNull(CompanionHaptics.saveTransitionOutcome("saved", "idle"))
        assertNull(CompanionHaptics.saveTransitionOutcome("saved", "saved"))
        assertNull(CompanionHaptics.saveTransitionOutcome("saving", "idle"))
    }
}
