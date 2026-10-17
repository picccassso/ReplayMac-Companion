import Foundation
import Combine

/// A pretend ReplayCap that lives in memory, so the app can be tried without
/// a Mac. It speaks the same events as the socket and never touches the
/// network, the Keychain or the remembered Macs.
public final class DemoCompanionSession: CompanionLink, @unchecked Sendable {
    /// Runs `work` after a delay. Injected so tests can move time by hand.
    public typealias Scheduler = (_ delay: TimeInterval, _ work: @escaping () -> Void) -> Void

    public static let serverName = "Demo Mac"
    static let tickInterval: TimeInterval = 1
    static let saveDuration: TimeInterval = 1.2
    static let statusResetDelay: TimeInterval = 2
    static let thumbnailDelay: TimeInterval = 0.2

    public static let connectedState = ConnectionState.connected(
        serverName: serverName,
        serverVersion: "Demo",
        host: "demo",
        port: 0
    )

    private let now: () -> Date
    private let schedule: Scheduler
    private let renderThumbnail: (CompanionClipSummary) -> String?

    private let lock = NSLock()
    private var state: DemoCompanionState?
    /// Bumped on start and stop, so work scheduled for an earlier run is dropped.
    private var runGeneration = 0
    /// Bumped whenever the save status changes, so a stale reset can't clear a newer status.
    private var saveGeneration = 0

    private let stateSubject = CurrentValueSubject<ConnectionState, Never>(.disconnected)
    public var connectionState: AnyPublisher<ConnectionState, Never> {
        stateSubject.eraseToAnyPublisher()
    }
    public var currentConnectionState: ConnectionState {
        stateSubject.value
    }

    private let eventsSubject = PassthroughSubject<CompanionServerEvent, Never>()
    public var events: AnyPublisher<CompanionServerEvent, Never> {
        eventsSubject.eraseToAnyPublisher()
    }

    /// The fake Mac's state while the demo runs.
    public var currentState: DemoCompanionState? {
        lock.lock()
        defer { lock.unlock() }
        return state
    }

    public init(
        now: @escaping () -> Date = Date.init,
        schedule: @escaping Scheduler = { delay, work in
            DispatchQueue.main.asyncAfter(deadline: .now() + delay, execute: work)
        },
        renderThumbnail: @escaping (CompanionClipSummary) -> String? = DemoThumbnailRenderer.render
    ) {
        self.now = now
        self.schedule = schedule
        self.renderThumbnail = renderThumbnail
    }

    // MARK: - Lifecycle

    public func start() {
        lock.lock()
        guard state == nil else {
            lock.unlock()
            return
        }
        let initial = DemoCompanionState.initial(now: nowSeconds)
        state = initial
        runGeneration += 1
        let generation = runGeneration
        lock.unlock()

        stateSubject.send(Self.connectedState)
        eventsSubject.send(.snapshot(initial.snapshot))
        scheduleTick(generation)
    }

    public func stop() {
        lock.lock()
        state = nil
        runGeneration += 1
        lock.unlock()
        stateSubject.send(.disconnected)
    }

    private var nowSeconds: Double {
        now().timeIntervalSince1970
    }

    // MARK: - CompanionLink

    public func resync() -> Bool {
        guard let current = currentState else { return false }
        eventsSubject.send(.snapshot(current.snapshot))
        return true
    }

    public func sendAction(_ actionName: String) -> Bool {
        let at = nowSeconds
        switch actionName {
        case CompanionActions.saveReplay:
            return save { $0.saveReplay() }
        case CompanionActions.saveExtendedReplay:
            return save { $0.saveExtendedReplay() }
        case CompanionActions.toggleBuffer:
            return update { $0.toggleBuffer() }
        case CompanionActions.toggleSession:
            return update { $0.toggleSession(now: at) }
        case CompanionActions.toggleMicrophone:
            return update { $0.toggleMicrophone() }
        case CompanionActions.toggleSystemAudio:
            return update { $0.toggleSystemAudio() }
        default:
            return false
        }
    }

    public func getThumbnail(_ clipId: String) -> Bool {
        lock.lock()
        let running = state != nil
        let clip = state?.clips.first { $0.id == clipId }
        let generation = runGeneration
        lock.unlock()
        guard running else { return false }

        // Answered later, as the Mac would, and never from inside the request.
        // An unknown clip gets an empty answer, so the request doesn't stay pending.
        schedule(Self.thumbnailDelay) { [weak self] in
            guard let self, self.isCurrentRun(generation) else { return }
            self.eventsSubject.send(.thumbnail(clipId: clipId, base64: clip.flatMap(self.renderThumbnail)))
        }
        return true
    }

    public func revealClip(_ clipId: String) -> Bool {
        update { $0.revealClip(clipId) }
    }

    public func deleteClip(_ clipId: String) -> Bool {
        update { $0.deleteClip(clipId) }
    }

    public func trimClip(_ clipId: String) -> Bool {
        let at = nowSeconds
        return update { $0.trimClip(clipId, now: at) }
    }

    public func copyClip(_ clipId: String) -> Bool {
        update { $0.copyClip(clipId) }
    }

    public func setVolume(target: String, volume: Double) -> Bool {
        update { $0.setVolume(target: target, volume: volume) }
    }

    public func toggleAppMute(_ bundleId: String) -> Bool {
        update { $0.toggleAppMute(bundleId) }
    }

    public func unmuteAllApps() -> Bool {
        update { $0.unmuteAllApps() }
    }

    public func selectDisplay(_ displayId: String) -> Bool {
        update { $0.selectDisplay(displayId) }
    }

    public func applyProfile(_ profileId: String) -> Bool {
        update { $0.applyProfile(profileId) }
    }

    // MARK: - State changes

    /// Applies a change and sends the new state if the phone would see it.
    /// False when the demo isn't running.
    @discardableResult
    private func update(_ change: (inout DemoCompanionState) -> Void) -> Bool {
        lock.lock()
        guard var current = state else {
            lock.unlock()
            return false
        }
        let before = current
        change(&current)
        state = current
        lock.unlock()

        let snapshot = current.snapshot
        if snapshot != before.snapshot {
            eventsSubject.send(.stateChanged(snapshot))
        }
        return true
    }

    /// Starts a save, then plays it out: "saving", the new clip with "saved", back to "idle".
    private func save(_ request: (inout DemoCompanionState) -> Void) -> Bool {
        var status: String?
        let sent = update {
            let before = $0.saveStatus
            request(&$0)
            if $0.saveStatus != before { status = $0.saveStatus }
        }
        guard let status else { return sent }

        let generation = bumpSaveGeneration()
        if status == "saving" {
            scheduleSaveStep(after: Self.saveDuration, generation: generation) { [weak self] in
                guard let self else { return }
                let at = self.nowSeconds
                self.update { $0.finishSave(now: at) }
                self.scheduleStatusReset()
            }
        } else {
            scheduleStatusReset(generation: generation)
        }
        return sent
    }

    private func scheduleStatusReset(generation: Int? = nil) {
        let generation = generation ?? bumpSaveGeneration()
        scheduleSaveStep(after: Self.statusResetDelay, generation: generation) { [weak self] in
            self?.update { $0.resetSaveStatus() }
        }
    }

    private func scheduleSaveStep(after delay: TimeInterval, generation: Int, _ work: @escaping () -> Void) {
        lock.lock()
        let run = runGeneration
        lock.unlock()
        schedule(delay) { [weak self] in
            guard let self, self.isCurrentRun(run), self.isCurrentSave(generation) else { return }
            work()
        }
    }

    private func bumpSaveGeneration() -> Int {
        lock.lock()
        defer { lock.unlock() }
        saveGeneration += 1
        return saveGeneration
    }

    private func isCurrentSave(_ generation: Int) -> Bool {
        lock.lock()
        defer { lock.unlock() }
        return saveGeneration == generation
    }

    private func isCurrentRun(_ generation: Int) -> Bool {
        lock.lock()
        defer { lock.unlock() }
        return runGeneration == generation && state != nil
    }

    private func scheduleTick(_ generation: Int) {
        schedule(Self.tickInterval) { [weak self] in
            guard let self, self.isCurrentRun(generation) else { return }
            self.update { $0.tick(seconds: Self.tickInterval) }
            self.scheduleTick(generation)
        }
    }
}
