import XCTest
@testable import ReplayCapCompanion

final class CompanionConnectionLogicTests: XCTestCase {

    func testTelemetryMergingIntoSnapshot() {
        let initial = CompanionStateSnapshot(
            isRecording: true,
            recordingElapsedSeconds: 10.0,
            bufferedSeconds: 10.0,
            bufferDurationSeconds: 30,
            bufferMemoryBytes: 20000000,
            bufferMemoryFormatted: "20 MB"
        )

        let tick = CompanionTelemetryTick(
            recordingElapsedSeconds: 15.5,
            extendedBufferElapsedSeconds: 45.0,
            sessionElapsedSeconds: 0.0,
            bufferedSeconds: 15.5,
            bufferMemoryBytes: 35000000,
            bufferMemoryFormatted: "35 MB"
        )

        let merged = CompanionStateSnapshot(
            type: initial.type,
            isRecording: initial.isRecording,
            isSessionRecording: initial.isSessionRecording,
            saveStatus: initial.saveStatus,
            recordingElapsedSeconds: tick.recordingElapsedSeconds,
            extendedBufferElapsedSeconds: tick.extendedBufferElapsedSeconds,
            sessionElapsedSeconds: tick.sessionElapsedSeconds,
            bufferedSeconds: tick.bufferedSeconds,
            bufferDurationSeconds: initial.bufferDurationSeconds,
            bufferMemoryBytes: tick.bufferMemoryBytes,
            bufferMemoryFormatted: tick.bufferMemoryFormatted,
            longBufferEnabled: initial.longBufferEnabled,
            longBufferDurationSeconds: initial.longBufferDurationSeconds,
            capturedDisplayName: initial.capturedDisplayName,
            isUsingFallbackDisplay: initial.isUsingFallbackDisplay,
            isMicrophoneEnabled: initial.isMicrophoneEnabled,
            isMicrophoneMuted: initial.isMicrophoneMuted,
            isSystemAudioEnabled: initial.isSystemAudioEnabled,
            isSystemAudioMuted: initial.isSystemAudioMuted,
            recentClips: initial.recentClips
        )

        XCTAssertEqual(merged.recordingElapsedSeconds, 15.5, accuracy: 0.01)
        XCTAssertEqual(merged.bufferMemoryBytes, 35000000)
        XCTAssertEqual(merged.bufferMemoryFormatted, "35 MB")
        XCTAssertEqual(merged.bufferDurationSeconds, 30)
    }

    func testDurationFormatter() {
        XCTAssertEqual(formatDurationSeconds(0.0), "00:00")
        XCTAssertEqual(formatDurationSeconds(5.2), "00:05")
        XCTAssertEqual(formatDurationSeconds(59.9), "00:59")
        XCTAssertEqual(formatDurationSeconds(60.0), "01:00")
        XCTAssertEqual(formatDurationSeconds(330.0), "05:30")
        XCTAssertEqual(formatDurationSeconds(3600.0), "01:00:00")
        XCTAssertEqual(formatDurationSeconds(7510.0), "02:05:10")
        XCTAssertEqual(formatDurationSeconds(-10.0), "00:00")
    }

    func testFileSizeFormatter() {
        XCTAssertEqual(formatFileSize(nil), "")
        XCTAssertEqual(formatFileSize(0), "")
        XCTAssertEqual(formatFileSize(450 * 1024), "450 KB")
        XCTAssertEqual(formatFileSize(5 * 1024 * 1024), "5.0 MB")
        XCTAssertEqual(formatFileSize(Int64(1.5 * 1024 * 1024 * 1024)), "1.5 GB")
    }

    func testClipKindFormatting() {
        XCTAssertEqual(clipKindTitle("replay"), "Replay")
        XCTAssertEqual(clipKindTitle("session"), "Session")
        XCTAssertEqual(clipKindTitle("extended"), "Extended Replay")
        XCTAssertEqual(clipKindTitle("extended replay"), "Extended Replay")
        XCTAssertEqual(clipKindTitle("custom"), "Custom")
    }

    func testConnectionStates() {
        let disconnected = ConnectionState.disconnected
        XCTAssertFalse(disconnected.isConnected)
        XCTAssertFalse(disconnected.isConnecting)

        let connecting = ConnectionState.connecting(host: "192.168.1.5", port: 7375)
        XCTAssertTrue(connecting.isConnecting)

        let pending = ConnectionState.pendingApproval(serverName: "Studio Mac", host: "192.168.1.5", port: 7375)
        XCTAssertTrue(pending.isPendingApproval)

        let reconnecting = ConnectionState.reconnecting(serverName: "Studio Mac")
        XCTAssertTrue(reconnecting.isReconnecting)

        let connected = ConnectionState.connected(serverName: "Studio Mac", serverVersion: "1.7.3", host: "192.168.1.5", port: 7375)
        XCTAssertTrue(connected.isConnected)

        let connectedWithDevice = ConnectionState.connected(serverName: "Studio Mac", serverVersion: "1.7.3", host: "192.168.1.5", port: 7375, deviceName: "My iPad")
        XCTAssertTrue(connectedWithDevice.isConnected)
        if case .connected(let sName, _, _, _, _, let dName) = connectedWithDevice {
            XCTAssertEqual(sName, "Studio Mac")
            XCTAssertEqual(dName, "My iPad")
        }

        let failed = ConnectionState.failed(error: "Connection refused", host: "127.0.0.1", port: 7375)
        XCTAssertFalse(failed.isConnected)
    }
}
