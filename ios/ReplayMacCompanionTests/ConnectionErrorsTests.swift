import XCTest
@testable import ReplayMacCompanion

final class ConnectionErrorsTests: XCTestCase {

    func testADroppedConnectionNeverReadsAsAFailedConnect() {
        let err = NSError(domain: NSPOSIXErrorDomain, code: Int(POSIXError.ECONNRESET.rawValue), userInfo: nil)
        XCTAssertEqual(
            "The connection to your Mac was interrupted.",
            ConnectionErrors.describe(err, wasConnected: true)
        )
    }

    func testNetworkExceptionsGetAReadableReason() {
        let connRefused = NSError(domain: NSPOSIXErrorDomain, code: Int(POSIXError.ECONNREFUSED.rawValue), userInfo: nil)
        XCTAssertEqual(
            "Nothing is listening there. Check Companion Server is on in ReplayMac.",
            ConnectionErrors.describe(connRefused, wasConnected: false)
        )

        let timedOut = NSError(domain: NSURLErrorDomain, code: NSURLErrorTimedOut, userInfo: nil)
        XCTAssertEqual(
            "Your Mac didn't respond. Check it's awake and on the same network.",
            ConnectionErrors.describe(timedOut, wasConnected: false)
        )

        let cannotFind = NSError(domain: NSURLErrorDomain, code: NSURLErrorCannotFindHost, userInfo: nil)
        XCTAssertEqual(
            "That address couldn't be found.",
            ConnectionErrors.describe(cannotFind, wasConnected: false)
        )

        let notConnected = NSError(domain: NSURLErrorDomain, code: NSURLErrorNotConnectedToInternet, userInfo: nil)
        XCTAssertEqual(
            "Your Mac isn't reachable from this network.",
            ConnectionErrors.describe(notConnected, wasConnected: false)
        )
    }

    func testUnknownErrorsKeepTheirOwnMessage() {
        struct CustomError: LocalizedError {
            var errorDescription: String? { "Custom server error" }
        }
        XCTAssertEqual("Custom server error", ConnectionErrors.describe(CustomError(), wasConnected: false))
    }
}
