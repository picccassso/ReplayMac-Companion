import XCTest
import Combine
import Network
@testable import ReplayCapCompanion

/// Signs the real client in against a scripted stand-in for ReplayCap.
final class CompanionAuthFlowTests: XCTestCase {
    private var cancellables = Set<AnyCancellable>()
    private var server: FakeMacServer!
    private var store: InMemorySecretStore!

    override func setUpWithError() throws {
        store = InMemorySecretStore()
    }

    override func tearDown() {
        server?.stop()
        cancellables.removeAll()
    }

    private func makeClient() -> CompanionWebSocketClient {
        CompanionWebSocketClient(deviceId: "phone-1", deviceName: "Test Phone", secretStore: store)
    }

    /// Connects and waits for the first state matching `predicate`.
    private func connect(_ client: CompanionWebSocketClient, until predicate: @escaping (ConnectionState) -> Bool) -> ConnectionState? {
        let exp = expectation(description: "state reached")
        var reached: ConnectionState?
        client.connectionState
            .sink { state in
                if reached == nil, predicate(state) {
                    reached = state
                    exp.fulfill()
                }
            }
            .store(in: &cancellables)
        client.connect(host: "127.0.0.1", port: server.port)
        wait(for: [exp], timeout: 5)
        return reached
    }

    func testProofMatchesCrossPlatformVector() {
        // Same vector as the Mac and Android tests, computed independently with openssl.
        let proof = PairingProof.make(secret: Data(repeating: 1, count: 32), serverId: "S", deviceId: "D", nonce: "N")
        XCTAssertEqual(proof, "uWE2MkK60y47fmnf7H64CPSeu+v47Xrw4QeYLwmpKcs=")
    }

    func testPairedPhoneAnswersChallengeWithProof() throws {
        let secret = Data(repeating: 9, count: 32)
        store.save(secret, forServerId: "mac-1")
        var receivedProof: String?
        server = try FakeMacServer { message in
            switch message["type"] as? String {
            case "hello":
                return [.send(#"{"type":"auth_challenge","nonce":"abc","serverId":"mac-1","serverName":"Studio","serverVersion":"1.8"}"#)]
            case "auth_proof":
                receivedProof = message["proof"] as? String
                return [.send(#"{"type":"auth_response","status":"approved","serverName":"Studio","serverVersion":"1.8","serverId":"mac-1"}"#)]
            default:
                return []
            }
        }

        let client = makeClient()
        defer { client.disconnect() }
        let state = connect(client) { $0.isConnected }
        XCTAssertNotNil(state)
        XCTAssertEqual(receivedProof, PairingProof.make(secret: secret, serverId: "mac-1", deviceId: "phone-1", nonce: "abc"))
        XCTAssertEqual(store.secret(forServerId: "mac-1"), secret, "Signing in keeps the existing secret")
    }

    func testPhoneWithoutSecretLeavesProofOutAndShowsCode() throws {
        var proofWasSent = true
        server = try FakeMacServer { message in
            switch message["type"] as? String {
            case "hello":
                return [.send(#"{"type":"auth_challenge","nonce":"abc","serverId":"mac-1","serverName":"Studio","serverVersion":"1.8"}"#)]
            case "auth_proof":
                proofWasSent = message["proof"] is String
                return [.send(#"{"type":"auth_response","status":"pending","serverName":"Studio","serverVersion":"1.8","pairingCode":"4821"}"#)]
            default:
                return []
            }
        }

        let client = makeClient()
        defer { client.disconnect() }
        let state = connect(client) { $0.isPendingApproval }
        XCTAssertEqual(state, .pendingApproval(serverName: "Studio", host: "127.0.0.1", port: server.port, pairingCode: "4821"))
        XCTAssertFalse(proofWasSent)
    }

    func testApprovalSavesSecretBeforeReportingConnected() throws {
        let secret = Data(repeating: 4, count: 32)
        server = try FakeMacServer { message in
            guard message["type"] as? String == "hello" else { return [] }
            return [
                .send(#"{"type":"auth_response","status":"pending","serverName":"Studio","serverVersion":"1.8","pairingCode":"1234"}"#),
                .send(#"{"type":"auth_response","status":"approved","serverName":"Studio","serverVersion":"1.8","serverId":"mac-1","pairingSecret":"\#(secret.base64EncodedString())"}"#)
            ]
        }

        let client = makeClient()
        defer { client.disconnect() }
        var secretWhenConnected: Data?
        let state = connect(client) { [store] state in
            if state.isConnected {
                secretWhenConnected = store?.secret(forServerId: "mac-1")
            }
            return state.isConnected
        }
        XCTAssertNotNil(state)
        XCTAssertEqual(secretWhenConnected, secret)
    }

    func testDenialForgetsTheSecret() throws {
        store.save(Data(repeating: 9, count: 32), forServerId: "mac-1")
        server = try FakeMacServer { message in
            switch message["type"] as? String {
            case "hello":
                return [.send(#"{"type":"auth_challenge","nonce":"abc","serverId":"mac-1","serverName":"Studio","serverVersion":"1.8"}"#)]
            case "auth_proof":
                return [.send(#"{"type":"auth_response","status":"denied","serverName":"Studio","serverVersion":"1.8","serverId":"mac-1","reason":"invalid_proof"}"#), .close]
            default:
                return []
            }
        }

        let client = makeClient()
        defer { client.disconnect() }
        guard case .failed(let error, _, _, _, let pairingDenied) = connect(client, until: { if case .failed = $0 { return true }; return false }) else {
            return XCTFail("Expected a failed state")
        }
        XCTAssertTrue(pairingDenied)
        XCTAssertEqual(error, "“Studio” couldn't verify this phone. Connect again to pair.")
        XCTAssertNil(store.secret(forServerId: "mac-1"))
    }

    func testMacsReasonIsShownWhenItClosesTheConnection() throws {
        server = try FakeMacServer { message in
            guard message["type"] as? String == "hello" else { return [] }
            return [.send(#"{"type":"error","message":"Another device is waiting for approval on the Mac.","code":"busy"}"#), .close]
        }

        let client = makeClient()
        defer { client.disconnect() }
        guard case .failed(let error, _, _, _, let pairingDenied) = connect(client, until: { if case .failed = $0 { return true }; return false }) else {
            return XCTFail("Expected a failed state")
        }
        XCTAssertEqual(error, "Another device is waiting for approval on the Mac.")
        XCTAssertFalse(pairingDenied, "Being turned away is not a denial; the phone keeps the Mac")
    }

    func testDeviceIdentityIsStable() {
        let keychain = KeychainStore(service: "com.replaycap.companion.tests.\(UUID().uuidString)")
        defer { keychain.remove(DeviceIdentity.account) }
        let first = DeviceIdentity.loadOrCreate(keychain: keychain)
        XCTAssertFalse(first.isEmpty)
        XCTAssertEqual(DeviceIdentity.loadOrCreate(keychain: keychain), first)
    }
}

final class InMemorySecretStore: PairingSecretStoring, @unchecked Sendable {
    private let lock = NSLock()
    private var secrets: [String: Data] = [:]

    func secret(forServerId serverId: String) -> Data? {
        lock.lock(); defer { lock.unlock() }
        return secrets[serverId]
    }

    func save(_ secret: Data, forServerId serverId: String) {
        lock.lock(); defer { lock.unlock() }
        secrets[serverId] = secret
    }

    func remove(forServerId serverId: String) {
        lock.lock(); defer { lock.unlock() }
        secrets.removeValue(forKey: serverId)
    }
}

/// A loopback WebSocket server that answers each client message from a script.
final class FakeMacServer: @unchecked Sendable {
    enum Reply {
        case send(String)
        case close
    }

    private(set) var port = 0
    private let listener: NWListener
    private let queue = DispatchQueue(label: "FakeMacServer")
    private let script: ([String: Any]) -> [Reply]

    init(script: @escaping ([String: Any]) -> [Reply]) throws {
        self.script = script
        let parameters = NWParameters.tcp
        parameters.defaultProtocolStack.applicationProtocols.insert(NWProtocolWebSocket.Options(), at: 0)
        listener = try NWListener(using: parameters, on: .any)

        let ready = DispatchSemaphore(value: 0)
        listener.stateUpdateHandler = { state in
            if case .ready = state { ready.signal() }
        }
        listener.newConnectionHandler = { [weak self] connection in
            self?.accept(connection)
        }
        listener.start(queue: queue)
        _ = ready.wait(timeout: .now() + 5)
        port = Int(listener.port?.rawValue ?? 0)
    }

    func stop() {
        listener.cancel()
    }

    private func accept(_ connection: NWConnection) {
        connection.start(queue: queue)
        receive(on: connection)
    }

    private func receive(on connection: NWConnection) {
        connection.receiveMessage { [weak self] data, _, _, error in
            guard let self = self, error == nil else { return }
            if let data = data,
               let message = try? JSONSerialization.jsonObject(with: data) as? [String: Any] {
                self.run(self.script(message), on: connection)
            }
            self.receive(on: connection)
        }
    }

    private func run(_ replies: [Reply], on connection: NWConnection) {
        for reply in replies {
            switch reply {
            case .send(let text):
                let metadata = NWProtocolWebSocket.Metadata(opcode: .text)
                let context = NWConnection.ContentContext(identifier: "text", metadata: [metadata])
                connection.send(content: Data(text.utf8), contentContext: context, isComplete: true, completion: .idempotent)
            case .close:
                connection.send(content: nil, completion: .contentProcessed { _ in connection.cancel() })
            }
        }
    }
}
