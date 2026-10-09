import Foundation

/// Persists the last paired Mac so the app can find it again on its next launch.
public final class RememberedMacStore: @unchecked Sendable {
    private static let keyServerId = "remembered_server_id"
    private static let keyName = "remembered_name"
    private static let keyNetworkHost = "remembered_network_host"
    private static let keyNetworkPort = "remembered_network_port"
    private static let keyUsbPort = "remembered_usb_port"
    private static let keyLastUsedUsb = "remembered_last_used_usb"

    private let defaults: UserDefaults

    public init(defaults: UserDefaults = .standard) {
        self.defaults = defaults
    }

    public func load() -> RememberedMac? {
        guard let name = defaults.string(forKey: Self.keyName) else { return nil }
        let serverId = defaults.string(forKey: Self.keyServerId)
        let networkHost = defaults.string(forKey: Self.keyNetworkHost)
        let networkPort = defaults.integer(forKey: Self.keyNetworkPort)
        let usbPort = defaults.integer(forKey: Self.keyUsbPort)
        let lastUsedUsb = defaults.bool(forKey: Self.keyLastUsedUsb)

        let networkEndpoint: MacEndpoint?
        if let host = networkHost, !host.isEmpty, networkPort > 0 {
            networkEndpoint = MacEndpoint(host: host, port: networkPort)
        } else {
            networkEndpoint = nil
        }

        return RememberedMac(
            serverId: serverId,
            name: name,
            networkEndpoint: networkEndpoint,
            usbPort: usbPort > 0 ? usbPort : nil,
            lastUsedUsb: lastUsedUsb
        )
    }

    public func save(_ mac: RememberedMac) {
        defaults.set(mac.serverId, forKey: Self.keyServerId)
        defaults.set(mac.name, forKey: Self.keyName)
        defaults.set(mac.networkEndpoint?.host, forKey: Self.keyNetworkHost)
        defaults.set(mac.networkEndpoint?.port ?? 0, forKey: Self.keyNetworkPort)
        defaults.set(mac.usbPort ?? 0, forKey: Self.keyUsbPort)
        defaults.set(mac.lastUsedUsb, forKey: Self.keyLastUsedUsb)
    }

    public func clear() {
        defaults.removeObject(forKey: Self.keyServerId)
        defaults.removeObject(forKey: Self.keyName)
        defaults.removeObject(forKey: Self.keyNetworkHost)
        defaults.removeObject(forKey: Self.keyNetworkPort)
        defaults.removeObject(forKey: Self.keyUsbPort)
        defaults.removeObject(forKey: Self.keyLastUsedUsb)
    }
}
