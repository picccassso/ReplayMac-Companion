package com.replaymac.companion

import com.replaymac.companion.data.model.CompanionActions
import com.replaymac.companion.data.model.CompanionClientMessage
import com.replaymac.companion.data.model.CompanionClipSummary
import com.replaymac.companion.data.model.CompanionStateSnapshot
import com.replaymac.companion.data.network.CompanionProtocolParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
}
