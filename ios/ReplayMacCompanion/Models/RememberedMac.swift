import Foundation

/// A host and port the companion can open a socket to.
public struct MacEndpoint: Sendable, Equatable, Hashable, Codable {
    public let host: String
    public let port: Int

    public var isUsb: Bool { host == Self.usbHost }

    public static let usbHost = "127.0.0.1"

    public init(host: String, port: Int) {
        self.host = host
        self.port = port
    }
}

/// The Mac this phone last paired with, remembered by identity rather than by
/// address so it can be found again on any network or over USB.
public struct RememberedMac: Sendable, Equatable, Hashable, Codable {
    /// ReplayMac's stable server id; nil when paired with a build that doesn't send one.
    public let serverId: String?
    public let name: String
    /// Last address the Mac was reached at over a network.
    public let networkEndpoint: MacEndpoint?
    /// Port last used over USB, if this Mac was ever reached that way.
    public let usbPort: Int?
    /// Whether the most recent connection went over USB rather than the network.
    public let lastUsedUsb: Bool

    public init(
        serverId: String?,
        name: String,
        networkEndpoint: MacEndpoint?,
        usbPort: Int?,
        lastUsedUsb: Bool
    ) {
        self.serverId = serverId
        self.name = name
        self.networkEndpoint = networkEndpoint
        self.usbPort = usbPort
        self.lastUsedUsb = lastUsedUsb
    }

    /// What to remember after connecting to `serverName` at `endpoint`. Keeps
    /// the other route (network or USB) when it's the same Mac as `previous`,
    /// and starts afresh when the phone has paired with a different one.
    public static func afterConnecting(
        previous: RememberedMac?,
        serverId: String?,
        serverName: String,
        endpoint: MacEndpoint
    ) -> RememberedMac {
        let base: RememberedMac
        if let prev = previous, prev.isSameMacAs(otherServerId: serverId, otherName: serverName) {
            base = prev
        } else {
            base = RememberedMac(serverId: serverId, name: serverName, networkEndpoint: nil, usbPort: nil, lastUsedUsb: false)
        }

        let updated = RememberedMac(
            serverId: serverId ?? base.serverId,
            name: serverName,
            networkEndpoint: base.networkEndpoint,
            usbPort: base.usbPort,
            lastUsedUsb: base.lastUsedUsb
        )
        return updated.reachedAt(endpoint)
    }

    /// Matches by server id when both sides have one, otherwise by name (older
    /// ReplayMac builds send no id).
    public func isSameMacAs(otherServerId: String?, otherName: String) -> Bool {
        if let myId = serverId, let otherId = otherServerId {
            return myId == otherId
        }
        return name == otherName
    }

    /// Remembers `endpoint` as the latest way this Mac was reached.
    public func reachedAt(_ endpoint: MacEndpoint) -> RememberedMac {
        if endpoint.isUsb {
            return RememberedMac(
                serverId: serverId,
                name: name,
                networkEndpoint: networkEndpoint,
                usbPort: endpoint.port,
                lastUsedUsb: true
            )
        } else {
            return RememberedMac(
                serverId: serverId,
                name: name,
                networkEndpoint: endpoint,
                usbPort: usbPort,
                lastUsedUsb: false
            )
        }
    }
}
