import Foundation
import Combine

public final class CompanionWebSocketClient: @unchecked Sendable {
    public static let defaultPort = 7375

    public let deviceId: String
    public let deviceName: String

    private let session: URLSession
    private let secretStore: PairingSecretStoring
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

    private let lock = NSLock()
    private var webSocketTask: URLSessionWebSocketTask?
    private var generation: Int = 0
    private var pingTask: Task<Void, Never>?
    private var currentHost: String?
    private var currentPort: Int = defaultPort
    /// The Mac's last word before it closes the socket, shown instead of a generic error.
    private var closingServerMessage: String?
    /// The Mac's id from its challenge, in case the following auth_response omits it.
    private var challengeServerId: String?

    public init(
        deviceId: String,
        deviceName: String,
        secretStore: PairingSecretStoring = KeychainPairingSecretStore(),
        session: URLSession? = nil
    ) {
        self.deviceId = deviceId
        self.deviceName = deviceName
        self.secretStore = secretStore
        if let session = session {
            self.session = session
        } else {
            let config = URLSessionConfiguration.default
            config.timeoutIntervalForRequest = 5.0
            config.timeoutIntervalForResource = 0 // Indefinite for WebSocket
            self.session = URLSession(configuration: config)
        }
    }

    deinit {
        disconnect()
    }

    public func connect(host: String, port: Int = defaultPort) {
        disconnect()

        let cleanHost = host.trimmingCharacters(in: .whitespacesAndNewlines)
            .components(separatedBy: "%").first ?? host
        let formattedHost: String
        if cleanHost.contains(":") && !cleanHost.hasPrefix("[") {
            formattedHost = "[\(cleanHost)]"
        } else {
            formattedHost = cleanHost
        }

        guard let url = URL(string: "ws://\(formattedHost):\(port)") else {
            stateSubject.send(.failed(error: "Invalid host address: \(host)", host: host, port: port, wasConnected: false))
            return
        }

        lock.lock()
        currentHost = host
        currentPort = port
        closingServerMessage = nil
        challengeServerId = nil
        generation += 1
        let socketGeneration = generation
        let task = session.webSocketTask(with: url)
        webSocketTask = task
        lock.unlock()

        stateSubject.send(.connecting(host: host, port: port))

        task.resume()

        // Send hello immediately upon initiating connection
        sendHello()
        startPingLoop()
        listenForMessages(task: task, generation: socketGeneration, host: host, port: port)
    }

    public func disconnect() {
        lock.lock()
        stopPingLoopLocked()
        generation += 1
        let task = webSocketTask
        webSocketTask = nil
        currentHost = nil
        lock.unlock()

        task?.cancel(with: .normalClosure, reason: "User disconnected".data(using: .utf8))
        stateSubject.send(.disconnected)
    }

    public func resync() -> Bool {
        guard case .connected = currentConnectionState else { return false }
        return sendMessage(CompanionClientMessage.hello(deviceId: deviceId, deviceName: deviceName))
    }

    public func clearError() {
        if case .failed = currentConnectionState {
            stateSubject.send(.disconnected)
        }
    }

    public func sendHello() {
        _ = sendMessage(CompanionClientMessage.hello(deviceId: deviceId, deviceName: deviceName))
    }

    public func sendAction(_ actionName: String) -> Bool {
        sendMessage(CompanionClientMessage.action(actionName))
    }

    public func getThumbnail(_ clipId: String) -> Bool {
        sendMessage(CompanionClientMessage.getThumbnail(clipId))
    }

    public func revealClip(_ clipId: String) -> Bool {
        sendMessage(CompanionClientMessage.revealClip(clipId))
    }

    public func deleteClip(_ clipId: String) -> Bool {
        sendMessage(CompanionClientMessage.deleteClip(clipId))
    }

    public func trimClip(_ clipId: String) -> Bool {
        sendMessage(CompanionClientMessage.trimClip(clipId))
    }

    public func copyClip(_ clipId: String) -> Bool {
        sendMessage(CompanionClientMessage.copyClip(clipId))
    }

    public func setVolume(target: String, volume: Double) -> Bool {
        sendMessage(CompanionClientMessage.setVolume(target: target, volume: volume))
    }

    public func toggleAppMute(_ bundleId: String) -> Bool {
        sendMessage(CompanionClientMessage.toggleAppMute(bundleId))
    }

    public func unmuteAllApps() -> Bool {
        sendMessage(CompanionClientMessage.unmuteAllApps())
    }

    public func selectDisplay(_ displayId: String) -> Bool {
        sendMessage(CompanionClientMessage.selectDisplay(displayId))
    }

    public func applyProfile(_ profileId: String) -> Bool {
        sendMessage(CompanionClientMessage.applyProfile(profileId))
    }

    public func ping() -> Bool {
        sendMessage(CompanionClientMessage.ping())
    }

    private func sendMessage(_ msg: CompanionClientMessage) -> Bool {
        lock.lock()
        guard let task = webSocketTask else {
            lock.unlock()
            return false
        }
        lock.unlock()

        do {
            let jsonString = try CompanionProtocolParser.encodeClientMessage(msg)
            task.send(.string(jsonString)) { error in
                if let error = error {
                    // Send error logged
                    _ = error
                }
            }
            return true
        } catch {
            return false
        }
    }

    private func listenForMessages(task: URLSessionWebSocketTask, generation: Int, host: String, port: Int) {
        task.receive { [weak self] result in
            guard let self = self else { return }

            self.lock.lock()
            let isCurrent = (self.generation == generation)
            self.lock.unlock()
            guard isCurrent else { return }

            switch result {
            case .success(let message):
                let text: String
                switch message {
                case .string(let s):
                    text = s
                case .data(let d):
                    text = String(decoding: d, as: UTF8.self)
                @unknown default:
                    text = ""
                }

                self.handleIncomingMessage(text, host: host, port: port)

                // Continue listening
                self.listenForMessages(task: task, generation: generation, host: host, port: port)

            case .failure(let error):
                self.handleSocketFailure(error, host: host, port: port)
            }
        }
    }

    private func handleIncomingMessage(_ text: String, host: String, port: Int) {
        let event = CompanionProtocolParser.parse(text)

        switch event {
        case .challenge(let challenge):
            answer(challenge)

        case .auth(let auth):
            handleAuth(auth, host: host, port: port)

        case .serverError(let error) where error.endsConnection:
            lock.lock()
            closingServerMessage = error.message
            lock.unlock()

        default:
            break
        }

        eventsSubject.send(event)
    }

    /// Proves this phone holds the Mac's pairing secret. With no secret the
    /// proof is left out, and the Mac asks the user to approve the phone again.
    private func answer(_ challenge: CompanionAuthChallenge) {
        lock.lock()
        challengeServerId = challenge.serverId
        lock.unlock()

        let proof = secretStore.secret(forServerId: challenge.serverId).map {
            PairingProof.make(secret: $0, serverId: challenge.serverId, deviceId: deviceId, nonce: challenge.nonce)
        }
        _ = sendMessage(CompanionClientMessage.authProof(proof))
    }

    private func handleAuth(_ auth: CompanionAuthResponse, host: String, port: Int) {
        lock.lock()
        let serverId = auth.serverId ?? challengeServerId
        lock.unlock()

        switch auth.status.lowercased() {
        case "approved":
            // Saved before reporting the connection, so the phone can sign in
            // next time even if it's closed the moment it connects.
            if let serverId = serverId,
               let encoded = auth.pairingSecret,
               let secret = Data(base64Encoded: encoded) {
                secretStore.save(secret, forServerId: serverId)
            }
            stateSubject.send(.connected(
                serverName: auth.serverName,
                serverVersion: auth.serverVersion,
                host: host,
                port: port,
                serverId: auth.serverId,
                deviceName: auth.deviceName
            ))
        case "pending":
            stateSubject.send(.pendingApproval(
                serverName: auth.serverName,
                host: host,
                port: port,
                pairingCode: auth.pairingCode
            ))
        case "denied":
            if let serverId = serverId {
                secretStore.remove(forServerId: serverId)
            }
            stateSubject.send(.failed(
                error: Self.denialMessage(reason: auth.reason, serverName: auth.serverName),
                host: host,
                port: port,
                wasConnected: false,
                pairingDenied: true
            ))
        default:
            break
        }
    }

    static func denialMessage(reason: String?, serverName: String) -> String {
        switch reason {
        case "revoked":
            return "“\(serverName)” removed this phone. Connect again to pair."
        case "invalid_proof":
            return "“\(serverName)” couldn't verify this phone. Connect again to pair."
        default:
            return "Pairing denied by \(serverName)"
        }
    }

    private func handleSocketFailure(_ error: Error, host: String, port: Int) {
        lock.lock()
        stopPingLoopLocked()

        // If we are already in a failed state (e.g. pairing was denied by the server
        // and the server subsequently closed the socket), do not clobber it with
        // a generic socket closure error.
        if case .failed = stateSubject.value {
            lock.unlock()
            return
        }

        let wasConnected: Bool
        if case .connected(_, _, let sHost, let sPort, _, _) = stateSubject.value,
           sHost == host, sPort == port {
            wasConnected = true
        } else {
            wasConnected = false
        }
        let serverMessage = closingServerMessage
        lock.unlock()

        let desc = serverMessage ?? ConnectionErrors.describe(error, wasConnected: wasConnected)
        stateSubject.send(.failed(
            error: desc,
            host: host,
            port: port,
            wasConnected: wasConnected,
            pairingDenied: false
        ))
    }

    private func startPingLoop() {
        lock.lock()
        stopPingLoopLocked()
        pingTask = Task { [weak self] in
            while !Task.isCancelled {
                try? await Task.sleep(nanoseconds: 20_000_000_000)
                guard !Task.isCancelled, let self = self else { break }
                _ = self.ping()
            }
        }
        lock.unlock()
    }

    private func stopPingLoopLocked() {
        pingTask?.cancel()
        pingTask = nil
    }
}
