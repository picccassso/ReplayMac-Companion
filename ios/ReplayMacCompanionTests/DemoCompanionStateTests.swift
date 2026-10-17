import XCTest
@testable import ReplayCapCompanion

/// The demo Mac's rules, without timers.
final class DemoCompanionStateTests: XCTestCase {
    private let now: Double = 1_760_000_000
    private let utc = TimeZone(identifier: "UTC")!

    private func makeState() -> DemoCompanionState {
        DemoCompanionState.initial(now: now, timeZone: utc)
    }

    // MARK: - Starting point

    func testStartsWithAFullRunningBufferAndSampleData() {
        let snap = makeState().snapshot

        XCTAssertTrue(snap.isRecording)
        XCTAssertFalse(snap.isSessionRecording)
        XCTAssertEqual(snap.saveStatus, "idle")
        XCTAssertEqual(snap.bufferedSeconds, 30)
        XCTAssertEqual(snap.bufferMemoryFormatted, "411 MB")
        XCTAssertTrue(snap.canSaveQuickReplay)
        XCTAssertTrue(snap.canSaveExtendedReplay)
        XCTAssertEqual(snap.capturedDisplayName, "Studio Display")
        XCTAssertEqual(snap.activeProfileName, "Gaming")
        XCTAssertEqual(snap.mutedAudioAppsCount, 1)
        XCTAssertEqual(snap.recentClips.map(\.id), ["demo-clip-1", "demo-clip-2", "demo-clip-3"])
        XCTAssertEqual(snap.recentClips.map(\.kind), ["replay", "session", "extended replay"])
        XCTAssertTrue(snap.recentClips.allSatisfy(\.hasThumbnail))
    }

    func testClipFilenamesUseReplayCapsTimestamp() {
        let clip = makeState().clips[0]
        // 1_760_000_000 - 300 s is 2025-10-09 08:48:20 UTC.
        XCTAssertEqual(clip.filename, "Replay 2025-10-09 at 08.48.20.mp4")
        XCTAssertEqual(clip.savedAt, now - 300)
    }

    // MARK: - Time

    func testTickFillsTheBufferUpToItsLength() {
        var state = makeState()
        state.toggleBuffer()
        state.toggleBuffer()
        state.tick(seconds: 12)
        XCTAssertEqual(state.bufferedSeconds, 12)
        XCTAssertEqual(state.recordingElapsedSeconds, 12)
        XCTAssertEqual(state.snapshot.bufferMemoryFormatted, "164 MB")

        state.tick(seconds: 100)
        XCTAssertEqual(state.bufferedSeconds, 30)
        XCTAssertEqual(state.extendedBufferElapsedSeconds, 112)
        XCTAssertEqual(state.recordingElapsedSeconds, 112)
    }

    func testTickCapsTheExtendedBufferAtItsLength() {
        var state = makeState()
        state.tick(seconds: 60)
        XCTAssertEqual(state.extendedBufferElapsedSeconds, 300)
    }

    func testTickLeavesAStoppedBufferAlone() {
        var state = makeState()
        state.toggleBuffer()
        state.tick(seconds: 10)
        XCTAssertEqual(state.bufferedSeconds, 0)
        XCTAssertEqual(state.recordingElapsedSeconds, 0)
    }

    // MARK: - Buffer and session

    func testStoppingTheBufferEmptiesIt() {
        var state = makeState()
        state.toggleBuffer()

        let snap = state.snapshot
        XCTAssertFalse(snap.isRecording)
        XCTAssertEqual(snap.bufferedSeconds, 0)
        XCTAssertEqual(snap.extendedBufferElapsedSeconds, 0)
        XCTAssertEqual(snap.bufferMemoryBytes, 0)
        XCTAssertEqual(snap.bufferMemoryFormatted, "0 MB")
        XCTAssertFalse(snap.canSaveQuickReplay)
    }

    func testStoppingASessionSavesItAsAClip() {
        var state = makeState()
        state.toggleSession(now: now)
        XCTAssertTrue(state.isSessionRecording)
        state.tick(seconds: 95)
        XCTAssertEqual(state.sessionElapsedSeconds, 95)

        state.toggleSession(now: now + 95)

        XCTAssertFalse(state.isSessionRecording)
        XCTAssertEqual(state.sessionElapsedSeconds, 0)
        let clip = state.clips[0]
        XCTAssertEqual(clip.id, "demo-clip-4")
        XCTAssertEqual(clip.kind, "session")
        XCTAssertEqual(clip.duration, 95)
        XCTAssertEqual(clip.savedAt, now + 95)
        XCTAssertTrue(clip.filename.hasPrefix("Session "))
    }

    func testStoppingASessionAtOnceSavesNothing() {
        var state = makeState()
        state.toggleSession(now: now)
        state.toggleSession(now: now)
        XCTAssertEqual(state.clips.count, 3)
    }

    // MARK: - Saving

    func testSavingAReplayAddsTheBufferAsAClip() {
        var state = makeState()
        state.saveReplay()
        XCTAssertEqual(state.saveStatus, "saving")
        XCTAssertTrue(state.snapshot.isSaveInProgress)

        state.finishSave(now: now + 2)
        XCTAssertEqual(state.saveStatus, "saved")
        XCTAssertEqual(state.clips[0].kind, "replay")
        XCTAssertEqual(state.clips[0].duration, 30)
        XCTAssertEqual(state.clips[0].fileSize, 48_000_000)

        state.resetSaveStatus()
        XCTAssertEqual(state.saveStatus, "idle")
    }

    func testExtendedReplaySavesTheLongBuffer() {
        var state = makeState()
        state.saveExtendedReplay()
        state.finishSave(now: now)
        XCTAssertEqual(state.clips[0].kind, "extended replay")
        XCTAssertEqual(state.clips[0].duration, 300)
    }

    func testSavingWithTheBufferStoppedFails() {
        var state = makeState()
        state.toggleBuffer()
        state.saveReplay()
        XCTAssertEqual(state.saveStatus, "failed")
        XCTAssertFalse(state.isSaving)

        state.finishSave(now: now)
        XCTAssertEqual(state.clips.count, 3, "a failed save writes nothing")
    }

    func testASecondSaveWhileSavingIsIgnored() {
        var state = makeState()
        state.saveReplay()
        state.saveExtendedReplay()
        state.finishSave(now: now)
        state.finishSave(now: now)

        XCTAssertEqual(state.clips.count, 4)
        XCTAssertEqual(state.clips[0].kind, "replay")
    }

    func testResetDoesNotCutASaveShort() {
        var state = makeState()
        state.saveReplay()
        state.resetSaveStatus()
        XCTAssertEqual(state.saveStatus, "saving")
    }

    func testClipListKeepsTheNewestTen() {
        var state = makeState()
        for i in 0..<12 {
            state.saveReplay()
            state.finishSave(now: now + Double(i))
        }
        XCTAssertEqual(state.clips.count, DemoCompanionState.maxClips)
        XCTAssertEqual(state.clips.first?.id, "demo-clip-15")
        XCTAssertFalse(state.clips.contains { $0.id == "demo-clip-1" })
    }

    // MARK: - Audio

    func testMuteTogglesFlipMicrophoneAndSystemAudio() {
        var state = makeState()
        state.toggleMicrophone()
        state.toggleSystemAudio()
        XCTAssertTrue(state.isMicrophoneMuted)
        XCTAssertTrue(state.isSystemAudioMuted)

        state.toggleMicrophone()
        XCTAssertFalse(state.isMicrophoneMuted)
    }

    func testVolumeIsClampedAndUnknownTargetsIgnored() {
        var state = makeState()
        state.setVolume(target: "microphone", volume: 1.7)
        state.setVolume(target: "systemAudio", volume: -0.2)
        state.setVolume(target: "speakers", volume: 0.1)

        XCTAssertEqual(state.microphoneVolume, 1)
        XCTAssertEqual(state.systemAudioVolume, 0)
    }

    func testAppMuteTogglesOneAppAndUnmuteAllClearsThem() {
        var state = makeState()
        state.toggleAppMute("com.apple.Safari")
        XCTAssertEqual(Set(state.audioApps.filter(\.isMuted).map(\.bundleId)), ["com.apple.Safari", "com.hnc.Discord"])

        state.toggleAppMute("com.hnc.Discord")
        XCTAssertEqual(state.audioApps.filter(\.isMuted).map(\.bundleId), ["com.apple.Safari"])

        state.unmuteAllApps()
        XCTAssertEqual(state.snapshot.mutedAudioAppsCount, 0)
    }

    // MARK: - Capture setup

    func testSelectingADisplayMakesItTheOnlyActiveOne() {
        var state = makeState()
        state.selectDisplay("demo-display-builtin")

        XCTAssertEqual(state.displays.filter(\.isActive).map(\.id), ["demo-display-builtin"])
        XCTAssertEqual(state.snapshot.capturedDisplayName, "Built-in Retina Display")
    }

    func testApplyingAProfileMakesItTheOnlyActiveOne() {
        var state = makeState()
        state.applyProfile("demo-profile-battery")
        XCTAssertEqual(state.profiles.filter(\.isActive).map(\.id), ["demo-profile-battery"])
    }

    func testUnknownIdsChangeNothing() {
        let original = makeState()
        var state = original
        state.selectDisplay("nope")
        state.applyProfile("nope")
        state.toggleAppMute("nope")
        state.deleteClip("nope")
        state.trimClip("nope", now: now)
        state.copyClip("nope")
        state.revealClip("nope")
        XCTAssertEqual(state, original)
    }

    // MARK: - Clips

    func testDeleteRemovesTheClip() {
        var state = makeState()
        state.deleteClip("demo-clip-2")
        XCTAssertEqual(state.clips.map(\.id), ["demo-clip-1", "demo-clip-3"])
    }

    func testTrimAddsAShorterCopyOnTop() {
        var state = makeState()
        state.trimClip("demo-clip-2", now: now)

        let trimmed = state.clips[0]
        XCTAssertEqual(state.clips.count, 4)
        XCTAssertEqual(trimmed.id, "demo-clip-4")
        XCTAssertEqual(trimmed.kind, "session")
        XCTAssertEqual(trimmed.duration, 377)
        XCTAssertEqual(trimmed.fileSize, 605_000_000)
        XCTAssertEqual(trimmed.filename, state.clips[2].filename.replacingOccurrences(of: ".mp4", with: " (Trimmed).mp4"))
    }

    func testCopyAndRevealOnlyRecordTheClip() {
        var state = makeState()
        let before = state.snapshot
        state.copyClip("demo-clip-1")
        state.revealClip("demo-clip-3")

        XCTAssertEqual(state.lastCopiedClipId, "demo-clip-1")
        XCTAssertEqual(state.lastRevealedClipId, "demo-clip-3")
        XCTAssertEqual(state.snapshot, before)
    }
}
