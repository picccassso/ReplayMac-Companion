import Foundation

/// The kinds of clip the demo Mac saves, named as ReplayCap names them.
public enum DemoClipKind: String, Sendable {
    case replay = "replay"
    case extendedReplay = "extended replay"
    case session = "session"

    var filenameTitle: String {
        switch self {
        case .replay: return "Replay"
        case .extendedReplay: return "Extended Replay"
        case .session: return "Session"
        }
    }
}

/// The fake Mac behind demo mode: everything ReplayCap would report, and how
/// each command changes it. Pure, so the rules can be tested without timers.
public struct DemoCompanionState: Equatable, Sendable {
    public static let maxClips = 10
    static let bufferBytesPerSecond = 13_700_000.0
    static let clipBytesPerSecond = 1_600_000.0

    public var isRecording = true
    public var isSessionRecording = false
    public var saveStatus = "idle"
    public var recordingElapsedSeconds: Double = 754
    public var extendedBufferElapsedSeconds: Double = 300
    public var sessionElapsedSeconds: Double = 0
    public var bufferedSeconds: Double = 30
    public var bufferDurationSeconds = 30
    public var longBufferEnabled = true
    public var longBufferDurationSeconds = 300
    public var isMicrophoneEnabled = true
    public var isMicrophoneMuted = false
    public var microphoneVolume = 0.8
    public var isSystemAudioEnabled = true
    public var isSystemAudioMuted = false
    public var systemAudioVolume = 1.0
    public var audioApps: [CompanionAudioApp]
    public var displays: [CompanionDisplayInfo]
    public var profiles: [CompanionProfileSummary]
    public var clips: [CompanionClipSummary]

    public private(set) var lastCopiedClipId: String?
    public private(set) var lastRevealedClipId: String?
    private var nextClipNumber: Int
    /// The save the "saving" status stands for, until it finishes.
    private var pendingSave: DemoClipKind?
    /// Clip filenames carry the wall-clock time, as ReplayCap's do.
    private let timeZone: TimeZone

    /// The demo Mac as it first appears: buffer full, a few clips already saved.
    public static func initial(now: Double, timeZone: TimeZone = .current) -> DemoCompanionState {
        DemoCompanionState(now: now, timeZone: timeZone)
    }

    private init(now: Double, timeZone: TimeZone) {
        self.timeZone = timeZone
        audioApps = [
            CompanionAudioApp(bundleId: "com.apple.Safari", name: "Safari"),
            CompanionAudioApp(bundleId: "com.apple.Music", name: "Music"),
            CompanionAudioApp(bundleId: "com.hnc.Discord", name: "Discord", isMuted: true),
            CompanionAudioApp(bundleId: "com.tinyspeck.slackmacgap", name: "Slack")
        ]
        displays = [
            CompanionDisplayInfo(id: "demo-display-studio", name: "Studio Display", isActive: true),
            CompanionDisplayInfo(id: "demo-display-builtin", name: "Built-in Retina Display")
        ]
        profiles = [
            CompanionProfileSummary(id: "demo-profile-gaming", name: "Gaming", summary: "1080p · 60 fps · System audio + mic", isActive: true),
            CompanionProfileSummary(id: "demo-profile-tutorials", name: "Tutorials", summary: "1440p · 30 fps · Mic only"),
            CompanionProfileSummary(id: "demo-profile-battery", name: "Battery Saver", summary: "720p · 30 fps")
        ]
        clips = [
            Self.clip(id: "demo-clip-1", kind: .replay, duration: 30, fileSize: 48_000_000, savedAt: now - 300, timeZone: timeZone),
            Self.clip(id: "demo-clip-2", kind: .session, duration: 754, fileSize: 1_210_000_000, savedAt: now - 3_600, timeZone: timeZone),
            Self.clip(id: "demo-clip-3", kind: .extendedReplay, duration: 300, fileSize: 482_000_000, savedAt: now - 86_400, timeZone: timeZone)
        ]
        nextClipNumber = 4
    }

    // MARK: - Snapshot

    public var bufferMemoryBytes: Int64 {
        Int64(bufferedSeconds * Self.bufferBytesPerSecond)
    }

    public var snapshot: CompanionStateSnapshot {
        let bytes = bufferMemoryBytes
        return CompanionStateSnapshot(
            type: "state_changed",
            isRecording: isRecording,
            isSessionRecording: isSessionRecording,
            saveStatus: saveStatus,
            recordingElapsedSeconds: recordingElapsedSeconds,
            extendedBufferElapsedSeconds: extendedBufferElapsedSeconds,
            sessionElapsedSeconds: sessionElapsedSeconds,
            bufferedSeconds: bufferedSeconds,
            bufferDurationSeconds: bufferDurationSeconds,
            bufferMemoryBytes: bytes,
            bufferMemoryFormatted: Self.formatMemory(bytes),
            longBufferEnabled: longBufferEnabled,
            longBufferDurationSeconds: longBufferDurationSeconds,
            capturedDisplayName: displays.first(where: \.isActive)?.name,
            isUsingFallbackDisplay: false,
            isMicrophoneEnabled: isMicrophoneEnabled,
            isMicrophoneMuted: isMicrophoneMuted,
            isSystemAudioEnabled: isSystemAudioEnabled,
            isSystemAudioMuted: isSystemAudioMuted,
            microphoneVolume: microphoneVolume,
            systemAudioVolume: systemAudioVolume,
            perAppAudioEnabled: false,
            audioApps: audioApps,
            displays: displays,
            profiles: profiles,
            recentClips: clips
        )
    }

    static func formatMemory(_ bytes: Int64) -> String {
        "\(Int((Double(bytes) / 1_000_000).rounded())) MB"
    }

    // MARK: - Time

    public mutating func tick(seconds: Double) {
        guard seconds > 0 else { return }
        if isRecording {
            recordingElapsedSeconds += seconds
            bufferedSeconds = min(bufferedSeconds + seconds, Double(bufferDurationSeconds))
            extendedBufferElapsedSeconds = min(extendedBufferElapsedSeconds + seconds, Double(longBufferDurationSeconds))
        }
        if isSessionRecording {
            sessionElapsedSeconds += seconds
        }
    }

    // MARK: - Recording

    public mutating func toggleBuffer() {
        isRecording.toggle()
        // A stopped buffer drops what it held; a started one fills from empty.
        recordingElapsedSeconds = 0
        bufferedSeconds = 0
        extendedBufferElapsedSeconds = 0
    }

    public mutating func toggleSession(now: Double) {
        if isSessionRecording {
            if sessionElapsedSeconds >= 1 {
                addClip(.session, duration: sessionElapsedSeconds, savedAt: now)
            }
            isSessionRecording = false
        } else {
            isSessionRecording = true
        }
        sessionElapsedSeconds = 0
    }

    // MARK: - Saving

    public var isSaving: Bool { pendingSave != nil }

    /// Starts saving the buffer, or shows "failed" when there is nothing to save.
    public mutating func saveReplay() {
        requestSave(.replay, allowed: snapshot.canSaveQuickReplay)
    }

    public mutating func saveExtendedReplay() {
        requestSave(.extendedReplay, allowed: snapshot.canSaveExtendedReplay)
    }

    private mutating func requestSave(_ kind: DemoClipKind, allowed: Bool) {
        guard !isSaving else { return }
        if allowed {
            pendingSave = kind
            saveStatus = "saving"
        } else {
            saveStatus = "failed"
        }
    }

    /// Writes the clip the pending save stands for.
    public mutating func finishSave(now: Double) {
        guard let kind = pendingSave else { return }
        pendingSave = nil
        let duration = kind == .extendedReplay
            ? min(extendedBufferElapsedSeconds, Double(longBufferDurationSeconds))
            : bufferedSeconds
        addClip(kind, duration: duration, savedAt: now)
        saveStatus = "saved"
    }

    /// Clears a "saved" or "failed" status once it has been shown.
    public mutating func resetSaveStatus() {
        guard !isSaving else { return }
        saveStatus = "idle"
    }

    // MARK: - Audio

    public mutating func toggleMicrophone() {
        isMicrophoneMuted.toggle()
    }

    public mutating func toggleSystemAudio() {
        isSystemAudioMuted.toggle()
    }

    public mutating func setVolume(target: String, volume: Double) {
        let clamped = min(max(volume, 0), 1)
        switch target {
        case "microphone": microphoneVolume = clamped
        case "systemAudio": systemAudioVolume = clamped
        default: break
        }
    }

    public mutating func toggleAppMute(_ bundleId: String) {
        audioApps = audioApps.map { app in
            app.bundleId == bundleId ? app.withMuted(!app.isMuted) : app
        }
    }

    public mutating func unmuteAllApps() {
        audioApps = audioApps.map { $0.withMuted(false) }
    }

    // MARK: - Capture setup

    public mutating func selectDisplay(_ displayId: String) {
        guard displays.contains(where: { $0.id == displayId }) else { return }
        displays = displays.map {
            CompanionDisplayInfo(id: $0.id, name: $0.name, isActive: $0.id == displayId, isConnected: $0.isConnected)
        }
    }

    public mutating func applyProfile(_ profileId: String) {
        guard profiles.contains(where: { $0.id == profileId }) else { return }
        profiles = profiles.map {
            CompanionProfileSummary(id: $0.id, name: $0.name, summary: $0.summary, isActive: $0.id == profileId)
        }
    }

    // MARK: - Clips

    public mutating func deleteClip(_ clipId: String) {
        clips.removeAll { $0.id == clipId }
    }

    /// Stands in for saving a trim in ReplayCap's editor: a shorter copy appears on top.
    public mutating func trimClip(_ clipId: String, now: Double) {
        guard let clip = clips.first(where: { $0.id == clipId }) else { return }
        let base = clip.filename.hasSuffix(".mp4") ? String(clip.filename.dropLast(4)) : clip.filename
        insert(CompanionClipSummary(
            id: takeClipId(),
            filename: "\(base) (Trimmed).mp4",
            kind: clip.kind,
            savedAt: now,
            duration: max(1, ((clip.duration ?? 0) / 2).rounded()),
            fileSize: (clip.fileSize ?? 0) / 2,
            hasThumbnail: true
        ))
    }

    /// The real Mac copies the file to its clipboard; nothing on the phone changes.
    public mutating func copyClip(_ clipId: String) {
        guard clips.contains(where: { $0.id == clipId }) else { return }
        lastCopiedClipId = clipId
    }

    /// The real Mac shows the file in Finder; nothing on the phone changes.
    public mutating func revealClip(_ clipId: String) {
        guard clips.contains(where: { $0.id == clipId }) else { return }
        lastRevealedClipId = clipId
    }

    private mutating func addClip(_ kind: DemoClipKind, duration: Double, savedAt: Double) {
        let fileSize = Int64(duration * Self.clipBytesPerSecond)
        insert(Self.clip(id: takeClipId(), kind: kind, duration: duration, fileSize: fileSize, savedAt: savedAt, timeZone: timeZone))
    }

    private mutating func takeClipId() -> String {
        defer { nextClipNumber += 1 }
        return "demo-clip-\(nextClipNumber)"
    }

    private mutating func insert(_ clip: CompanionClipSummary) {
        clips.insert(clip, at: 0)
        if clips.count > Self.maxClips {
            clips.removeLast(clips.count - Self.maxClips)
        }
    }

    private static func clip(
        id: String,
        kind: DemoClipKind,
        duration: Double,
        fileSize: Int64,
        savedAt: Double,
        timeZone: TimeZone
    ) -> CompanionClipSummary {
        CompanionClipSummary(
            id: id,
            filename: "\(kind.filenameTitle) \(timestamp(savedAt, timeZone: timeZone)).mp4",
            kind: kind.rawValue,
            savedAt: savedAt,
            duration: duration,
            fileSize: fileSize,
            hasThumbnail: true
        )
    }

    /// "2026-10-17 at 14.32.05", the way ReplayCap names its files.
    static func timestamp(_ epochSeconds: Double, timeZone: TimeZone) -> String {
        let formatter = DateFormatter()
        formatter.locale = Locale(identifier: "en_US_POSIX")
        formatter.timeZone = timeZone
        formatter.dateFormat = "yyyy-MM-dd 'at' HH.mm.ss"
        return formatter.string(from: Date(timeIntervalSince1970: epochSeconds))
    }
}

private extension CompanionAudioApp {
    func withMuted(_ muted: Bool) -> CompanionAudioApp {
        CompanionAudioApp(bundleId: bundleId, name: name, isMuted: muted, isRunning: isRunning, iconBase64: iconBase64)
    }
}
