import Foundation

// MARK: - Server Event

public enum CompanionServerEvent: Sendable, Equatable {
    case auth(CompanionAuthResponse)
    case snapshot(CompanionStateSnapshot)
    case stateChanged(CompanionStateSnapshot)
    case telemetry(CompanionTelemetryTick)
    case thumbnail(clipId: String, base64: String?)
    case pong
    case error(String)
    case unknown(rawType: String, rawJson: String)
}

// MARK: - Connection State

public enum ConnectionState: Sendable, Equatable {
    case disconnected
    case connecting(host: String, port: Int)
    case pendingApproval(serverName: String, host: String, port: Int)
    /// Finding a remembered Mac again, on whichever network or cable it is reachable.
    case reconnecting(serverName: String)
    case connected(serverName: String, serverVersion: String, host: String, port: Int, serverId: String? = nil, deviceName: String? = nil)
    /// `wasConnected` separates a dropped connection from one that never opened.
    case failed(error: String, host: String? = nil, port: Int? = nil, wasConnected: Bool = false, pairingDenied: Bool = false)

    public var isConnected: Bool {
        if case .connected = self { return true }
        return false
    }

    public var isConnecting: Bool {
        if case .connecting = self { return true }
        return false
    }

    public var isPendingApproval: Bool {
        if case .pendingApproval = self { return true }
        return false
    }

    public var isReconnecting: Bool {
        if case .reconnecting = self { return true }
        return false
    }
}

// MARK: - Discovered Mac

public struct DiscoveredMac: Identifiable, Sendable, Equatable, Hashable {
    public let id: String
    public let name: String
    public let host: String
    public let port: Int
    /// ReplayMac's stable identity from its Bonjour TXT record, when it advertises one.
    public let serverId: String?
    public let lastSeenTimestamp: Date

    public init(
        id: String,
        name: String,
        host: String,
        port: Int,
        serverId: String? = nil,
        lastSeenTimestamp: Date = Date()
    ) {
        self.id = id
        self.name = name
        self.host = host
        self.port = port
        self.serverId = serverId
        self.lastSeenTimestamp = lastSeenTimestamp
    }
}
