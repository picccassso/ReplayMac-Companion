import Foundation

/// Holds the secret each Mac handed this phone when it was approved, keyed by
/// the Mac's server id.
public protocol PairingSecretStoring: AnyObject, Sendable {
    func secret(forServerId serverId: String) -> Data?
    func save(_ secret: Data, forServerId serverId: String)
    func remove(forServerId serverId: String)
}

public final class KeychainPairingSecretStore: PairingSecretStoring, @unchecked Sendable {
    private let keychain: KeychainStore

    public init(keychain: KeychainStore = KeychainStore(service: "com.replaycap.companion.pairing")) {
        self.keychain = keychain
    }

    public func secret(forServerId serverId: String) -> Data? {
        keychain.data(for: serverId)
    }

    public func save(_ secret: Data, forServerId serverId: String) {
        keychain.set(secret, for: serverId)
    }

    public func remove(forServerId serverId: String) {
        keychain.remove(serverId)
    }
}

/// This phone's identity towards every Mac. Kept in the Keychain rather than
/// UserDefaults so that restoring a backup onto another phone gives that phone
/// a new identity instead of a copy of this one.
public enum DeviceIdentity {
    static let account = "device_id"

    public static func loadOrCreate(
        keychain: KeychainStore = KeychainStore(service: "com.replaycap.companion.device")
    ) -> String {
        if let data = keychain.data(for: account),
           let existing = String(data: data, encoding: .utf8),
           !existing.isEmpty {
            return existing
        }
        let created = UUID().uuidString
        keychain.set(Data(created.utf8), for: account)
        return created
    }
}
