import Foundation

/// Decides where to look for a remembered Mac, and how long to wait between rounds.
public enum ReconnectPlan {
    public static let firstBackoffMs: Int64 = 1_000
    public static let maxBackoffMs: Int64 = 15_000

    public static func isSameMac(discovered: DiscoveredMac, remembered: RememberedMac) -> Bool {
        remembered.isSameMacAs(otherServerId: discovered.serverId, otherName: discovered.name)
    }

    /// Addresses to try, best first: wherever discovery currently sees the Mac
    /// on this network, then the way it was last reached (network or USB),
    /// then the other one.
    public static func candidates(remembered: RememberedMac, discovered: [DiscoveredMac]) -> [MacEndpoint] {
        let seenNow = discovered
            .filter { isSameMac(discovered: $0, remembered: remembered) }
            .map { MacEndpoint(host: $0.host, port: $0.port) }

        let usb = remembered.usbPort.map { MacEndpoint(host: MacEndpoint.usbHost, port: $0) }
        let previous: [MacEndpoint]
        if remembered.lastUsedUsb {
            previous = [usb, remembered.networkEndpoint].compactMap { $0 }
        } else {
            previous = [remembered.networkEndpoint, usb].compactMap { $0 }
        }

        var result: [MacEndpoint] = []
        var seen = Set<MacEndpoint>()
        for endpoint in (seenNow + previous) {
            if !seen.contains(endpoint) {
                seen.insert(endpoint)
                result.append(endpoint)
            }
        }
        return result
    }

    /// 1s, 2s, 4s, 8s, then every 15s.
    public static func backoffMillis(round: Int) -> Int64 {
        let clamped = min(max(round, 0), 4)
        let doubled = firstBackoffMs << clamped
        return min(doubled, maxBackoffMs)
    }
}
