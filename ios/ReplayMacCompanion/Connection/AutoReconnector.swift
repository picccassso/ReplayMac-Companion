import Foundation
import Combine

/// Finds the remembered Mac again and connects to it. Tries every address it
/// might be at, and jumps straight to a new one as soon as discovery sees the
/// Mac somewhere else, e.g. after the phone moves to another network.
public final class AutoReconnector: @unchecked Sendable {
    public static let attemptTimeoutMs: Int64 = 8_000

    fileprivate enum Outcome: Sendable {
        case settled
        case denied
        case failed
        case foundElsewhere(MacEndpoint)
    }

    private let connectionStatePublisher: AnyPublisher<ConnectionState, Never>
    private let discoveredMacsPublisher: AnyPublisher<[DiscoveredMac], Never>
    private let getCurrentDiscoveredMacs: @Sendable () -> [DiscoveredMac]
    private let connectAction: @Sendable (MacEndpoint) -> Void
    private let onPairingDeniedAction: @Sendable () -> Void
    private let attemptTimeoutSeconds: TimeInterval

    private let targetSubject = CurrentValueSubject<RememberedMac?, Never>(nil)
    public var target: AnyPublisher<RememberedMac?, Never> {
        targetSubject.eraseToAnyPublisher()
    }
    public var currentTarget: RememberedMac? {
        targetSubject.value
    }

    private let lock = NSLock()
    private var task: Task<Void, Never>?

    public init(
        connectionState: AnyPublisher<ConnectionState, Never>,
        discoveredMacs: AnyPublisher<[DiscoveredMac], Never>,
        getCurrentDiscoveredMacs: @escaping @Sendable () -> [DiscoveredMac],
        connect: @escaping @Sendable (MacEndpoint) -> Void,
        onPairingDenied: @escaping @Sendable () -> Void,
        attemptTimeoutMs: Int64 = attemptTimeoutMs
    ) {
        self.connectionStatePublisher = connectionState
        self.discoveredMacsPublisher = discoveredMacs
        self.getCurrentDiscoveredMacs = getCurrentDiscoveredMacs
        self.connectAction = connect
        self.onPairingDeniedAction = onPairingDenied
        self.attemptTimeoutSeconds = Double(attemptTimeoutMs) / 1000.0
    }

    deinit {
        stop()
    }

    public func start(_ mac: RememberedMac) {
        lock.lock()
        if task != nil {
            lock.unlock()
            return
        }
        targetSubject.send(mac)
        task = Task { [weak self, mac] in
            guard let self = self else { return }
            await self.run(mac: mac)
            self.finishRun(for: mac)
        }
        lock.unlock()
    }

    public func stop() {
        lock.lock()
        task?.cancel()
        task = nil
        targetSubject.send(nil)
        lock.unlock()
    }

    private func finishRun(for mac: RememberedMac) {
        lock.lock()
        if targetSubject.value == mac {
            targetSubject.send(nil)
        }
        task = nil
        lock.unlock()
    }

    private func run(mac: RememberedMac) async {
        var round = 0
        while !Task.isCancelled {
            var tried = Set<MacEndpoint>()
            var next = untried(mac: mac, tried: tried)

            while let current = next {
                guard !Task.isCancelled else { return }
                tried.insert(current)
                connectAction(current)

                let outcome = await awaitOutcome(mac: mac, endpoint: current, tried: tried)
                switch outcome {
                case .settled:
                    return
                case .denied:
                    onPairingDeniedAction()
                    return
                case .foundElsewhere(let endpoint):
                    next = endpoint
                case .failed:
                    next = untried(mac: mac, tried: tried)
                }
            }

            guard !Task.isCancelled else { return }
            let backoff = ReconnectPlan.backoffMillis(round: round)
            round += 1
            await waitBackoffOrDiscovered(mac: mac, tried: tried, backoffMs: backoff)
        }
    }

    private func untried(
        mac: RememberedMac,
        tried: Set<MacEndpoint>,
        discovered: [DiscoveredMac]? = nil
    ) -> MacEndpoint? {
        let disc = discovered ?? getCurrentDiscoveredMacs()
        return ReconnectPlan.candidates(remembered: mac, discovered: disc).first { !tried.contains($0) }
    }

    private func awaitOutcome(mac: RememberedMac, endpoint: MacEndpoint, tried: Set<MacEndpoint>) async -> Outcome {
        let relay = CancellationRelay()
        return await withTaskCancellationHandler {
            await withCheckedContinuation { (continuation: CheckedContinuation<Outcome, Never>) in
                let waiter = OutcomeWaiter(continuation: continuation)
                relay.setHandler {
                    waiter.resolve(with: .failed)
                }

                // Monitor connection state
                connectionStatePublisher
                    .sink { state in
                        switch state {
                        case .connected, .pendingApproval:
                            waiter.resolve(with: .settled)
                        case .failed(_, _, _, _, let denied):
                            waiter.resolve(with: denied ? .denied : .failed)
                        case .connecting, .disconnected, .reconnecting:
                            break
                        }
                    }
                    .store(in: waiter)

                // Monitor discovery for the Mac appearing at a new address
                discoveredMacsPublisher
                    .sink { discovered in
                        let newAddress = discovered
                            .filter { ReconnectPlan.isSameMac(discovered: $0, remembered: mac) }
                            .map { MacEndpoint(host: $0.host, port: $0.port) }
                            .first { $0 != endpoint && !tried.contains($0) }

                        if let found = newAddress {
                            waiter.resolve(with: .foundElsewhere(found))
                        }
                    }
                    .store(in: waiter)

                // Timeout timer
                DispatchQueue.global().asyncAfter(deadline: .now() + attemptTimeoutSeconds) {
                    waiter.resolve(with: .failed)
                }
            }
        } onCancel: {
            relay.cancel()
        }
    }

    private func waitBackoffOrDiscovered(mac: RememberedMac, tried: Set<MacEndpoint>, backoffMs: Int64) async {
        let timeoutSeconds = Double(backoffMs) / 1000.0
        let relay = CancellationRelay()
        await withTaskCancellationHandler {
            await withCheckedContinuation { (continuation: CheckedContinuation<Void, Never>) in
                let waiter = DiscoveredWaiter(continuation: continuation)
                relay.setHandler {
                    waiter.resolve()
                }

                discoveredMacsPublisher
                    .sink { [weak self] discovered in
                        guard let self = self else { return }
                        if self.untried(mac: mac, tried: tried, discovered: discovered) != nil {
                            waiter.resolve()
                        }
                    }
                    .store(in: waiter)

                DispatchQueue.global().asyncAfter(deadline: .now() + timeoutSeconds) {
                    waiter.resolve()
                }
            }
        } onCancel: {
            relay.cancel()
        }
    }
}

// MARK: - Thread-Safe Continuation Resolvers

private final class OutcomeWaiter: @unchecked Sendable {
    private var isResolved = false
    private let continuation: CheckedContinuation<AutoReconnector.Outcome, Never>
    private var cancellables = Set<AnyCancellable>()
    private let lock = NSLock()

    init(continuation: CheckedContinuation<AutoReconnector.Outcome, Never>) {
        self.continuation = continuation
    }

    func resolve(with outcome: AutoReconnector.Outcome) {
        lock.lock()
        guard !isResolved else {
            lock.unlock()
            return
        }
        isResolved = true
        cancellables.removeAll()
        lock.unlock()
        continuation.resume(returning: outcome)
    }

    func store(_ cancellable: AnyCancellable) {
        lock.lock()
        if isResolved {
            cancellable.cancel()
        } else {
            cancellables.insert(cancellable)
        }
        lock.unlock()
    }
}

private final class DiscoveredWaiter: @unchecked Sendable {
    private var isResolved = false
    private let continuation: CheckedContinuation<Void, Never>
    private var cancellables = Set<AnyCancellable>()
    private let lock = NSLock()

    init(continuation: CheckedContinuation<Void, Never>) {
        self.continuation = continuation
    }

    func resolve() {
        lock.lock()
        guard !isResolved else {
            lock.unlock()
            return
        }
        isResolved = true
        cancellables.removeAll()
        lock.unlock()
        continuation.resume()
    }

    func store(_ cancellable: AnyCancellable) {
        lock.lock()
        if isResolved {
            cancellable.cancel()
        } else {
            cancellables.insert(cancellable)
        }
        lock.unlock()
    }
}

private extension AnyCancellable {
    func store(in waiter: OutcomeWaiter) {
        waiter.store(self)
    }

    func store(in waiter: DiscoveredWaiter) {
        waiter.store(self)
    }
}

private final class CancellationRelay: @unchecked Sendable {
    private var onCancelHandler: (@Sendable () -> Void)?
    private let lock = NSLock()
    private var isCancelled = false

    func setHandler(_ handler: @escaping @Sendable () -> Void) {
        lock.lock()
        if isCancelled {
            lock.unlock()
            handler()
        } else {
            onCancelHandler = handler
            lock.unlock()
        }
    }

    func cancel() {
        lock.lock()
        isCancelled = true
        let handler = onCancelHandler
        onCancelHandler = nil
        lock.unlock()
        handler?()
    }
}
