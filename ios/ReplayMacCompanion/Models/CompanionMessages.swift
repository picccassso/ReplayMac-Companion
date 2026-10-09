import Foundation

// MARK: - Client Messages (iOS Companion -> macOS Server)

public struct CompanionClientMessage: Codable, Sendable, Equatable {
    public let type: String
    public let deviceId: String?
    public let deviceName: String?
    public let clientVersion: String?
    public let action: String?
    public let clipId: String?

    public init(
        type: String,
        deviceId: String? = nil,
        deviceName: String? = nil,
        clientVersion: String? = nil,
        action: String? = nil,
        clipId: String? = nil
    ) {
        self.type = type
        self.deviceId = deviceId
        self.deviceName = deviceName
        self.clientVersion = clientVersion
        self.action = action
        self.clipId = clipId
    }

    public static func hello(deviceId: String, deviceName: String, clientVersion: String = "1.0.0") -> CompanionClientMessage {
        CompanionClientMessage(
            type: "hello",
            deviceId: deviceId,
            deviceName: deviceName,
            clientVersion: clientVersion
        )
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

    public static func ping() -> CompanionClientMessage {
        CompanionClientMessage(type: "ping")
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

    public init(
        type: String = "auth_response",
        status: String,
        serverName: String = "ReplayCap",
        serverVersion: String = "1.0",
        serverId: String? = nil,
        deviceName: String? = nil
    ) {
        self.type = type
        self.status = status
        self.serverName = serverName
        self.serverVersion = serverVersion
        self.serverId = serverId
        self.deviceName = deviceName
    }

    private enum CodingKeys: String, CodingKey {
        case type, status, serverName, serverVersion, serverId, deviceName
    }

    public init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        self.type = try container.decodeIfPresent(String.self, forKey: .type) ?? "auth_response"
        self.status = try container.decode(String.self, forKey: .status)
        self.serverName = try container.decodeIfPresent(String.self, forKey: .serverName) ?? "ReplayCap"
        self.serverVersion = try container.decodeIfPresent(String.self, forKey: .serverVersion) ?? "1.0"
        self.serverId = try container.decodeIfPresent(String.self, forKey: .serverId)
        self.deviceName = try container.decodeIfPresent(String.self, forKey: .deviceName)
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

    private enum CodingKeys: String, CodingKey {
        case type, isRecording, isSessionRecording, saveStatus
        case recordingElapsedSeconds, extendedBufferElapsedSeconds, sessionElapsedSeconds
        case bufferedSeconds, bufferDurationSeconds, bufferMemoryBytes, bufferMemoryFormatted
        case longBufferEnabled, longBufferDurationSeconds, capturedDisplayName, isUsingFallbackDisplay
        case isMicrophoneEnabled, isMicrophoneMuted, isSystemAudioEnabled, isSystemAudioMuted
        case recentClips
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

    public init(type: String = "error", message: String) {
        self.type = type
        self.message = message
    }
}
