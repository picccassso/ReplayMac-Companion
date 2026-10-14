import Foundation

// MARK: - Client Messages (iOS Companion -> macOS Server)

public struct CompanionClientMessage: Codable, Sendable, Equatable {
    public let type: String
    public let deviceId: String?
    public let deviceName: String?
    public let clientVersion: String?
    public let action: String?
    public let clipId: String?
    public let protocolVersion: Int?
    /// Base64 HMAC answering an `auth_challenge`; nil when this phone holds no secret for the Mac.
    public let proof: String?
    public let bundleId: String?
    public let target: String?
    public let volume: Double?
    public let displayId: String?
    public let profileId: String?

    public init(
        type: String,
        deviceId: String? = nil,
        deviceName: String? = nil,
        clientVersion: String? = nil,
        action: String? = nil,
        clipId: String? = nil,
        protocolVersion: Int? = nil,
        proof: String? = nil,
        bundleId: String? = nil,
        target: String? = nil,
        volume: Double? = nil,
        displayId: String? = nil,
        profileId: String? = nil
    ) {
        self.type = type
        self.deviceId = deviceId
        self.deviceName = deviceName
        self.clientVersion = clientVersion
        self.action = action
        self.clipId = clipId
        self.protocolVersion = protocolVersion
        self.proof = proof
        self.bundleId = bundleId
        self.target = target
        self.volume = volume
        self.displayId = displayId
        self.profileId = profileId
    }

    public static func hello(deviceId: String, deviceName: String, clientVersion: String = CompanionProtocol.clientVersion) -> CompanionClientMessage {
        CompanionClientMessage(
            type: "hello",
            deviceId: deviceId,
            deviceName: deviceName,
            clientVersion: clientVersion,
            protocolVersion: CompanionProtocol.version
        )
    }

    public static func authProof(_ proof: String?) -> CompanionClientMessage {
        CompanionClientMessage(type: "auth_proof", proof: proof)
    }

    public static func action(_ actionName: String) -> CompanionClientMessage {
        CompanionClientMessage(
            type: "action",
            action: actionName
        )
    }

    public static func getThumbnail(_ clipId: String) -> CompanionClientMessage {
        CompanionClientMessage(
            type: "get_thumbnail",
            action: "get_thumbnail",
            clipId: clipId
        )
    }

    public static func revealClip(_ clipId: String) -> CompanionClientMessage {
        CompanionClientMessage(
            type: "reveal_clip",
            action: "reveal_clip",
            clipId: clipId
        )
    }

    public static func deleteClip(_ clipId: String) -> CompanionClientMessage {
        CompanionClientMessage(
            type: "delete_clip",
            action: "delete_clip",
            clipId: clipId
        )
    }

    public static func trimClip(_ clipId: String) -> CompanionClientMessage {
        CompanionClientMessage(
            type: "trim_clip",
            action: "trim_clip",
            clipId: clipId
        )
    }

    public static func copyClip(_ clipId: String) -> CompanionClientMessage {
        CompanionClientMessage(
            type: "copy_clip",
            action: "copy_clip",
            clipId: clipId
        )
    }

    public static func setVolume(target: String, volume: Double) -> CompanionClientMessage {
        CompanionClientMessage(
            type: "set_volume",
            action: "set_volume",
            target: target,
            volume: min(max(volume, 0.0), 1.0)
        )
    }

    public static func toggleAppMute(_ bundleId: String) -> CompanionClientMessage {
        CompanionClientMessage(
            type: "toggle_app_mute",
            action: "toggle_app_mute",
            bundleId: bundleId
        )
    }

    public static func unmuteAllApps() -> CompanionClientMessage {
        CompanionClientMessage(
            type: "unmute_all_apps",
            action: "unmute_all_apps"
        )
    }

    public static func selectDisplay(_ displayId: String) -> CompanionClientMessage {
        CompanionClientMessage(
            type: "select_display",
            action: "select_display",
            displayId: displayId
        )
    }

    public static func applyProfile(_ profileId: String) -> CompanionClientMessage {
        CompanionClientMessage(
            type: "apply_profile",
            action: "apply_profile",
            profileId: profileId
        )
    }

    public static func ping() -> CompanionClientMessage {
        CompanionClientMessage(type: "ping")
    }
}

// MARK: - Protocol

public enum CompanionProtocol {
    /// ReplayCap refuses phones that send an older version in `hello`.
    public static let version = 2

    public static var clientVersion: String {
        Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String ?? "1.0.0"
    }
}

// MARK: - Action Names

public enum CompanionActions {
    public static let saveReplay = "saveReplay"
    public static let saveExtendedReplay = "saveExtendedReplay"
    public static let toggleBuffer = "toggleBuffer"
    public static let toggleSession = "toggleSession"
    public static let toggleMicrophone = "toggleMicrophone"
    public static let toggleSystemAudio = "toggleSystemAudio"
}

// MARK: - Server Response Models

public struct CompanionAuthResponse: Codable, Sendable, Equatable {
    public let type: String
    public let status: String // "approved", "pending", "denied"
    public let serverName: String
    public let serverVersion: String
    public let serverId: String?
    public let deviceName: String?
    /// With "pending": the code ReplayCap's Allow prompt shows.
    public let pairingCode: String?
    /// With "approved", only right after the user clicks Allow: the secret to keep for this Mac.
    public let pairingSecret: String?
    /// With "denied": "user_denied", "revoked" or "invalid_proof".
    public let reason: String?

    public init(
        type: String = "auth_response",
        status: String,
        serverName: String = "ReplayCap",
        serverVersion: String = "1.0",
        serverId: String? = nil,
        deviceName: String? = nil,
        pairingCode: String? = nil,
        pairingSecret: String? = nil,
        reason: String? = nil
    ) {
        self.type = type
        self.status = status
        self.serverName = serverName
        self.serverVersion = serverVersion
        self.serverId = serverId
        self.deviceName = deviceName
        self.pairingCode = pairingCode
        self.pairingSecret = pairingSecret
        self.reason = reason
    }

    private enum CodingKeys: String, CodingKey {
        case type, status, serverName, serverVersion, serverId, deviceName, pairingCode, pairingSecret, reason
    }

    public init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        self.type = try container.decodeIfPresent(String.self, forKey: .type) ?? "auth_response"
        self.status = try container.decode(String.self, forKey: .status)
        self.serverName = try container.decodeIfPresent(String.self, forKey: .serverName) ?? "ReplayCap"
        self.serverVersion = try container.decodeIfPresent(String.self, forKey: .serverVersion) ?? "1.0"
        self.serverId = try container.decodeIfPresent(String.self, forKey: .serverId)
        self.deviceName = try container.decodeIfPresent(String.self, forKey: .deviceName)
        self.pairingCode = try container.decodeIfPresent(String.self, forKey: .pairingCode)
        self.pairingSecret = try container.decodeIfPresent(String.self, forKey: .pairingSecret)
        self.reason = try container.decodeIfPresent(String.self, forKey: .reason)
    }
}

/// Sent by a Mac this phone is paired with; answered with `auth_proof`.
public struct CompanionAuthChallenge: Codable, Sendable, Equatable {
    public let type: String
    public let nonce: String
    public let serverId: String
    public let serverName: String
    public let serverVersion: String

    public init(type: String = "auth_challenge", nonce: String, serverId: String, serverName: String = "ReplayCap", serverVersion: String = "1.0") {
        self.type = type
        self.nonce = nonce
        self.serverId = serverId
        self.serverName = serverName
        self.serverVersion = serverVersion
    }
}

public struct CompanionClipSummary: Codable, Sendable, Identifiable, Equatable {
    public let id: String
    public let filename: String
    public let kind: String
    public let savedAt: Double
    public let duration: Double?
    public let fileSize: Int64?
    public let hasThumbnail: Bool

    public init(
        id: String,
        filename: String,
        kind: String = "replay",
        savedAt: Double = 0.0,
        duration: Double? = nil,
        fileSize: Int64? = nil,
        hasThumbnail: Bool = false
    ) {
        self.id = id
        self.filename = filename
        self.kind = kind
        self.savedAt = savedAt
        self.duration = duration
        self.fileSize = fileSize
        self.hasThumbnail = hasThumbnail
    }

    private enum CodingKeys: String, CodingKey {
        case id, filename, kind, savedAt, duration, fileSize, hasThumbnail
    }

    public init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        self.id = try container.decode(String.self, forKey: .id)
        self.filename = try container.decode(String.self, forKey: .filename)
        self.kind = try container.decodeIfPresent(String.self, forKey: .kind) ?? "replay"
        self.savedAt = try container.decodeIfPresent(Double.self, forKey: .savedAt) ?? 0.0
        self.duration = try container.decodeIfPresent(Double.self, forKey: .duration)
        self.fileSize = try container.decodeIfPresent(Int64.self, forKey: .fileSize)
        self.hasThumbnail = try container.decodeIfPresent(Bool.self, forKey: .hasThumbnail) ?? false
    }
}

public struct CompanionAudioApp: Codable, Sendable, Identifiable, Equatable {
    public let bundleId: String
    public let name: String
    public let isMuted: Bool
    public let isRunning: Bool
    public let iconBase64: String?

    public var id: String { bundleId }

    public init(
        bundleId: String,
        name: String,
        isMuted: Bool = false,
        isRunning: Bool = true,
        iconBase64: String? = nil
    ) {
        self.bundleId = bundleId
        self.name = name
        self.isMuted = isMuted
        self.isRunning = isRunning
        self.iconBase64 = iconBase64
    }

    private enum CodingKeys: String, CodingKey {
        case bundleId, name, isMuted, isRunning, iconBase64
    }

    public init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        self.bundleId = try container.decode(String.self, forKey: .bundleId)
        self.name = try container.decodeIfPresent(String.self, forKey: .name) ?? self.bundleId
        self.isMuted = try container.decodeIfPresent(Bool.self, forKey: .isMuted) ?? false
        self.isRunning = try container.decodeIfPresent(Bool.self, forKey: .isRunning) ?? true
        self.iconBase64 = try container.decodeIfPresent(String.self, forKey: .iconBase64)
    }
}

public struct CompanionDisplayInfo: Codable, Sendable, Identifiable, Equatable {
    public let id: String
    public let name: String
    public let isActive: Bool
    public let isConnected: Bool

    public init(
        id: String,
        name: String,
        isActive: Bool = false,
        isConnected: Bool = true
    ) {
        self.id = id
        self.name = name
        self.isActive = isActive
        self.isConnected = isConnected
    }

    private enum CodingKeys: String, CodingKey {
        case id, name, isActive, isConnected
    }

    public init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        self.id = try container.decode(String.self, forKey: .id)
        self.name = try container.decodeIfPresent(String.self, forKey: .name) ?? self.id
        self.isActive = try container.decodeIfPresent(Bool.self, forKey: .isActive) ?? false
        self.isConnected = try container.decodeIfPresent(Bool.self, forKey: .isConnected) ?? true
    }
}

public struct CompanionProfileSummary: Codable, Sendable, Identifiable, Equatable {
    public let id: String
    public let name: String
    public let summary: String
    public let isActive: Bool

    public init(
        id: String,
        name: String,
        summary: String = "",
        isActive: Bool = false
    ) {
        self.id = id
        self.name = name
        self.summary = summary
        self.isActive = isActive
    }

    private enum CodingKeys: String, CodingKey {
        case id, name, summary, isActive
    }

    public init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        self.id = try container.decode(String.self, forKey: .id)
        self.name = try container.decodeIfPresent(String.self, forKey: .name) ?? "Profile"
        self.summary = try container.decodeIfPresent(String.self, forKey: .summary) ?? ""
        self.isActive = try container.decodeIfPresent(Bool.self, forKey: .isActive) ?? false
    }
}

public struct CompanionStateSnapshot: Codable, Sendable, Equatable {
    public let type: String
    public let isRecording: Bool
    public let isSessionRecording: Bool
    public let saveStatus: String
    public let recordingElapsedSeconds: Double
    public let extendedBufferElapsedSeconds: Double
    public let sessionElapsedSeconds: Double
    public let bufferedSeconds: Double
    public let bufferDurationSeconds: Int
    public let bufferMemoryBytes: Int64
    public let bufferMemoryFormatted: String
    public let longBufferEnabled: Bool
    public let longBufferDurationSeconds: Int
    public let capturedDisplayName: String?
    public let isUsingFallbackDisplay: Bool
    public let isMicrophoneEnabled: Bool
    public let isMicrophoneMuted: Bool
    public let isSystemAudioEnabled: Bool
    public let isSystemAudioMuted: Bool
    public let microphoneVolume: Double
    public let systemAudioVolume: Double
    public let perAppAudioEnabled: Bool
    public let audioApps: [CompanionAudioApp]
    public let displays: [CompanionDisplayInfo]
    public let profiles: [CompanionProfileSummary]
    public let recentClips: [CompanionClipSummary]

    public init(
        type: String = "state_snapshot",
        isRecording: Bool = false,
        isSessionRecording: Bool = false,
        saveStatus: String = "idle",
        recordingElapsedSeconds: Double = 0.0,
        extendedBufferElapsedSeconds: Double = 0.0,
        sessionElapsedSeconds: Double = 0.0,
        bufferedSeconds: Double = 0.0,
        bufferDurationSeconds: Int = 30,
        bufferMemoryBytes: Int64 = 0,
        bufferMemoryFormatted: String = "0 MB",
        longBufferEnabled: Bool = false,
        longBufferDurationSeconds: Int = 300,
        capturedDisplayName: String? = nil,
        isUsingFallbackDisplay: Bool = false,
        isMicrophoneEnabled: Bool = false,
        isMicrophoneMuted: Bool = false,
        isSystemAudioEnabled: Bool = true,
        isSystemAudioMuted: Bool = false,
        microphoneVolume: Double = 1.0,
        systemAudioVolume: Double = 1.0,
        perAppAudioEnabled: Bool = false,
        audioApps: [CompanionAudioApp] = [],
        displays: [CompanionDisplayInfo] = [],
        profiles: [CompanionProfileSummary] = [],
        recentClips: [CompanionClipSummary] = []
    ) {
        self.type = type
        self.isRecording = isRecording
        self.isSessionRecording = isSessionRecording
        self.saveStatus = saveStatus
        self.recordingElapsedSeconds = recordingElapsedSeconds
        self.extendedBufferElapsedSeconds = extendedBufferElapsedSeconds
        self.sessionElapsedSeconds = sessionElapsedSeconds
        self.bufferedSeconds = bufferedSeconds
        self.bufferDurationSeconds = bufferDurationSeconds
        self.bufferMemoryBytes = bufferMemoryBytes
        self.bufferMemoryFormatted = bufferMemoryFormatted
        self.longBufferEnabled = longBufferEnabled
        self.longBufferDurationSeconds = longBufferDurationSeconds
        self.capturedDisplayName = capturedDisplayName
        self.isUsingFallbackDisplay = isUsingFallbackDisplay
        self.isMicrophoneEnabled = isMicrophoneEnabled
        self.isMicrophoneMuted = isMicrophoneMuted
        self.isSystemAudioEnabled = isSystemAudioEnabled
        self.isSystemAudioMuted = isSystemAudioMuted
        self.microphoneVolume = microphoneVolume
        self.systemAudioVolume = systemAudioVolume
        self.perAppAudioEnabled = perAppAudioEnabled
        self.audioApps = audioApps
        self.displays = displays
        self.profiles = profiles
        self.recentClips = recentClips
    }

    public static var empty: CompanionStateSnapshot {
        CompanionStateSnapshot()
    }

    public var isSaveInProgress: Bool {
        saveStatus.caseInsensitiveCompare("saving") == .orderedSame ||
        saveStatus.caseInsensitiveCompare("busy") == .orderedSame
    }

    public var isSaveSuccess: Bool {
        saveStatus.caseInsensitiveCompare("saved") == .orderedSame
    }

    public var isSaveFailed: Bool {
        saveStatus.caseInsensitiveCompare("failed") == .orderedSame
    }

    public var canSaveQuickReplay: Bool {
        isRecording && bufferedSeconds >= 1.0 && !isSaveInProgress
    }

    public var canSaveExtendedReplay: Bool {
        isRecording && longBufferEnabled && !isSaveInProgress
    }

    public var mutedAudioAppsCount: Int {
        guard isSystemAudioEnabled && !perAppAudioEnabled else { return 0 }
        return audioApps.filter(\.isMuted).count
    }

    public var activeDisplayName: String? {
        displays.first(where: \.isActive)?.name ?? capturedDisplayName
    }

    public var activeProfileName: String? {
        profiles.first(where: \.isActive)?.name
    }

    public var hasCaptureSetupOptions: Bool {
        !displays.isEmpty || !profiles.isEmpty
    }

    private enum CodingKeys: String, CodingKey {
        case type, isRecording, isSessionRecording, saveStatus
        case recordingElapsedSeconds, extendedBufferElapsedSeconds, sessionElapsedSeconds
        case bufferedSeconds, bufferDurationSeconds, bufferMemoryBytes, bufferMemoryFormatted
        case longBufferEnabled, longBufferDurationSeconds, capturedDisplayName, isUsingFallbackDisplay
        case isMicrophoneEnabled, isMicrophoneMuted, isSystemAudioEnabled, isSystemAudioMuted
        case microphoneVolume, systemAudioVolume, perAppAudioEnabled
        case audioApps, displays, profiles, recentClips
    }

    public init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        self.type = try container.decodeIfPresent(String.self, forKey: .type) ?? "state_snapshot"
        self.isRecording = try container.decodeIfPresent(Bool.self, forKey: .isRecording) ?? false
        self.isSessionRecording = try container.decodeIfPresent(Bool.self, forKey: .isSessionRecording) ?? false
        self.saveStatus = try container.decodeIfPresent(String.self, forKey: .saveStatus) ?? "idle"
        self.recordingElapsedSeconds = try container.decodeIfPresent(Double.self, forKey: .recordingElapsedSeconds) ?? 0.0
        self.extendedBufferElapsedSeconds = try container.decodeIfPresent(Double.self, forKey: .extendedBufferElapsedSeconds) ?? 0.0
        self.sessionElapsedSeconds = try container.decodeIfPresent(Double.self, forKey: .sessionElapsedSeconds) ?? 0.0
        self.bufferedSeconds = try container.decodeIfPresent(Double.self, forKey: .bufferedSeconds) ?? 0.0
        self.bufferDurationSeconds = try container.decodeIfPresent(Int.self, forKey: .bufferDurationSeconds) ?? 30
        self.bufferMemoryBytes = try container.decodeIfPresent(Int64.self, forKey: .bufferMemoryBytes) ?? 0
        self.bufferMemoryFormatted = try container.decodeIfPresent(String.self, forKey: .bufferMemoryFormatted) ?? "0 MB"
        self.longBufferEnabled = try container.decodeIfPresent(Bool.self, forKey: .longBufferEnabled) ?? false
        self.longBufferDurationSeconds = try container.decodeIfPresent(Int.self, forKey: .longBufferDurationSeconds) ?? 300
        self.capturedDisplayName = try container.decodeIfPresent(String.self, forKey: .capturedDisplayName)
        self.isUsingFallbackDisplay = try container.decodeIfPresent(Bool.self, forKey: .isUsingFallbackDisplay) ?? false
        self.isMicrophoneEnabled = try container.decodeIfPresent(Bool.self, forKey: .isMicrophoneEnabled) ?? false
        self.isMicrophoneMuted = try container.decodeIfPresent(Bool.self, forKey: .isMicrophoneMuted) ?? false
        self.isSystemAudioEnabled = try container.decodeIfPresent(Bool.self, forKey: .isSystemAudioEnabled) ?? true
        self.isSystemAudioMuted = try container.decodeIfPresent(Bool.self, forKey: .isSystemAudioMuted) ?? false
        self.microphoneVolume = try container.decodeIfPresent(Double.self, forKey: .microphoneVolume) ?? 1.0
        self.systemAudioVolume = try container.decodeIfPresent(Double.self, forKey: .systemAudioVolume) ?? 1.0
        self.perAppAudioEnabled = try container.decodeIfPresent(Bool.self, forKey: .perAppAudioEnabled) ?? false
        self.audioApps = try container.decodeIfPresent([CompanionAudioApp].self, forKey: .audioApps) ?? []
        self.displays = try container.decodeIfPresent([CompanionDisplayInfo].self, forKey: .displays) ?? []
        self.profiles = try container.decodeIfPresent([CompanionProfileSummary].self, forKey: .profiles) ?? []
        self.recentClips = try container.decodeIfPresent([CompanionClipSummary].self, forKey: .recentClips) ?? []
    }
}

public struct CompanionTelemetryTick: Codable, Sendable, Equatable {
    public let type: String
    public let recordingElapsedSeconds: Double
    public let extendedBufferElapsedSeconds: Double
    public let sessionElapsedSeconds: Double
    public let bufferedSeconds: Double
    public let bufferMemoryBytes: Int64
    public let bufferMemoryFormatted: String

    public init(
        type: String = "telemetry_tick",
        recordingElapsedSeconds: Double = 0.0,
        extendedBufferElapsedSeconds: Double = 0.0,
        sessionElapsedSeconds: Double = 0.0,
        bufferedSeconds: Double = 0.0,
        bufferMemoryBytes: Int64 = 0,
        bufferMemoryFormatted: String = "0 MB"
    ) {
        self.type = type
        self.recordingElapsedSeconds = recordingElapsedSeconds
        self.extendedBufferElapsedSeconds = extendedBufferElapsedSeconds
        self.sessionElapsedSeconds = sessionElapsedSeconds
        self.bufferedSeconds = bufferedSeconds
        self.bufferMemoryBytes = bufferMemoryBytes
        self.bufferMemoryFormatted = bufferMemoryFormatted
    }

    private enum CodingKeys: String, CodingKey {
        case type, recordingElapsedSeconds, extendedBufferElapsedSeconds, sessionElapsedSeconds
        case bufferedSeconds, bufferMemoryBytes, bufferMemoryFormatted
    }

    public init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        self.type = try container.decodeIfPresent(String.self, forKey: .type) ?? "telemetry_tick"
        self.recordingElapsedSeconds = try container.decodeIfPresent(Double.self, forKey: .recordingElapsedSeconds) ?? 0.0
        self.extendedBufferElapsedSeconds = try container.decodeIfPresent(Double.self, forKey: .extendedBufferElapsedSeconds) ?? 0.0
        self.sessionElapsedSeconds = try container.decodeIfPresent(Double.self, forKey: .sessionElapsedSeconds) ?? 0.0
        self.bufferedSeconds = try container.decodeIfPresent(Double.self, forKey: .bufferedSeconds) ?? 0.0
        self.bufferMemoryBytes = try container.decodeIfPresent(Int64.self, forKey: .bufferMemoryBytes) ?? 0
        self.bufferMemoryFormatted = try container.decodeIfPresent(String.self, forKey: .bufferMemoryFormatted) ?? "0 MB"
    }
}

public struct CompanionThumbnailResponse: Codable, Sendable, Equatable {
    public let type: String
    public let clipId: String
    public let thumbnailBase64: String?

    public init(
        type: String = "thumbnail_response",
        clipId: String,
        thumbnailBase64: String? = nil
    ) {
        self.type = type
        self.clipId = clipId
        self.thumbnailBase64 = thumbnailBase64
    }
}

public struct CompanionPongMessage: Codable, Sendable, Equatable {
    public let type: String

    public init(type: String = "pong") {
        self.type = type
    }
}

public struct CompanionErrorMessage: Codable, Sendable, Equatable {
    public let type: String
    public let message: String
    /// Every code except "not_authorized" means ReplayCap is about to close the connection.
    public let code: String?

    public init(type: String = "error", message: String, code: String? = nil) {
        self.type = type
        self.message = message
        self.code = code
    }

    /// Whether this error explains why the connection is about to close.
    public var endsConnection: Bool {
        guard let code = code else { return false }
        return code != "not_authorized"
    }
}
