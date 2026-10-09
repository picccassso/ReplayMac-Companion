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

    func testMultiDevicePersistenceAndOrdering() {
        let suite = UserDefaults(suiteName: "test.multi.\(UUID().uuidString)")!
        let store = RememberedMacStore(defaults: suite)

        let mac1 = RememberedMac(
            serverId: "mac-1",
            name: "MacBook Pro",
            networkEndpoint: MacEndpoint(host: "192.168.1.10", port: 7375),
            usbPort: nil,
            lastUsedUsb: false
        )
        let mac2 = RememberedMac(
            serverId: "mac-2",
            name: "Mac mini",
            networkEndpoint: MacEndpoint(host: "192.168.1.20", port: 7375),
            usbPort: nil,
            lastUsedUsb: false
        )

        store.save(mac1)
        store.save(mac2)

        let all = store.loadAll()
        XCTAssertEqual(all.count, 2)
        XCTAssertEqual(all[0].name, "Mac mini")
        XCTAssertEqual(all[1].name, "MacBook Pro")
        XCTAssertEqual(store.loadLatest()?.name, "Mac mini")

        // Re-saving mac1 promotes it to the top
        store.save(mac1)
        let updated = store.loadAll()
        XCTAssertEqual(updated.count, 2)
        XCTAssertEqual(updated[0].name, "MacBook Pro")
        XCTAssertEqual(updated[1].name, "Mac mini")
        XCTAssertEqual(store.loadLatest()?.name, "Mac MacBook Pro".isEmpty ? "MacBook Pro" : "MacBook Pro")
    }

    func testRemoveMac() {
        let suite = UserDefaults(suiteName: "test.remove.\(UUID().uuidString)")!
        let store = RememberedMacStore(defaults: suite)

        let mac1 = RememberedMac(
            serverId: "mac-1",
            name: "MacBook Pro",
            networkEndpoint: MacEndpoint(host: "192.168.1.10", port: 7375),
            usbPort: nil,
            lastUsedUsb: false
        )
        let mac2 = RememberedMac(
            serverId: "mac-2",
            name: "Mac Studio",
            networkEndpoint: MacEndpoint(host: "192.168.1.30", port: 7375),
            usbPort: nil,
            lastUsedUsb: false
        )

        store.save(mac1)
        store.save(mac2)
        XCTAssertEqual(store.loadAll().count, 2)

        store.remove(mac2)
        let remaining = store.loadAll()
        XCTAssertEqual(remaining.count, 1)
        XCTAssertEqual(remaining.first?.name, "MacBook Pro")
        XCTAssertEqual(store.loadLatest()?.name, "MacBook Pro")
    }

    func testLegacyMigration() {
        let suite = UserDefaults(suiteName: "test.legacy.\(UUID().uuidString)")!
        suite.set("Legacy MacBook", forKey: "remembered_name")
        suite.set("server-legacy-1", forKey: "remembered_server_id")
        suite.set("192.168.1.55", forKey: "remembered_network_host")
        suite.set(7375, forKey: "remembered_network_port")
        suite.set(0, forKey: "remembered_usb_port")
        suite.set(false, forKey: "remembered_last_used_usb")

        let store = RememberedMacStore(defaults: suite)
        let list = store.loadAll()

        XCTAssertEqual(list.count, 1)
        XCTAssertEqual(list.first?.name, "Legacy MacBook")
        XCTAssertEqual(list.first?.serverId, "server-legacy-1")
        XCTAssertEqual(list.first?.networkEndpoint?.host, "192.168.1.55")

        // Next load loads from migrated list key
        let secondLoad = store.loadAll()
        XCTAssertEqual(secondLoad.count, 1)
        XCTAssertEqual(secondLoad.first?.name, "Legacy MacBook")
    }

    func testPreviouslyConnectedFilteringAndOnlineMatching() {
        let savedMac = RememberedMac(
            serverId: "srv-1",
            name: "Studio Mac",
            networkEndpoint: MacEndpoint(host: "192.168.1.10", port: 7375),
            usbPort: nil,
            lastUsedUsb: false
        )

        let discovered1 = DiscoveredMac(
            id: "d1",
            name: "Studio Mac",
            host: "192.168.1.10",
            port: 7375,
            serverId: "srv-1",
            lastSeenTimestamp: Date()
        )

        let discovered2 = DiscoveredMac(
            id: "d2",
            name: "Living Room Mac",
            host: "192.168.1.12",
            port: 7375,
            serverId: "srv-2",
            lastSeenTimestamp: Date()
        )

        let discovered = [discovered1, discovered2]
        let saved = [savedMac]

        // Filtered discovered: srv-1 excluded because it's in saved
        let filtered = discovered.filter { disc in
            !saved.contains { $0.isSameMacAs(disc) }
        }
        XCTAssertEqual(filtered.count, 1)
        XCTAssertEqual(filtered.first?.name, "Living Room Mac")

        // Previously connected items: srv-1 marked online
        let items = saved.map { mac -> PreviouslyConnectedItem in
            let match = discovered.first { mac.isSameMacAs($0) }
            return PreviouslyConnectedItem(
                mac: mac,
                isOnline: match != nil,
                onlineEndpoint: match.map { MacEndpoint(host: $0.host, port: $0.port) }
            )
        }
        XCTAssertEqual(items.count, 1)
        XCTAssertTrue(items[0].isOnline)
        XCTAssertEqual(items[0].onlineEndpoint?.host, "192.168.1.10")
    }
}

