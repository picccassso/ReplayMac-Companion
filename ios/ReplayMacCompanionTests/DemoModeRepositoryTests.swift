import XCTest
import Combine
@testable import ReplayCapCompanion

/// Demo mode through the repository the screens use: the demo Mac answers
/// every command, and nothing about a real Mac is read or stored.
final class DemoModeRepositoryTests: XCTestCase {
    private var scheduler: ManualScheduler!
    private var defaults: UserDefaults!
    private var secrets: CountingSecretStore!
    private var repository: CompanionRepository!
    private var connectionState: ConnectionState = .disconnected
    private var cancellables = Set<AnyCancellable>()

    override func setUp() {
        scheduler = ManualScheduler()
        let scheduler = scheduler!
        defaults = UserDefaults(suiteName: "test.demo.\(UUID().uuidString)")!
        secrets = CountingSecretStore()
        let demo = DemoCompanionSession(
            now: { scheduler.now },
            schedule: { delay, work in scheduler.schedule(delay, work) },
            renderThumbnail: { "thumb-\($0.id)" }
        )
        repository = CompanionRepository(defaults: defaults, secretStore: secrets, deviceId: "phone-1", demoSession: demo)
        repository.connectionState
            .sink { [unowned self] in self.connectionState = $0 }
            .store(in: &cancellables)
    }

    override func tearDown() {
        repository.disconnect()
        repository.stopDiscovery()
        cancellables.removeAll()
    }

    func testStartingTheDemoShowsTheDemoMacAsConnected() {
        repository.startDemo()

        XCTAssertTrue(repository.currentIsDemoActive)
        XCTAssertEqual(connectionState, DemoCompanionSession.connectedState)
        XCTAssertEqual(repository.currentSnapshot.recentClips.count, 3)
        XCTAssertTrue(repository.currentSnapshot.isRecording)
    }

    func testCommandsReachTheDemoMac() {
        repository.startDemo()

        repository.saveReplay()
        XCTAssertEqual(repository.currentSnapshot.saveStatus, "saving")

        repository.toggleSystemAudio()
        repository.unmuteAllApps()
        repository.applyProfile("demo-profile-tutorials")
        repository.deleteClip("demo-clip-1")

        let snap = repository.currentSnapshot
        XCTAssertTrue(snap.isSystemAudioMuted)
        XCTAssertEqual(snap.mutedAudioAppsCount, 0)
        XCTAssertEqual(snap.activeProfileName, "Tutorials")
        XCTAssertFalse(snap.recentClips.contains { $0.id == "demo-clip-1" })
    }

    func testClipThumbnailsAreFetchedLikeAMacs() {
        repository.startDemo()
        scheduler.advance(by: 1)

        XCTAssertEqual(repository.currentThumbnails["demo-clip-2"], "thumb-demo-clip-2")
        XCTAssertEqual(repository.currentThumbnails.count, 3)

        repository.deleteClip("demo-clip-2")
        XCTAssertNil(repository.currentThumbnails["demo-clip-2"], "a deleted clip's thumbnail is dropped")
    }

    func testDisconnectLeavesTheDemoForTheConnectScreen() {
        repository.startDemo()
        repository.disconnect()

        XCTAssertFalse(repository.currentIsDemoActive)
        XCTAssertEqual(connectionState, .disconnected)
        XCTAssertEqual(repository.currentSnapshot, .empty)
        XCTAssertTrue(repository.currentThumbnails.isEmpty)
    }

    func testTheDemoNeverStoresOrReadsAnythingAboutAMac() {
        repository.startDemo()
        repository.saveReplay()
        scheduler.advance(by: 5)
        repository.onAppBackgrounded()
        repository.onAppForegrounded()
        repository.disconnect()

        XCTAssertEqual(secrets.calls, 0, "no Keychain access")
        XCTAssertTrue(repository.currentPreviouslyConnectedMacs.isEmpty)
        XCTAssertTrue(RememberedMacStore(defaults: defaults).loadAll().isEmpty)
        XCTAssertNil(repository.getLastConnectedHost())
        XCTAssertNil(repository.reconnector.currentTarget)
    }

    func testForegroundingDuringTheDemoKeepsIt() {
        repository.startDemo()
        repository.onAppForegrounded()

        XCTAssertTrue(repository.currentIsDemoActive)
        XCTAssertEqual(connectionState, DemoCompanionSession.connectedState)
    }
}

/// Counts every Keychain call, to prove the demo makes none.
final class CountingSecretStore: PairingSecretStoring, @unchecked Sendable {
    private let lock = NSLock()
    private var count = 0

    var calls: Int {
        lock.lock(); defer { lock.unlock() }
        return count
    }

    private func record() {
        lock.lock(); count += 1; lock.unlock()
    }

    func secret(forServerId serverId: String) -> Data? {
        record()
        return nil
    }

    func save(_ secret: Data, forServerId serverId: String) {
        record()
    }

    func remove(forServerId serverId: String) {
        record()
    }
}
