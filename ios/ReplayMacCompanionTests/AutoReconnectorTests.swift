import XCTest
import Combine
@testable import ReplayMacCompanion

final class AutoReconnectorTests: XCTestCase {

    private enum Reply {
        case approves
        case refuses
        case silent
        case denies
    }

    private let office = MacEndpoint(host: "192.168.8.239", port: 7375)
    private let home = MacEndpoint(host: "192.168.68.74", port: 7375)
    private let usb = MacEndpoint(host: MacEndpoint.usbHost, port: 7375)
    private let mac = RememberedMac(serverId: "mac-1", name: "Alex’s MacBook Pro", networkEndpoint: MacEndpoint(host: "192.168.8.239", port: 7375), usbPort: 7375, lastUsedUsb: false)

    private final class FakeNetwork: @unchecked Sendable {
        let mac: RememberedMac
        let state = CurrentValueSubject<ConnectionState, Never>(.disconnected)
        let discovered = CurrentValueSubject<[DiscoveredMac], Never>([])
        private var replies: [MacEndpoint: Reply] = [:]
        private var attempts: [MacEndpoint] = []
        private var denied = false
        private let lock = NSLock()

        init(mac: RememberedMac) {
            self.mac = mac
        }

        func getAttempts() -> [MacEndpoint] {
            lock.lock()
            defer { lock.unlock() }
            return attempts
        }

        func setReply(for endpoint: MacEndpoint, reply: Reply) {
            lock.lock()
            defer { lock.unlock() }
            replies[endpoint] = reply
        }

        func isDenied() -> Bool {
            lock.lock()
            defer { lock.unlock() }
            return denied
        }

        lazy var reconnector: AutoReconnector = {
            AutoReconnector(
                connectionState: state.eraseToAnyPublisher(),
                discoveredMacs: discovered.eraseToAnyPublisher(),
                getCurrentDiscoveredMacs: { [weak self] in
                    self?.discovered.value ?? []
                },
                connect: { [weak self] endpoint in
                    self?.handleConnect(endpoint)
                },
                onPairingDenied: { [weak self] in
                    guard let self = self else { return }
                    self.lock.lock()
                    self.denied = true
                    self.lock.unlock()
                },
                attemptTimeoutMs: 150
            )
        }()

        private func handleConnect(_ endpoint: MacEndpoint) {
            lock.lock()
            attempts.append(endpoint)
            let reply = replies[endpoint] ?? .refuses
            lock.unlock()

            state.send(.connecting(host: endpoint.host, port: endpoint.port))

            DispatchQueue.global().asyncAfter(deadline: .now() + 0.01) { [weak self] in
                guard let self = self else { return }
                switch reply {
                case .approves:
                    self.state.send(.connected(serverName: self.mac.name, serverVersion: "1.7.3", host: endpoint.host, port: endpoint.port, serverId: "mac-1"))
                case .refuses:
                    self.state.send(.failed(error: "refused", host: endpoint.host, port: endpoint.port, wasConnected: false, pairingDenied: false))
                case .silent:
                    break
                case .denies:
                    self.state.send(.failed(error: "denied", host: endpoint.host, port: endpoint.port, wasConnected: false, pairingDenied: true))
                }
            }
        }

        func macAppearsAt(_ endpoint: MacEndpoint) {
            discovered.send([
                DiscoveredMac(id: "\(endpoint.host):\(endpoint.port)", name: mac.name, host: endpoint.host, port: endpoint.port, serverId: "mac-1")
            ])
        }
    }

    func testConnectsToTheMacWhereDiscoverySeesItNow() async {
        let net = FakeNetwork(mac: mac)
        net.setReply(for: home, reply: .approves)
        net.macAppearsAt(home)

        net.reconnector.start(mac)

        // Wait until connected and reconnector is settled
        for _ in 0..<50 {
            if net.state.value.isConnected && net.reconnector.currentTarget == nil { break }
            try? await Task.sleep(nanoseconds: 10_000_000)
        }

        XCTAssertEqual(net.getAttempts(), [home])
        XCTAssertTrue(net.state.value.isConnected)
        XCTAssertNil(net.reconnector.currentTarget)
        net.reconnector.stop()
    }

    func testFallsBackToUsbWhenTheOldNetworkAddressIsGone() async {
        let net = FakeNetwork(mac: mac)
        net.setReply(for: office, reply: .refuses)
        net.setReply(for: usb, reply: .approves)

        net.reconnector.start(mac)

        for _ in 0..<50 {
            if net.state.value.isConnected && net.reconnector.currentTarget == nil { break }
            try? await Task.sleep(nanoseconds: 10_000_000)
        }

        XCTAssertEqual(net.getAttempts(), [office, usb])
        XCTAssertTrue(net.state.value.isConnected)
        XCTAssertNil(net.reconnector.currentTarget)
        net.reconnector.stop()
    }

    func testStopsAndForgetsWhenTheMacDeniesThePhone() async {
        let net = FakeNetwork(mac: mac)
        net.setReply(for: office, reply: .denies)

        net.reconnector.start(mac)

        for _ in 0..<50 {
            if net.isDenied() && net.reconnector.currentTarget == nil { break }
            try? await Task.sleep(nanoseconds: 10_000_000)
        }

        XCTAssertEqual(net.getAttempts(), [office])
        XCTAssertTrue(net.isDenied())
        XCTAssertNil(net.reconnector.currentTarget)
        net.reconnector.stop()
    }

    func testAbandonsAHangingAddressAsSoonAsTheMacIsFoundElsewhere() async {
        let net = FakeNetwork(mac: mac)
        net.setReply(for: office, reply: .silent)
        net.setReply(for: home, reply: .approves)

        net.reconnector.start(mac)

        // Wait until office was attempted
        for _ in 0..<50 {
            if net.getAttempts().contains(office) { break }
            try? await Task.sleep(nanoseconds: 10_000_000)
        }

        XCTAssertEqual(net.getAttempts(), [office])

        // Well before attempt timeout, the Mac appears at home
        net.macAppearsAt(home)

        // Wait until connected to home and idle
        for _ in 0..<50 {
            if net.state.value.isConnected && net.reconnector.currentTarget == nil { break }
            try? await Task.sleep(nanoseconds: 10_000_000)
        }

        XCTAssertEqual(net.getAttempts(), [office, home])
        XCTAssertTrue(net.state.value.isConnected)
        XCTAssertNil(net.reconnector.currentTarget)
        net.reconnector.stop()
    }

    func testKeepsLookingAfterEveryAddressFailsAndWakesWhenTheMacAppears() async {
        let net = FakeNetwork(mac: mac)
        net.setReply(for: office, reply: .refuses)
        net.setReply(for: usb, reply: .refuses)
        net.setReply(for: home, reply: .approves)

        net.reconnector.start(mac)

        // Wait until office and usb have both been tried
        for _ in 0..<50 {
            if net.getAttempts().count >= 2 { break }
            try? await Task.sleep(nanoseconds: 10_000_000)
        }

        XCTAssertEqual(net.getAttempts(), [office, usb])
        XCTAssertEqual(net.reconnector.currentTarget, mac)

        // During the backoff wait, Mac appears at home
        net.macAppearsAt(home)

        // Wait until connected and reconnector returns to idle
        for _ in 0..<50 {
            if net.state.value.isConnected && net.reconnector.currentTarget == nil { break }
            try? await Task.sleep(nanoseconds: 10_000_000)
        }

        XCTAssertEqual(net.getAttempts(), [office, usb, home])
        XCTAssertTrue(net.state.value.isConnected)
        XCTAssertNil(net.reconnector.currentTarget)
        net.reconnector.stop()
    }

    func testRetriesKnownAddressesAfterTheBackoff() async {
        let net = FakeNetwork(mac: mac)
        net.setReply(for: office, reply: .refuses)
        net.setReply(for: usb, reply: .refuses)

        net.reconnector.start(mac)

        // Wait until office and usb both tried
        for _ in 0..<50 {
            if net.getAttempts().count >= 2 { break }
            try? await Task.sleep(nanoseconds: 10_000_000)
        }

        XCTAssertEqual(net.getAttempts(), [office, usb])

        // Mac wakes up and starts answering
        net.setReply(for: office, reply: .approves)

        // Wait for round 1 backoff (1s) to elapse and office to be retried
        for _ in 0..<150 {
            if net.state.value.isConnected && net.reconnector.currentTarget == nil { break }
            try? await Task.sleep(nanoseconds: 20_000_000)
        }

        XCTAssertTrue(net.getAttempts().count >= 3)
        XCTAssertEqual(net.getAttempts().last, office)
        XCTAssertTrue(net.state.value.isConnected)
        XCTAssertNil(net.reconnector.currentTarget)
        net.reconnector.stop()
    }

    func testStopEndsTheSearch() async {
        let net = FakeNetwork(mac: mac)
        net.setReply(for: office, reply: .refuses)
        net.setReply(for: usb, reply: .refuses)

        net.reconnector.start(mac)
        try? await Task.sleep(nanoseconds: 50_000_000)
        net.reconnector.stop()

        XCTAssertNil(net.reconnector.currentTarget)
    }
}
