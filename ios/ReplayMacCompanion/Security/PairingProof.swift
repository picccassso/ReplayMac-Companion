import Foundation
import CryptoKit

/// Answers ReplayCap's sign-in challenge. The phone proves it holds the
/// secret it was given at pairing without ever sending the secret again.
public enum PairingProof {
    static let context = "replaycap-companion-auth-v1"

    /// Base64 HMAC-SHA256 of the challenge, keyed with the pairing secret.
    public static func make(secret: Data, serverId: String, deviceId: String, nonce: String) -> String {
        let message = Data("\(context)\n\(serverId)\n\(deviceId)\n\(nonce)".utf8)
        let code = HMAC<SHA256>.authenticationCode(for: message, using: SymmetricKey(data: secret))
        return Data(code).base64EncodedString()
    }
}
