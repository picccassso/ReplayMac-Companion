import XCTest
@testable import ReplayCapCompanion

final class CompanionModelTests: XCTestCase {

    func testHelloMessageEncoding() throws {
        let msg = CompanionClientMessage.hello(
            deviceId: "test-device-123",
            deviceName: "iPhone 16 Pro",
            clientVersion: "1.0.0"
        )
        let encoded = try CompanionProtocolParser.encodeClientMessage(msg)
        XCTAssertTrue(encoded.contains("\"type\":\"hello\""))
        XCTAssertTrue(encoded.contains("\"deviceId\":\"test-device-123\""))
        XCTAssertTrue(encoded.contains("\"deviceName\":\"iPhone 16 Pro\""))
    }

    func testActionMessageEncoding() throws {
        let msg = CompanionClientMessage.action(CompanionActions.saveReplay)
        let encoded = try CompanionProtocolParser.encodeClientMessage(msg)
        XCTAssertTrue(encoded.contains("\"type\":\"action\""))
        XCTAssertTrue(encoded.contains("\"action\":\"saveReplay\""))
    }

    func testClipActionsEncoding() throws {
        let thumbMsg = CompanionClientMessage.getThumbnail("clip-456")
        let thumbEncoded = try CompanionProtocolParser.encodeClientMessage(thumbMsg)
        XCTAssertTrue(thumbEncoded.contains("\"clipId\":\"clip-456\""))
        XCTAssertTrue(thumbEncoded.contains("\"action\":\"get_thumbnail\""))

        let revealMsg = CompanionClientMessage.revealClip("clip-456")
        let revealEncoded = try CompanionProtocolParser.encodeClientMessage(revealMsg)
        XCTAssertTrue(revealEncoded.contains("\"type\":\"reveal_clip\""))
        XCTAssertTrue(revealEncoded.contains("\"action\":\"reveal_clip\""))
        XCTAssertTrue(revealEncoded.contains("\"clipId\":\"clip-456\""))

        let deleteMsg = CompanionClientMessage.deleteClip("clip-456")
        let deleteEncoded = try CompanionProtocolParser.encodeClientMessage(deleteMsg)
        XCTAssertTrue(deleteEncoded.contains("\"type\":\"delete_clip\""))
        XCTAssertTrue(deleteEncoded.contains("\"action\":\"delete_clip\""))
        XCTAssertTrue(deleteEncoded.contains("\"clipId\":\"clip-456\""))

        let trimMsg = CompanionClientMessage.trimClip("clip-456")
        let trimEncoded = try CompanionProtocolParser.encodeClientMessage(trimMsg)
        XCTAssertTrue(trimEncoded.contains("\"type\":\"trim_clip\""))
        XCTAssertTrue(trimEncoded.contains("\"action\":\"trim_clip\""))
        XCTAssertTrue(trimEncoded.contains("\"clipId\":\"clip-456\""))

        let copyMsg = CompanionClientMessage.copyClip("clip-456")
        let copyEncoded = try CompanionProtocolParser.encodeClientMessage(copyMsg)
        XCTAssertTrue(copyEncoded.contains("\"type\":\"copy_clip\""))
        XCTAssertTrue(copyEncoded.contains("\"action\":\"copy_clip\""))
        XCTAssertTrue(copyEncoded.contains("\"clipId\":\"clip-456\""))

        let volMsg = CompanionClientMessage.setVolume(target: "microphone", volume: 0.75)
        let volEncoded = try CompanionProtocolParser.encodeClientMessage(volMsg)
        XCTAssertTrue(volEncoded.contains("\"type\":\"set_volume\""))
        XCTAssertTrue(volEncoded.contains("\"target\":\"microphone\""))
        XCTAssertTrue(volEncoded.contains("\"volume\":0.75"))

        let muteAppMsg = CompanionClientMessage.toggleAppMute("com.hnc.Discord")
        let muteAppEncoded = try CompanionProtocolParser.encodeClientMessage(muteAppMsg)
        XCTAssertTrue(muteAppEncoded.contains("\"type\":\"toggle_app_mute\""))
        XCTAssertTrue(muteAppEncoded.contains("\"bundleId\":\"com.hnc.Discord\""))

        let unmuteAllMsg = CompanionClientMessage.unmuteAllApps()
        let unmuteAllEncoded = try CompanionProtocolParser.encodeClientMessage(unmuteAllMsg)
        XCTAssertTrue(unmuteAllEncoded.contains("\"type\":\"unmute_all_apps\""))

        let displayMsg = CompanionClientMessage.selectDisplay("builtin:main")
        let displayEncoded = try CompanionProtocolParser.encodeClientMessage(displayMsg)
        XCTAssertTrue(displayEncoded.contains("\"type\":\"select_display\""))
        XCTAssertTrue(displayEncoded.contains("\"displayId\":\"builtin:main\""))

        let profileMsg = CompanionClientMessage.applyProfile("prof-123")
        let profileEncoded = try CompanionProtocolParser.encodeClientMessage(profileMsg)
        XCTAssertTrue(profileEncoded.contains("\"type\":\"apply_profile\""))
        XCTAssertTrue(profileEncoded.contains("\"profileId\":\"prof-123\""))
    }

    func testSnapshotPreflightLogic() {
        // Idle state: cannot save
        let idle = CompanionStateSnapshot(
            isRecording: false,
            saveStatus: "idle",
            bufferedSeconds: 0.0
        )
        XCTAssertFalse(idle.canSaveQuickReplay)
        XCTAssertFalse(idle.canSaveExtendedReplay)
        XCTAssertFalse(idle.isSaveInProgress)
        XCTAssertFalse(idle.isSaveSuccess)
        XCTAssertFalse(idle.isSaveFailed)

        // Recording but under 1s buffered
        let buffering = CompanionStateSnapshot(
            isRecording: true,
            saveStatus: "idle",
            bufferedSeconds: 0.5,
            longBufferEnabled: true
        )
        XCTAssertFalse(buffering.canSaveQuickReplay)
        XCTAssertTrue(buffering.canSaveExtendedReplay)

        // Ready to save
        let ready = CompanionStateSnapshot(
            isRecording: true,
            saveStatus: "idle",
            bufferedSeconds: 25.0,
            longBufferEnabled: true
        )
        XCTAssertTrue(ready.canSaveQuickReplay)
        XCTAssertTrue(ready.canSaveExtendedReplay)

        // Saving in progress
        let saving = CompanionStateSnapshot(
            isRecording: true,
            saveStatus: "saving",
            bufferedSeconds: 25.0,
            longBufferEnabled: true
        )
        XCTAssertTrue(saving.isSaveInProgress)
        XCTAssertFalse(saving.canSaveQuickReplay)
        XCTAssertFalse(saving.canSaveExtendedReplay)

        // Save success state
        let saved = CompanionStateSnapshot(
            isRecording: true,
            saveStatus: "saved",
            bufferedSeconds: 25.0
        )
        XCTAssertTrue(saved.isSaveSuccess)
        XCTAssertFalse(saved.isSaveInProgress)

        // Save failed state
        let failed = CompanionStateSnapshot(
            isRecording: true,
            saveStatus: "failed",
            bufferedSeconds: 25.0
        )
        XCTAssertTrue(failed.isSaveFailed)
        XCTAssertFalse(failed.isSaveInProgress)
    }
}
