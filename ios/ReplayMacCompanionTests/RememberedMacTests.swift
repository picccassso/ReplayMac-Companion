import XCTest
@testable import ReplayMacCompanion

final class RememberedMacTests: XCTestCase {

    private let office = MacEndpoint(host: "192.168.8.239", port: 7375)
    private let home = MacEndpoint(host: "192.168.68.74", port: 7375)
    private let usb = MacEndpoint(host: MacEndpoint.usbHost, port: 7375)

    private var paired: RememberedMac {
        RememberedMac.afterConnecting(previous: nil, serverId: "mac-1", serverName: "Alex’s MacBook Pro", endpoint: office)
    }

    func testFirstConnectionRemembersTheMacAndItsAddress() {
        XCTAssertEqual(paired.serverId, "mac-1")
        XCTAssertEqual(paired.networkEndpoint, office)
        XCTAssertNil(paired.usbPort)
        XCTAssertFalse(paired.lastUsedUsb)
    }

    func testConnectingOverUsbKeepsTheNetworkAddress() {
        let viaUsb = RememberedMac.afterConnecting(previous: paired, serverId: "mac-1", serverName: "Alex’s MacBook Pro", endpoint: usb)
        XCTAssertEqual(viaUsb.networkEndpoint, office)
        XCTAssertEqual(viaUsb.usbPort, 7375)
        XCTAssertTrue(viaUsb.lastUsedUsb)
    }

    func testNewNetworkAddressReplacesTheOldOneAndKeepsUsb() {
        let viaUsb = RememberedMac.afterConnecting(previous: paired, serverId: "mac-1", serverName: "Alex’s MacBook Pro", endpoint: usb)
        let atHome = RememberedMac.afterConnecting(previous: viaUsb, serverId: "mac-1", serverName: "Alex’s MacBook Pro", endpoint: home)
        XCTAssertEqual(atHome.networkEndpoint, home)
        XCTAssertEqual(atHome.usbPort, 7375)
        XCTAssertFalse(atHome.lastUsedUsb)
    }

    func testPairingWithADifferentMacForgetsTheOldRoutes() {
        let viaUsb = RememberedMac.afterConnecting(previous: paired, serverId: "mac-1", serverName: "Alex’s MacBook Pro", endpoint: usb)
        let other = RememberedMac.afterConnecting(previous: viaUsb, serverId: "mac-2", serverName: "Sam’s iMac", endpoint: home)
        XCTAssertEqual(other.serverId, "mac-2")
        XCTAssertEqual(other.networkEndpoint, home)
        XCTAssertNil(other.usbPort)
    }

    func testLearnsTheServerIdOnceTheMacStartsSendingOne() {
        let beforeUpdate = RememberedMac.afterConnecting(previous: nil, serverId: nil, serverName: "Alex’s MacBook Pro", endpoint: office)
        let afterUpdate = RememberedMac.afterConnecting(previous: beforeUpdate, serverId: "mac-1", serverName: "Alex’s MacBook Pro", endpoint: office)
        XCTAssertEqual(afterUpdate.serverId, "mac-1")
    }

    func testPersistenceRoundTrip() {
        let suite = UserDefaults(suiteName: "test.remembered.mac.\(UUID().uuidString)")!
        let store = RememberedMacStore(defaults: suite)

        XCTAssertNil(store.load())

        let sample = RememberedMac(
            serverId: "server-xyz",
            name: "Gaming Mac",
            networkEndpoint: MacEndpoint(host: "10.0.0.42", port: 7375),
            usbPort: 7375,
            lastUsedUsb: true
        )
        store.save(sample)

        let loaded = store.load()
        XCTAssertEqual(loaded, sample)

        store.clear()
        XCTAssertNil(store.load())
    }
}
