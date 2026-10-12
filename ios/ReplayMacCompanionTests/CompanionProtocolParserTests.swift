import XCTest
@testable import ReplayCapCompanion

final class CompanionProtocolParserTests: XCTestCase {

    func testParseAuthResponse() {
        let approvedJson = """
        {
            "type": "auth_response",
            "status": "approved",
            "serverName": "Alex's MacBook Pro",
            "serverVersion": "1.7.3"
        }
        """

        let approvedEvent = CompanionProtocolParser.parse(approvedJson)
        guard case .auth(let auth) = approvedEvent else {
            XCTFail("Expected .auth event")
            return
        }
        XCTAssertEqual(auth.status, "approved")
        XCTAssertEqual(auth.serverName, "Alex's MacBook Pro")
        XCTAssertEqual(auth.serverVersion, "1.7.3")

        let pendingJson = """
        {"type":"auth_response","status":"pending","serverName":"Mac Mini","serverVersion":"1.0"}
        """
        let pendingEvent = CompanionProtocolParser.parse(pendingJson)
        guard case .auth(let pendingAuth) = pendingEvent else {
            XCTFail("Expected .auth event")
            return
        }
        XCTAssertEqual(pendingAuth.status, "pending")

        let renamedJson = """
        {
            "type": "auth_response",
            "status": "approved",
            "serverName": "Living Room Mac",
            "serverVersion": "1.7.3",
            "deviceName": "Alex's iPhone"
        }
        """
        let renamedEvent = CompanionProtocolParser.parse(renamedJson)
        guard case .auth(let renamedAuth) = renamedEvent else {
            XCTFail("Expected .auth event")
            return
        }
        XCTAssertEqual(renamedAuth.serverName, "Living Room Mac")
        XCTAssertEqual(renamedAuth.deviceName, "Alex's iPhone")
    }

    func testParseStateSnapshot() {
        let snapshotJson = """
        {
            "type": "state_snapshot",
            "isRecording": true,
            "isSessionRecording": false,
            "saveStatus": "idle",
            "recordingElapsedSeconds": 42.5,
            "extendedBufferElapsedSeconds": 120.0,
            "sessionElapsedSeconds": 0.0,
            "bufferedSeconds": 30.0,
            "bufferDurationSeconds": 30,
            "bufferMemoryBytes": 104857600,
            "bufferMemoryFormatted": "100 MB",
            "longBufferEnabled": true,
            "longBufferDurationSeconds": 300,
            "capturedDisplayName": "Built-in Retina Display",
            "isUsingFallbackDisplay": true,
            "isMicrophoneEnabled": true,
            "isMicrophoneMuted": false,
            "isSystemAudioEnabled": true,
            "isSystemAudioMuted": false,
            "recentClips": [
                {
                    "id": "clip-1",
                    "filename": "Replay 2026-10-08 14.00.00.mov",
                    "kind": "replay",
                    "savedAt": 1728392400.0,
                    "duration": 30.0,
                    "fileSize": 45000000,
                    "hasThumbnail": true
                }
            ]
        }
        """

        let event = CompanionProtocolParser.parse(snapshotJson)
        guard case .snapshot(let snapshot) = event else {
            XCTFail("Expected .snapshot event")
            return
        }
        XCTAssertTrue(snapshot.isRecording)
        XCTAssertEqual(snapshot.recordingElapsedSeconds, 42.5, accuracy: 0.01)
        XCTAssertEqual(snapshot.bufferDurationSeconds, 30)
        XCTAssertEqual(snapshot.bufferMemoryFormatted, "100 MB")
        XCTAssertEqual(snapshot.capturedDisplayName, "Built-in Retina Display")
        XCTAssertTrue(snapshot.isUsingFallbackDisplay)
        XCTAssertEqual(snapshot.recentClips.count, 1)
        XCTAssertEqual(snapshot.recentClips[0].id, "clip-1")
        XCTAssertTrue(snapshot.recentClips[0].hasThumbnail)
    }

    func testParseStateChanged() {
        let json = """
        {
            "type": "state_changed",
            "isRecording": false,
            "isSessionRecording": true,
            "saveStatus": "saving",
            "recordingElapsedSeconds": 0.0,
            "extendedBufferElapsedSeconds": 0.0,
            "sessionElapsedSeconds": 15.0,
            "bufferedSeconds": 0.0,
            "bufferDurationSeconds": 30,
            "bufferMemoryBytes": 0,
            "bufferMemoryFormatted": "0 MB",
            "longBufferEnabled": false,
            "longBufferDurationSeconds": 300,
            "capturedDisplayName": null,
            "isMicrophoneEnabled": false,
            "isMicrophoneMuted": false,
            "isSystemAudioEnabled": true,
            "isSystemAudioMuted": false,
            "recentClips": []
        }
        """

        let event = CompanionProtocolParser.parse(json)
        guard case .stateChanged(let snapshot) = event else {
            XCTFail("Expected .stateChanged event")
            return
        }
        XCTAssertTrue(snapshot.isSessionRecording)
        XCTAssertEqual(snapshot.saveStatus, "saving")
        XCTAssertTrue(snapshot.isSaveInProgress)
        XCTAssertFalse(snapshot.isUsingFallbackDisplay)
    }

    func testParseTelemetryTick() {
        let tickJson = """
        {
            "type": "telemetry_tick",
            "recordingElapsedSeconds": 12.3,
            "extendedBufferElapsedSeconds": 45.6,
            "sessionElapsedSeconds": 0.0,
            "bufferedSeconds": 12.3,
            "bufferMemoryBytes": 52428800,
            "bufferMemoryFormatted": "50 MB"
        }
        """

        let event = CompanionProtocolParser.parse(tickJson)
        guard case .telemetry(let tick) = event else {
            XCTFail("Expected .telemetry event")
            return
        }
        XCTAssertEqual(tick.recordingElapsedSeconds, 12.3, accuracy: 0.01)
        XCTAssertEqual(tick.bufferMemoryBytes, 52428800)
        XCTAssertEqual(tick.bufferMemoryFormatted, "50 MB")
    }

    func testParseThumbnailResponse() {
        let thumbJson = """
        {
            "type": "thumbnail_response",
            "clipId": "clip-99",
            "thumbnailBase64": "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg=="
        }
        """

        let event = CompanionProtocolParser.parse(thumbJson)
        guard case .thumbnail(let clipId, let base64) = event else {
            XCTFail("Expected .thumbnail event")
            return
        }
        XCTAssertEqual(clipId, "clip-99")
        XCTAssertTrue(base64?.hasPrefix("iVBORw0") == true)
    }

    func testParseThumbnailResponseWithNullBase64() {
        let thumbJson = """
        {
            "type": "thumbnail_response",
            "clipId": "clip-pending",
            "thumbnailBase64": null
        }
        """

        let event = CompanionProtocolParser.parse(thumbJson)
        guard case .thumbnail(let clipId, let base64) = event else {
            XCTFail("Expected .thumbnail event")
            return
        }
        XCTAssertEqual(clipId, "clip-pending")
        XCTAssertNil(base64)
    }

    func testParseDeniedAuthResponse() {
        let deniedJson = """
        {
            "type": "auth_response",
            "status": "denied",
            "serverName": "Security Mac",
            "serverVersion": "1.7.3"
        }
        """

        let event = CompanionProtocolParser.parse(deniedJson)
        guard case .auth(let auth) = event else {
            XCTFail("Expected .auth event")
            return
        }
        XCTAssertEqual(auth.status, "denied")
        XCTAssertEqual(auth.serverName, "Security Mac")
    }

    func testParsePongAndError() {
        let pongEvent = CompanionProtocolParser.parse("{\"type\":\"pong\"}")
        XCTAssertEqual(pongEvent, .pong)

        let errEvent = CompanionProtocolParser.parse("{\"type\":\"error\",\"message\":\"Invalid token\"}")
        guard case .serverError(let err) = errEvent else {
            XCTFail("Expected .serverError event")
            return
        }
        XCTAssertEqual(err.message, "Invalid token")
        XCTAssertFalse(err.endsConnection, "An error without a code doesn't explain a disconnect")

        let busy = CompanionProtocolParser.parse("{\"type\":\"error\",\"message\":\"Busy\",\"code\":\"busy\"}")
        guard case .serverError(let busyErr) = busy else {
            XCTFail("Expected .serverError event")
            return
        }
        XCTAssertEqual(busyErr.code, "busy")
        XCTAssertTrue(busyErr.endsConnection)
    }

    func testParseAuthChallengeAndPairingFields() {
        let challenge = CompanionProtocolParser.parse(#"{"type":"auth_challenge","nonce":"n0nce","serverId":"mac-1","serverName":"Studio","serverVersion":"1.8"}"#)
        XCTAssertEqual(challenge, .challenge(CompanionAuthChallenge(nonce: "n0nce", serverId: "mac-1", serverName: "Studio", serverVersion: "1.8")))

        guard case .auth(let pending) = CompanionProtocolParser.parse(#"{"type":"auth_response","status":"pending","serverName":"Studio","serverVersion":"1.8","pairingCode":"0420"}"#) else {
            return XCTFail("Expected .auth event")
        }
        XCTAssertEqual(pending.pairingCode, "0420")

        guard case .auth(let denied) = CompanionProtocolParser.parse(#"{"type":"auth_response","status":"denied","serverName":"Studio","serverVersion":"1.8","reason":"revoked"}"#) else {
            return XCTFail("Expected .auth event")
        }
        XCTAssertEqual(denied.reason, "revoked")
    }

    func testHelloAnnouncesProtocolVersion() throws {
        let json = try CompanionProtocolParser.encodeClientMessage(.hello(deviceId: "d", deviceName: "n"))
        XCTAssertTrue(json.contains(#""protocolVersion":2"#), json)
    }

    func testParseMalformedOrUnknown() {
        let unknownEvent = CompanionProtocolParser.parse("{\"type\":\"custom_future_type\",\"foo\":\"bar\"}")
        guard case .unknown(let rawType, _) = unknownEvent else {
            XCTFail("Expected .unknown event")
            return
        }
        XCTAssertEqual(rawType, "custom_future_type")

        let malformedEvent = CompanionProtocolParser.parse("NOT_JSON")
        guard case .error = malformedEvent else {
            XCTFail("Expected .error event for malformed input")
            return
        }
    }

    func testAuthResponseCarriesTheMacsStableIdWhenSent() {
        let withId = CompanionProtocolParser.parse(
            "{\"type\":\"auth_response\",\"status\":\"approved\",\"serverName\":\"Mac\",\"serverVersion\":\"1.7.3\",\"serverId\":\"5F1C\"}"
        )
        guard case .auth(let authWithId) = withId else {
            XCTFail("Expected .auth event")
            return
        }
        XCTAssertEqual(authWithId.serverId, "5F1C")

        let withoutId = CompanionProtocolParser.parse(
            "{\"type\":\"auth_response\",\"status\":\"approved\",\"serverName\":\"Mac\",\"serverVersion\":\"1.7.3\"}"
        )
        guard case .auth(let authWithoutId) = withoutId else {
            XCTFail("Expected .auth event")
            return
        }
        XCTAssertNil(authWithoutId.serverId)
    }
}
