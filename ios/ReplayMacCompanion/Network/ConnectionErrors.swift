import Foundation

/// Turns socket and network errors into wording a person can act on.
public enum ConnectionErrors {

    public static func describe(_ error: Error, wasConnected: Bool) -> String {
        if wasConnected {
            return "The connection to your Mac was interrupted."
        }

        let nsError = error as NSError

        if nsError.domain == NSURLErrorDomain {
            switch nsError.code {
            case NSURLErrorTimedOut:
                return "Your Mac didn't respond. Check it's awake and on the same network."
            case NSURLErrorCannotConnectToHost:
                return "Nothing is listening there. Check Companion Server is on in ReplayCap."
            case NSURLErrorCannotFindHost, NSURLErrorDNSLookupFailed:
                return "That address couldn't be found."
            case NSURLErrorNotConnectedToInternet, NSURLErrorNetworkConnectionLost:
                return "Your Mac isn't reachable from this network."
            default:
                break
            }
        }

        if nsError.domain == NSPOSIXErrorDomain {
            switch POSIXErrorCode(rawValue: Int32(nsError.code)) {
            case .ECONNREFUSED:
                return "Nothing is listening there. Check Companion Server is on in ReplayCap."
            case .ETIMEDOUT:
                return "Your Mac didn't respond. Check it's awake and on the same network."
            case .EHOSTUNREACH, .ENETUNREACH:
                return "Your Mac isn't reachable from this network."
            case .ECONNRESET:
                return "ReplayCap closed the connection."
            default:
                break
            }
        }

        let message = error.localizedDescription.trimmingCharacters(in: .whitespacesAndNewlines)
        if !message.isEmpty && message != "The operation couldn’t be completed." {
            return message
        }

        return "Connection failed"
    }
}
