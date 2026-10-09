import XCTest
@testable import ReplayCapCompanion

final class HomeControlLabelsTests: XCTestCase {

    private let idle = CompanionStateSnapshot.empty
    private let buffering = CompanionStateSnapshot(
        isRecording: true,
        saveStatus: "idle",
        recordingElapsedSeconds: 12.0,
        bufferedSeconds: 12.0
    )

    private func tile(for control: HomeControl, in snapshot: CompanionStateSnapshot) -> ControlTileSpec {
        HomeControlLabels.tiles(snapshot).first { $0.control == control }!
    }

    func testGridListsAllSixActionsInDesktopOrder() {
        let expected: [HomeControl] = [
            .saveReplay,
            .saveExtendedReplay,
            .replayBuffer,
            .session,
            .microphone,
            .systemAudio
        ]
        let actual = HomeControlLabels.tiles(idle).map { $0.control }
        XCTAssertEqual(actual, expected)
    }

    func testSaveTileIsOnlyTappableOnceTheBufferHasFootage() {
        let idleTile = tile(for: .saveReplay, in: idle)
        XCTAssertFalse(idleTile.isEnabled)
        XCTAssertEqual(idleTile.subtitle, "Buffer stopped")

        let justStarted = CompanionStateSnapshot(isRecording: true, bufferedSeconds: 0.4)
        let bufferingTile = tile(for: .saveReplay, in: justStarted)
        XCTAssertFalse(bufferingTile.isEnabled)
        XCTAssertEqual(bufferingTile.subtitle, "Buffering…")

        let readyTile = tile(for: .saveReplay, in: buffering)
        XCTAssertTrue(readyTile.isEnabled)
        XCTAssertEqual(readyTile.subtitle, "Ready")
        XCTAssertEqual(readyTile.title, "Save Last 30s")
    }

    func testSavingShowsASpinnerAndBlocksSessionToggle() {
        let saving = CompanionStateSnapshot(
            isRecording: true,
            isSessionRecording: false,
            saveStatus: "saving",
            bufferedSeconds: 12.0
        )
        let saveTile = tile(for: .saveReplay, in: saving)
        XCTAssertTrue(saveTile.isLoading)
        XCTAssertFalse(saveTile.isEnabled)
        XCTAssertEqual(saveTile.subtitle, "Saving…")

        let sessionTile = tile(for: .session, in: saving)
        XCTAssertFalse(sessionTile.isEnabled)
    }

    func testExtendedReplayExplainsWhyItIsUnavailable() {
        let off = CompanionStateSnapshot(
            isRecording: true,
            longBufferEnabled: false
        )
        let offTile = tile(for: .saveExtendedReplay, in: off)
        XCTAssertFalse(offTile.isEnabled)
        XCTAssertEqual(offTile.subtitle, "Turned off")

        let on = CompanionStateSnapshot(
            isRecording: true,
            longBufferEnabled: true
        )
        let onTile = tile(for: .saveExtendedReplay, in: on)
        XCTAssertTrue(onTile.isEnabled)
    }

    func testExtendedReplayTitleUsesMinutesOnlyForWholeMinutes() {
        XCTAssertEqual(HomeControlLabels.extendedReplayTitle(300), "Save Last 5m")
        XCTAssertEqual(HomeControlLabels.extendedReplayTitle(90), "Save Last 90s")
        XCTAssertEqual(HomeControlLabels.extendedReplayTitle(45), "Save Last 45s")
    }

    func testCompactTitlesAreProvidedForLongLabels() {
        XCTAssertEqual(HomeControlLabels.extendedReplayCompactTitle(300), "Save 5m")
        XCTAssertEqual(HomeControlLabels.extendedReplayCompactTitle(90), "Save 90s")
        XCTAssertEqual(HomeControlLabels.extendedReplayCompactTitle(45), "Save 45s")

        let readyTile = tile(for: .saveReplay, in: buffering)
        XCTAssertEqual(readyTile.compactTitle, "Save 30s")

        let idleSession = tile(for: .session, in: idle)
        XCTAssertEqual(idleSession.compactTitle, "Record Session")

        let recording = CompanionStateSnapshot(
            isRecording: false,
            isSessionRecording: true,
            sessionElapsedSeconds: 75.0
        )
        let recSession = tile(for: .session, in: recording)
        XCTAssertEqual(recSession.compactTitle, "Stop Session")
    }

    func testSessionTileTurnsLiveAndShowsElapsedTimeWhileRecording() {
        let recording = CompanionStateSnapshot(
            isRecording: false,
            isSessionRecording: true,
            sessionElapsedSeconds: 75.0
        )
        let sessionTile = tile(for: .session, in: recording)
        XCTAssertEqual(sessionTile.tone, .live)
        XCTAssertEqual(sessionTile.title, "Stop & Save Session")
        XCTAssertEqual(sessionTile.subtitle, "01:15")
    }

    func testSwitchedOffAudioSourceStaysTappableBecauseTheMacEnablesIt() {
        let micOff = CompanionStateSnapshot(
            isMicrophoneEnabled: false
        )
        let micTile = tile(for: .microphone, in: micOff)
        XCTAssertTrue(micTile.isEnabled)
        XCTAssertEqual(micTile.tone, .off)
        XCTAssertEqual(micTile.subtitle, "Off")

        let audioMuted = CompanionStateSnapshot(
            isSystemAudioEnabled: true,
            isSystemAudioMuted: true
        )
        let audioTile = tile(for: .systemAudio, in: audioMuted)
        XCTAssertEqual(audioTile.tone, .muted)
        XCTAssertEqual(audioTile.subtitle, "Muted")
    }

    func testStatusCardDescribesTheCurrentCapture() {
        XCTAssertEqual(HomeControlLabels.headline(idle), "Not recording")
        XCTAssertTrue(HomeControlLabels.subheadline(idle).contains("last 30 seconds"))

        let running = CompanionStateSnapshot(
            isRecording: true,
            recordingElapsedSeconds: 65.0,
            capturedDisplayName: "Studio Display"
        )
        XCTAssertEqual(HomeControlLabels.headline(running), "Replay buffer running")
        XCTAssertEqual(HomeControlLabels.subheadline(running), "01:05 · Studio Display")

        let both = CompanionStateSnapshot(
            isRecording: true,
            isSessionRecording: true,
            sessionElapsedSeconds: 5.0,
            capturedDisplayName: "Studio Display"
        )
        XCTAssertEqual(HomeControlLabels.headline(both), "Session recording")
        XCTAssertEqual(HomeControlLabels.subheadline(both), "00:05 · Studio Display")
    }

    func testStatusCardFlagsAFallbackDisplayLikeTheDesktop() {
        let fallback = CompanionStateSnapshot(
            isRecording: true,
            recordingElapsedSeconds: 11.0,
            capturedDisplayName: "Built-in Retina Display",
            isUsingFallbackDisplay: true
        )
        XCTAssertEqual(HomeControlLabels.subheadline(fallback), "00:11 · Built-in Retina Display (Fallback)")
    }

    func testMeterReportsReadyOnlyWhenTheWindowIsFull() {
        XCTAssertEqual(meterFillSuffix(availableSeconds: 29.9, capSeconds: 30), "Filling…")
        XCTAssertEqual(meterFillSuffix(availableSeconds: 30.0, capSeconds: 30), "Ready")
    }

    func testClipDetailLineSkipsMissingMetadata() {
        let bare = CompanionClipSummary(id: "1", filename: "clip.mp4", kind: "session")
        XCTAssertEqual(clipDetailLine(bare), "Session")

        let full = CompanionClipSummary(
            id: "2",
            filename: "clip.mov",
            kind: "replay",
            duration: 30.0,
            fileSize: 5 * 1024 * 1024
        )
        XCTAssertEqual(clipDetailLine(full), "Replay · 00:30 · 5.0 MB")
    }
}
