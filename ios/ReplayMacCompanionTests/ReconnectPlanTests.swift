import XCTest
@testable import ReplayCapCompanion

final class ReconnectPlanTests: XCTestCase {

    private let office = MacEndpoint(host: "10.0.0.42", port: 7375)
    private let usb = MacEndpoint(host: MacEndpoint.usbHost, port: 7375)

    private var mac: RememberedMac {
        RememberedMac(
            serverId: "mac-1",
            name: "Alex’s MacBook Pro",
            networkEndpoint: office,
            usbPort: 7375,
            lastUsedUsb: false
        )
    }

    private func discovered(host: String, serverId: String? = "mac-1", name: String = "Alex’s MacBook Pro") -> DiscoveredMac {
        DiscoveredMac(id: "\(host):7375", name: name, host: host, port: 7375, serverId: serverId)
    }

    func testMacSeenOnTheCurrentNetworkIsTriedBeforeItsOldAddress() {
        let home = discovered(host: "192.168.0.20")
        let candidates = ReconnectPlan.candidates(remembered: mac, discovered: [home])
        XCTAssertEqual(
            candidates,
            [MacEndpoint(host: "192.168.0.20", port: 7375), office, usb]
        )
    }

    func testUsbComesFirstWhenThatWasTheLastRoute() {
        let usbMac = RememberedMac(
            serverId: mac.serverId,
            name: mac.name,
            networkEndpoint: mac.networkEndpoint,
            usbPort: mac.usbPort,
            lastUsedUsb: true
        )
        let candidates = ReconnectPlan.candidates(remembered: usbMac, discovered: [])
        XCTAssertEqual(candidates, [usb, office])
    }

    func testAnotherMacOnTheNetworkIsNotACandidate() {
        let colleague = discovered(host: "192.168.0.30", serverId: "mac-2", name: "Sam’s iMac")
        let candidates = ReconnectPlan.candidates(remembered: mac, discovered: [colleague])
        XCTAssertEqual(candidates, [office, usb])
    }

    func testSameAddressIsOnlyTriedOnce() {
        let candidates = ReconnectPlan.candidates(remembered: mac, discovered: [discovered(host: office.host)])
        XCTAssertEqual(candidates, [office, usb])
    }

    func testMatchesByServerIdEvenWhenTheMacIsRenamed() {
        XCTAssertTrue(ReconnectPlan.isSameMac(discovered: discovered(host: "10.0.0.2", name: "Renamed Mac"), remembered: mac))
        XCTAssertFalse(ReconnectPlan.isSameMac(discovered: discovered(host: "10.0.0.2", serverId: "mac-2"), remembered: mac))
    }

    func testFallsBackToNameForMacsThatAdvertiseNoId() {
        XCTAssertTrue(ReconnectPlan.isSameMac(discovered: discovered(host: "10.0.0.2", serverId: nil), remembered: mac))
        XCTAssertFalse(ReconnectPlan.isSameMac(discovered: discovered(host: "10.0.0.2", serverId: nil, name: "Other"), remembered: mac))
    }

    func testBackoffDoublesThenCaps() {
        let actual = (0...5).map(ReconnectPlan.backoffMillis)
        XCTAssertEqual(actual, [1_000, 2_000, 4_000, 8_000, 15_000, 15_000])
    }
}
