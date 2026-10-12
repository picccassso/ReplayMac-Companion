import Foundation

public enum CompanionProtocolParser {

    private static let decoder = JSONDecoder()
    private static let encoder: JSONEncoder = {
        let enc = JSONEncoder()
        enc.outputFormatting = [.sortedKeys]
        return enc
    }()

    private struct TypeEnvelope: Decodable {
        let type: String?
    }

    public static func parse(_ text: String) -> CompanionServerEvent {
        guard let data = text.data(using: .utf8) else {
            return .error("JSON Parse failed: Invalid UTF-8")
        }

        do {
            guard let jsonObject = try JSONSerialization.jsonObject(with: data) as? [String: Any] else {
                return .error("JSON Parse failed: Root is not an object")
            }

            let type = (jsonObject["type"] as? String) ?? ""

            switch type {
            case "auth_response":
                let auth = try decoder.decode(CompanionAuthResponse.self, from: data)
                return .auth(auth)

            case "auth_challenge":
                return .challenge(try decoder.decode(CompanionAuthChallenge.self, from: data))

            case "state_snapshot":
                let snapshot = try decoder.decode(CompanionStateSnapshot.self, from: data)
                return .snapshot(snapshot)

            case "state_changed":
                let snapshot = try decoder.decode(CompanionStateSnapshot.self, from: data)
                return .stateChanged(snapshot)

            case "telemetry_tick":
                let tick = try decoder.decode(CompanionTelemetryTick.self, from: data)
                return .telemetry(tick)

            case "thumbnail_response":
                let resp = try decoder.decode(CompanionThumbnailResponse.self, from: data)
                return .thumbnail(clipId: resp.clipId, base64: resp.thumbnailBase64)

            case "pong":
                return .pong

            case "error":
                return .serverError(try decoder.decode(CompanionErrorMessage.self, from: data))

            default:
                return .unknown(rawType: type, rawJson: text)
            }
        } catch {
            return .error("JSON Parse failed: \(error.localizedDescription)")
        }
    }

    public static func encodeClientMessage(_ msg: CompanionClientMessage) throws -> String {
        let data = try encoder.encode(msg)
        return String(decoding: data, as: UTF8.self)
    }
}
