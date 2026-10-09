import Foundation

/// The six actions on the Home grid, in the desktop Home page's order.
public enum HomeControl: Sendable, Equatable, CaseIterable {
    case saveReplay
    case saveExtendedReplay
    case replayBuffer
    case session
    case microphone
    case systemAudio
}

public struct ControlTileSpec: Sendable, Equatable {
    public let control: HomeControl
    public let title: String
    public let subtitle: String
    public let tone: TileTone
    public let isEnabled: Bool
    public let isLoading: Bool

    public init(
        control: HomeControl,
        title: String,
        subtitle: String,
        tone: TileTone,
        isEnabled: Bool,
        isLoading: Bool = false
    ) {
        self.control = control
        self.title = title
        self.subtitle = subtitle
        self.tone = tone
        self.isEnabled = isEnabled
        self.isLoading = isLoading
    }
}

/// Wording and state for the Home screen, kept in one place (like the desktop's
/// ReplayControlLabels) so the portrait and landscape layouts always agree.
public enum HomeControlLabels {

    public static func headline(_ snapshot: CompanionStateSnapshot) -> String {
        if snapshot.isSessionRecording {
            return "Session recording"
        } else if snapshot.isRecording {
            return "Replay buffer running"
        } else {
            return "Not recording"
        }
    }

    public static func subheadline(_ snapshot: CompanionStateSnapshot) -> String {
        if !snapshot.isRecording && !snapshot.isSessionRecording {
            return "Start the replay buffer to keep your last \(snapshot.bufferDurationSeconds) seconds ready to save."
        }
        let elapsed = snapshot.isSessionRecording ? snapshot.sessionElapsedSeconds : snapshot.recordingElapsedSeconds
        let display: String?
        if let d = snapshot.capturedDisplayName {
            display = snapshot.isUsingFallbackDisplay ? "\(d) (Fallback)" : d
        } else {
            display = nil
        }
        let parts = [formatDurationSeconds(elapsed), display].compactMap { $0 }
        return parts.joined(separator: " · ")
    }

    /// "Save Last 5m" for whole minutes, otherwise "Save Last 90s".
    public static func extendedReplayTitle(_ seconds: Int) -> String {
        if seconds >= 60 && seconds % 60 == 0 {
            return "Save Last \(seconds / 60)m"
        } else {
            return "Save Last \(seconds)s"
        }
    }

    public static func tiles(_ snapshot: CompanionStateSnapshot) -> [ControlTileSpec] {
        [
            saveReplay(snapshot),
            saveExtendedReplay(snapshot),
            replayBuffer(snapshot),
            session(snapshot),
            audioSource(control: .microphone, title: "Microphone", isEnabled: snapshot.isMicrophoneEnabled, isMuted: snapshot.isMicrophoneMuted),
            audioSource(control: .systemAudio, title: "System Audio", isEnabled: snapshot.isSystemAudioEnabled, isMuted: snapshot.isSystemAudioMuted)
        ]
    }

    public static func iconFor(_ control: HomeControl, snapshot: CompanionStateSnapshot) -> String {
        switch control {
        case .saveReplay:
            return "arrow.down.to.line.compact"
        case .saveExtendedReplay:
            return "clock.arrow.circlepath"
        case .replayBuffer:
            return snapshot.isRecording ? "pause.fill" : "play.fill"
        case .session:
            return snapshot.isSessionRecording ? "stop.fill" : "record.circle"
        case .microphone:
            return (snapshot.isMicrophoneEnabled && !snapshot.isMicrophoneMuted) ? "mic.fill" : "mic.slash.fill"
        case .systemAudio:
            return (snapshot.isSystemAudioEnabled && !snapshot.isSystemAudioMuted) ? "speaker.wave.2.fill" : "speaker.slash.fill"
        }
    }

    private static func saveReplay(_ snapshot: CompanionStateSnapshot) -> ControlTileSpec {
        let subtitle: String
        if snapshot.isSaveInProgress {
            subtitle = "Saving…"
        } else if snapshot.isSaveSuccess {
            subtitle = "Saved"
        } else if snapshot.isSaveFailed {
            subtitle = "Save failed"
        } else if !snapshot.isRecording {
            subtitle = "Buffer stopped"
        } else if snapshot.bufferedSeconds < 1.0 {
            subtitle = "Buffering…"
        } else {
            subtitle = "Ready"
        }

        return ControlTileSpec(
            control: .saveReplay,
            title: "Save Last \(snapshot.bufferDurationSeconds)s",
            subtitle: subtitle,
            tone: .prominent,
            isEnabled: snapshot.canSaveQuickReplay,
            isLoading: snapshot.isSaveInProgress
        )
    }

    private static func saveExtendedReplay(_ snapshot: CompanionStateSnapshot) -> ControlTileSpec {
        let subtitle: String
        if !snapshot.longBufferEnabled {
            subtitle = "Turned off"
        } else if snapshot.isSaveInProgress {
            subtitle = "Saving…"
        } else if !snapshot.isRecording {
            subtitle = "Buffer stopped"
        } else {
            subtitle = "Extended replay"
        }

        return ControlTileSpec(
            control: .saveExtendedReplay,
            title: extendedReplayTitle(snapshot.longBufferDurationSeconds),
            subtitle: subtitle,
            tone: .normal,
            isEnabled: snapshot.canSaveExtendedReplay
        )
    }

    private static func replayBuffer(_ snapshot: CompanionStateSnapshot) -> ControlTileSpec {
        ControlTileSpec(
            control: .replayBuffer,
            title: snapshot.isRecording ? "Stop Buffer" : "Start Buffer",
            subtitle: snapshot.isRecording ? "Running" : "Stopped",
            tone: .normal,
            isEnabled: true
        )
    }

    private static func session(_ snapshot: CompanionStateSnapshot) -> ControlTileSpec {
        ControlTileSpec(
            control: .session,
            title: snapshot.isSessionRecording ? "Stop & Save Session" : "Record Session",
            subtitle: snapshot.isSessionRecording ? formatDurationSeconds(snapshot.sessionElapsedSeconds) : "Full recording",
            tone: snapshot.isSessionRecording ? .live : .normal,
            isEnabled: !snapshot.isSaveInProgress
        )
    }

    private static func audioSource(
        control: HomeControl,
        title: String,
        isEnabled: Bool,
        isMuted: Bool
    ) -> ControlTileSpec {
        let subtitle: String
        let tone: TileTone
        if !isEnabled {
            subtitle = "Off"
            tone = .off
        } else if isMuted {
            subtitle = "Muted"
            tone = .muted
        } else {
            subtitle = "On"
            tone = .normal
        }

        return ControlTileSpec(
            control: control,
            title: title,
            subtitle: subtitle,
            tone: tone,
            isEnabled: true
        )
    }
}
