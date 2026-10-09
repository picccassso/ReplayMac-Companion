import XCTest
import Combine
@testable import ReplayMacCompanion

final class CompanionWebSocketClientTests: XCTestCase {

    private var cancellables = Set<AnyCancellable>()

    func testConnectingStateDoesNotHoldLock() {
        let client = CompanionWebSocketClient(deviceId: "dev-1", deviceName: "Test Phone")

        let exp = expectation(description: "Connecting state received without deadlock")

        client.connectionState
            .sink { state in
                if case .connecting = state {
                    // This synchronously queries the client's currentConnectionState;
                    // if connect() held the lock while publishing .connecting, this would deadlock.
                    let current = client.currentConnectionState
                    XCTAssertTrue(current.isConnecting)
                    exp.fulfill()
                }
            }
            .store(in: &cancellables)

        client.connect(host: "127.0.0.1", port: 7375)
        wait(for: [exp], timeout: 2.0)
        client.disconnect()
    }

    func testDisconnectTransitionsToDisconnected() {
        let client = CompanionWebSocketClient(deviceId: "dev-1", deviceName: "Test Phone")
        client.connect(host: "127.0.0.1", port: 7375)
        XCTAssertTrue(client.currentConnectionState.isConnecting)

        client.disconnect()
        XCTAssertEqual(client.currentConnectionState, .disconnected)
    }

    func testResyncRequiresConnectedState() {
        let client = CompanionWebSocketClient(deviceId: "dev-1", deviceName: "Test Phone")
        XCTAssertFalse(client.resync())
    }

    func testClearErrorTransitionsFailedToDisconnected() {
        let client = CompanionWebSocketClient(deviceId: "dev-1", deviceName: "Test Phone")
        // Invalid URL forces immediate failure
        client.connect(host: "invalid::host::address", port: -1)
        if case .failed = client.currentConnectionState {
            client.clearError()
            XCTAssertEqual(client.currentConnectionState, .disconnected)
        }
    }
}
