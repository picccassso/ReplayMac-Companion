import Foundation

/// Persists paired Macs so the app can find them again, maintaining an ordered history
/// with the most recently used Mac first.
public final class RememberedMacStore: @unchecked Sendable {
    private static let keyServerId = "remembered_server_id"
    private static let keyName = "remembered_name"
    private static let keyNetworkHost = "remembered_network_host"
    private static let keyNetworkPort = "remembered_network_port"
    private static let keyUsbPort = "remembered_usb_port"
    private static let keyLastUsedUsb = "remembered_last_used_usb"
    private static let keyMacsList = "remembered_macs_list"

    private let defaults: UserDefaults

    public init(defaults: UserDefaults = .standard) {
        self.defaults = defaults
    }

    /// Loads all remembered Macs in order (most recently used first).
    /// Automatically migrates from the legacy single-Mac preferences if needed.
    public func loadAll() -> [RememberedMac] {
        if let data = defaults.data(forKey: Self.keyMacsList),
           let macs = try? JSONDecoder().decode([RememberedMac].self, from: data) {
            return macs
        }

        // Migrate legacy single-device format if present
        if let legacy = loadLegacy() {
            let list = [legacy]
            persistList(list)
            return list
        }

        return []
    }

    /// The most recently connected Mac, or nil if none saved.
    public func loadLatest() -> RememberedMac? {
        loadAll().first
    }

    /// Backward-compatible load for auto-reconnect, returning the latest Mac.
    public func load() -> RememberedMac? {
        loadLatest()
    }

    public func save(_ mac: RememberedMac) {
        var macs = loadAll()
        // Deduplicate any existing entry for this Mac
        macs.removeAll { $0.isSameMacAs(otherServerId: mac.serverId, otherName: mac.name) }
        // Insert as most recently used at top
        macs.insert(mac, at: 0)
        persistList(macs)
        syncLegacy(mac)
    }

    public func remove(serverId: String?, name: String) {
        var macs = loadAll()
        macs.removeAll { $0.isSameMacAs(otherServerId: serverId, otherName: name) }
        persistList(macs)
        if let first = macs.first {
            syncLegacy(first)
        } else {
            clearLegacy()
        }
    }

    public func remove(_ mac: RememberedMac) {
        remove(serverId: mac.serverId, name: mac.name)
    }

    public func clear() {
        defaults.removeObject(forKey: Self.keyMacsList)
        clearLegacy()
    }

    // MARK: - Private Helpers

    private func persistList(_ list: [RememberedMac]) {
        if let data = try? JSONEncoder().encode(list) {
            defaults.set(data, forKey: Self.keyMacsList)
        }
    }

    private func loadLegacy() -> RememberedMac? {
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

    private func syncLegacy(_ mac: RememberedMac) {
        defaults.set(mac.serverId, forKey: Self.keyServerId)
        defaults.set(mac.name, forKey: Self.keyName)
        defaults.set(mac.networkEndpoint?.host, forKey: Self.keyNetworkHost)
        defaults.set(mac.networkEndpoint?.port ?? 0, forKey: Self.keyNetworkPort)
        defaults.set(mac.usbPort ?? 0, forKey: Self.keyUsbPort)
        defaults.set(mac.lastUsedUsb, forKey: Self.keyLastUsedUsb)
    }

    private func clearLegacy() {
        defaults.removeObject(forKey: Self.keyServerId)
        defaults.removeObject(forKey: Self.keyName)
        defaults.removeObject(forKey: Self.keyNetworkHost)
        defaults.removeObject(forKey: Self.keyNetworkPort)
        defaults.removeObject(forKey: Self.keyUsbPort)
        defaults.removeObject(forKey: Self.keyLastUsedUsb)
    }
}
