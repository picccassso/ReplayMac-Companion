import XCTest
import Combine
@testable import ReplayCapCompanion

/// Runs scheduled work only when the test moves time forward.
final class ManualScheduler {
    private(set) var now = Date(timeIntervalSince1970: 1_760_000_000)
    private var pending: [(due: Date, order: Int, work: () -> Void)] = []
    private var order = 0

    func schedule(_ delay: TimeInterval, _ work: @escaping () -> Void) {
        order += 1
        pending.append((now.addingTimeInterval(delay), order, work))
    }

    func advance(by seconds: TimeInterval) {
        let end = now.addingTimeInterval(seconds)
        while let next = pending.filter({ $0.due <= end }).min(by: { ($0.due, $0.order) < ($1.due, $1.order) }) {
            pending.removeAll { $0.order == next.order }
            now = next.due
            next.work()
        }
        now = end
    }
}

/// The demo Mac as the repository sees it: events, timing and thumbnails.
final class DemoCompanionSessionTests: XCTestCase {
    private var scheduler: ManualScheduler!
    private var session: DemoCompanionSession!
    private var events: [CompanionServerEvent] = []
    private var cancellables = Set<AnyCancellable>()

    override func setUp() {
        scheduler = ManualScheduler()
        let scheduler = scheduler!
        session = DemoCompanionSession(
            now: { scheduler.now },
            schedule: { delay, work in scheduler.schedule(delay, work) },
            renderThumbnail: { "thumb-\($0.id)" }
        )
        events = []
        session.events.sink { [unowned self] in self.events.append($0) }.store(in: &cancellables)
    }

    override func tearDown() {
        cancellables.removeAll()
    }

    private var lastSnapshot: CompanionStateSnapshot? {
        for event in events.reversed() {
            switch event {
            case .snapshot(let snap), .stateChanged(let snap):
                return snap
            default:
                continue
            }
        }
        return nil
    }

    func testStartConnectsAndSendsTheFirstSnapshot() {
        session.start()

        XCTAssertEqual(session.currentConnectionState, DemoCompanionSession.connectedState)
        guard case .snapshot(let snap) = events.first else {
            return XCTFail("expected a snapshot first, got \(events)")
        }
        XCTAssertEqual(snap.recentClips.count, 3)
    }

    func testCommandsDoNothingBeforeTheDemoStarts() {
        XCTAssertFalse(session.sendAction(CompanionActions.toggleBuffer))
        XCTAssertFalse(session.getThumbnail("demo-clip-1"))
        XCTAssertTrue(events.isEmpty)
    }

    func testCommandsChangeTheStateAndSendIt() {
        session.start()

        XCTAssertTrue(session.sendAction(CompanionActions.toggleMicrophone))
        XCTAssertTrue(session.toggleAppMute("com.apple.Music"))
        XCTAssertTrue(session.selectDisplay("demo-display-builtin"))
        XCTAssertTrue(session.deleteClip("demo-clip-3"))

        let snap = lastSnapshot
        XCTAssertEqual(snap?.isMicrophoneMuted, true)
        XCTAssertEqual(snap?.audioApps.first { $0.bundleId == "com.apple.Music" }?.isMuted, true)
        XCTAssertEqual(snap?.capturedDisplayName, "Built-in Retina Display")
        XCTAssertEqual(snap?.recentClips.map(\.id), ["demo-clip-1", "demo-clip-2"])
    }

    func testTheClockTicksWhileRunning() {
        session.start()
        _ = session.sendAction(CompanionActions.toggleSession)

        scheduler.advance(by: 5)

        XCTAssertEqual(lastSnapshot?.sessionElapsedSeconds, 5)
        XCTAssertEqual(lastSnapshot?.recordingElapsedSeconds, 759)
    }

    func testSavePlaysOutSavingThenSavedThenIdle() {
        session.start()
        _ = session.sendAction(CompanionActions.saveReplay)
        XCTAssertEqual(lastSnapshot?.saveStatus, "saving")

        scheduler.advance(by: 1.2)
        XCTAssertEqual(lastSnapshot?.saveStatus, "saved")
        XCTAssertEqual(lastSnapshot?.recentClips.first?.id, "demo-clip-4")
        XCTAssertEqual(lastSnapshot?.recentClips.first?.savedAt ?? 0, 1_760_000_001.2, accuracy: 0.001)

        scheduler.advance(by: 2)
        XCTAssertEqual(lastSnapshot?.saveStatus, "idle")
    }

    func testFailedSaveClearsAfterItHasShown() {
        session.start()
        _ = session.sendAction(CompanionActions.toggleBuffer)
        _ = session.sendAction(CompanionActions.saveReplay)
        XCTAssertEqual(lastSnapshot?.saveStatus, "failed")

        scheduler.advance(by: 2)
        XCTAssertEqual(lastSnapshot?.saveStatus, "idle")
    }

    func testAnOldResetDoesNotClearANewerSave() {
        session.start()
        _ = session.sendAction(CompanionActions.saveReplay)
        scheduler.advance(by: 1.2)                       // saved; reset due at 3.2
        scheduler.advance(by: 1)
        _ = session.sendAction(CompanionActions.saveReplay) // saving again at 2.2

        scheduler.advance(by: 1.1)                        // past the first reset
        XCTAssertEqual(lastSnapshot?.saveStatus, "saving")
        scheduler.advance(by: 0.2)
        XCTAssertEqual(lastSnapshot?.saveStatus, "saved")
        XCTAssertEqual(lastSnapshot?.recentClips.prefix(2).map(\.id), ["demo-clip-5", "demo-clip-4"])
    }

    func testThumbnailsArriveLaterFromTheRenderer() {
        session.start()
        XCTAssertTrue(session.getThumbnail("demo-clip-2"))
        XCTAssertFalse(events.contains { if case .thumbnail = $0 { return true } else { return false } })

        scheduler.advance(by: 1)

        XCTAssertTrue(events.contains(.thumbnail(clipId: "demo-clip-2", base64: "thumb-demo-clip-2")))
    }

    func testAnUnknownClipGetsAnEmptyThumbnail() {
        session.start()
        XCTAssertTrue(session.getThumbnail("not-a-clip"))
        scheduler.advance(by: 1)
        XCTAssertTrue(events.contains(.thumbnail(clipId: "not-a-clip", base64: nil)))
    }

    func testCopyAndRevealSendNothingThePhoneWouldSee() {
        session.start()
        events.removeAll()

        XCTAssertTrue(session.copyClip("demo-clip-1"))
        XCTAssertTrue(session.revealClip("demo-clip-1"))

        XCTAssertTrue(events.isEmpty)
        XCTAssertEqual(session.currentState?.lastCopiedClipId, "demo-clip-1")
    }

    func testStopDisconnectsAndDropsPendingWork() {
        session.start()
        _ = session.sendAction(CompanionActions.saveReplay)
        session.stop()
        events.removeAll()

        scheduler.advance(by: 10)

        XCTAssertEqual(session.currentConnectionState, .disconnected)
        XCTAssertNil(session.currentState)
        XCTAssertTrue(events.isEmpty, "nothing from the stopped demo should arrive")
    }

    func testRestartingBeginsFromTheSampleDataAgain() {
        session.start()
        _ = session.deleteClip("demo-clip-1")
        session.stop()
        session.start()

        XCTAssertEqual(session.currentState?.clips.count, 3)
    }
}
