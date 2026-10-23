import Foundation

/// Remembers each Mac's last theme, so the app opens looking like the Mac it
/// will reconnect to, and whether the phone should match the Mac at all.
public final class ThemeStore: @unchecked Sendable {
    private static let keyMatchMac = "theme.matchMac"
    private static let keyThemePrefix = "theme.byMac."

    private let defaults: UserDefaults

    public init(defaults: UserDefaults = .standard) {
        self.defaults = defaults
    }

    public var matchMac: Bool {
        get { defaults.object(forKey: Self.keyMatchMac) as? Bool ?? true }
        set { defaults.set(newValue, forKey: Self.keyMatchMac) }
    }

    public func save(_ theme: CompanionTheme, serverId: String?, name: String) {
        guard let data = try? JSONEncoder().encode(theme) else { return }
        defaults.set(data, forKey: Self.key(serverId: serverId, name: name))
    }

    public func load(serverId: String?, name: String) -> CompanionTheme? {
        guard let data = defaults.data(forKey: Self.key(serverId: serverId, name: name)) else { return nil }
        return try? JSONDecoder().decode(CompanionTheme.self, from: data)
    }

    public func clear(serverId: String?, name: String) {
        defaults.removeObject(forKey: Self.key(serverId: serverId, name: name))
    }

    /// Keyed by ReplayCap's stable id, or by name for builds that don't send one.
    private static func key(serverId: String?, name: String) -> String {
        keyThemePrefix + (serverId ?? name)
    }
}

public extension ThemeStore {
    func load(for mac: RememberedMac) -> CompanionTheme? {
        load(serverId: mac.serverId, name: mac.name)
    }

    func clear(for mac: RememberedMac) {
        clear(serverId: mac.serverId, name: mac.name)
    }
}
