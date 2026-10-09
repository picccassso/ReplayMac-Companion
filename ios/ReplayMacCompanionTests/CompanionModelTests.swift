import XCTest
@testable import ReplayMacCompanion

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
