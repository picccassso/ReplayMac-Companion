import SwiftUI

public struct HomeScreen: View {
    public var connectionState: ConnectionState
    public var snapshot: CompanionStateSnapshot
    public var thumbnails: [String: String]
    public var keepScreenAwake: Bool
    public var onToggleKeepScreenAwake: () -> Void
    public var onDisconnect: () -> Void
    public var onSaveReplay: () -> Void
    public var onSaveExtendedReplay: () -> Void
    public var onToggleBuffer: () -> Void
    public var onToggleSession: () -> Void
    public var onToggleMicrophone: () -> Void
    public var onToggleSystemAudio: () -> Void
    public var onClipSelected: (CompanionClipSummary) -> Void
    public var onOpenTutorial: () -> Void

    public init(
        connectionState: ConnectionState,
        snapshot: CompanionStateSnapshot,
        thumbnails: [String: String],
        keepScreenAwake: Bool,
        onToggleKeepScreenAwake: @escaping () -> Void,
        onDisconnect: @escaping () -> Void,
        onSaveReplay: @escaping () -> Void,
        onSaveExtendedReplay: @escaping () -> Void,
        onToggleBuffer: @escaping () -> Void,
        onToggleSession: @escaping () -> Void,
        onToggleMicrophone: @escaping () -> Void,
        onToggleSystemAudio: @escaping () -> Void,
        onClipSelected: @escaping (CompanionClipSummary) -> Void,
        onOpenTutorial: @escaping () -> Void = {}
    ) {
        self.connectionState = connectionState
        self.snapshot = snapshot
        self.thumbnails = thumbnails
        self.keepScreenAwake = keepScreenAwake
        self.onToggleKeepScreenAwake = onToggleKeepScreenAwake
        self.onDisconnect = onDisconnect
        self.onSaveReplay = onSaveReplay
        self.onSaveExtendedReplay = onSaveExtendedReplay
        self.onToggleBuffer = onToggleBuffer
        self.onToggleSession = onToggleSession
        self.onToggleMicrophone = onToggleMicrophone
        self.onToggleSystemAudio = onToggleSystemAudio
        self.onClipSelected = onClipSelected
        self.onOpenTutorial = onOpenTutorial
    }

    private var serverName: String {
        if case .connected(let sName, _, _, _, _) = connectionState {
            return sName
        }
        return "ReplayMac"
    }

    private func handleControl(_ control: HomeControl) {
        switch control {
        case .saveReplay:
            onSaveReplay()
        case .saveExtendedReplay:
            onSaveExtendedReplay()
        case .replayBuffer:
            onToggleBuffer()
        case .session:
            onToggleSession()
        case .microphone:
            onToggleMicrophone()
        case .systemAudio:
            onToggleSystemAudio()
        }
    }

    public var body: some View {
        GlassBackground {
            GeometryReader { geo in
                let isLandscape = geo.size.width > geo.size.height

                ScrollView {
                    VStack(spacing: 16) {
                        HomeTopBar(
                            serverName: serverName,
                            keepScreenAwake: keepScreenAwake,
                            onToggleKeepScreenAwake: onToggleKeepScreenAwake,
                            onOpenTutorial: onOpenTutorial,
                            onDisconnect: onDisconnect
                        )

                        if isLandscape {
                            HStack(alignment: .top, spacing: 16) {
                                StatusCard(snapshot: snapshot, onToggleBuffer: onToggleBuffer)
                                    .frame(maxWidth: .infinity)

                                ControlGrid(
                                    snapshot: snapshot,
                                    columns: 3,
                                    onControl: handleControl
                                )
                                .frame(maxWidth: .infinity * 1.5)
                            }
                        } else {
                            VStack(spacing: 16) {
                                StatusCard(snapshot: snapshot, onToggleBuffer: onToggleBuffer)

                                ControlGrid(
                                    snapshot: snapshot,
                                    columns: 2,
                                    onControl: handleControl
                                )
                            }
                        }

                        SessionClipsSection(
                            recentClips: snapshot.recentClips,
                            thumbnails: thumbnails,
                            onClipSelected: onClipSelected
                        )
                    }
                    .padding(.horizontal, isLandscape ? 24 : 16)
                    .padding(.top, 8)
                    .padding(.bottom, 24)
                }
            }
        }
    }
}

// MARK: - Top Bar

private struct HomeTopBar: View {
    let serverName: String
    let keepScreenAwake: Bool
    let onToggleKeepScreenAwake: () -> Void
    let onOpenTutorial: () -> Void
    let onDisconnect: () -> Void

    var body: some View {
        HStack(spacing: 12) {
            VStack(alignment: .leading, spacing: 2) {
                Text(serverName)
                    .font(.system(size: 24, weight: .bold))
                    .foregroundColor(AppColors.textPrimary)
                    .lineLimit(1)

                HStack(spacing: 6) {
                    Circle()
                        .fill(AppColors.success)
                        .frame(width: 7, height: 7)

                    Text("Connected")
                        .font(.system(size: 13, weight: .medium))
                        .foregroundColor(AppColors.textSecondary)
                }
            }
            .padding(.leading, 4)

            Spacer()

            GlassButtonGroup {
                GroupIconButton(
                    systemImage: "lightbulb.fill",
                    contentDescription: keepScreenAwake ? "Deck mode on" : "Deck mode off",
                    tint: AppColors.textSecondary,
                    selected: keepScreenAwake,
                    onClick: onToggleKeepScreenAwake
                )

                GroupIconButton(
                    systemImage: "questionmark.circle",
                    contentDescription: "Quick guide",
                    tint: AppColors.textSecondary,
                    selected: false,
                    onClick: onOpenTutorial
                )

                GroupIconButton(
                    systemImage: "power",
                    contentDescription: "Disconnect",
                    tint: AppColors.danger,
                    selected: false,
                    onClick: onDisconnect
                )
            }
        }
    }
}

// MARK: - Status Card

private struct StatusCard: View {
    let snapshot: CompanionStateSnapshot
    let onToggleBuffer: () -> Void

    var body: some View {
        GlassCard(cornerRadius: AppRadii.large) {
            VStack(alignment: .leading, spacing: 16) {
                HStack(spacing: 12) {
                    StatusDot(isLive: snapshot.isRecording || snapshot.isSessionRecording)

                    VStack(alignment: .leading, spacing: 2) {
                        Text(HomeControlLabels.headline(snapshot))
                            .font(.system(size: 16, weight: .semibold))
                            .foregroundColor(AppColors.textPrimary)

                        Text(HomeControlLabels.subheadline(snapshot))
                            .font(.system(size: 13))
                            .foregroundColor(AppColors.textSecondary)
                            .lineLimit(2)
                    }

                    Spacer()

                    BufferToggleButton(isRunning: snapshot.isRecording, onClick: onToggleBuffer)
                }

                if snapshot.isRecording {
                    DualProgressMeters(
                        bufferedSeconds: snapshot.bufferedSeconds,
                        bufferDurationSeconds: snapshot.bufferDurationSeconds,
                        bufferMemoryFormatted: snapshot.bufferMemoryFormatted,
                        longBufferEnabled: snapshot.longBufferEnabled,
                        extendedBufferElapsedSeconds: snapshot.extendedBufferElapsedSeconds,
                        longBufferDurationSeconds: snapshot.longBufferDurationSeconds
                    )
                    .transition(.opacity.combined(with: .move(edge: .top)))
                }
            }
            .padding(18)
            .animation(.easeInOut(duration: 0.25), value: snapshot.isRecording)
        }
    }
}

private struct BufferToggleButton: View {
    let isRunning: Bool
    let onClick: () -> Void

    var body: some View {
        Button(action: onClick) {
            HStack(spacing: 6) {
                Image(systemName: isRunning ? "stop.fill" : "play.fill")
                    .font(.system(size: 14, weight: .bold))
                    .foregroundColor(AppColors.danger)

                if isRunning {
                    Text("Stop Buffer")
                        .font(.system(size: 14, weight: .semibold))
                        .foregroundColor(AppColors.danger)
                }
            }
            .padding(.horizontal, isRunning ? 12 : 10)
            .padding(.vertical, 8)
            .background(AppColors.dangerContainer, in: Capsule())
        }
        .buttonStyle(GlassPressableStyle())
    }
}

// MARK: - Control Grid

private struct ControlGrid: View {
    let snapshot: CompanionStateSnapshot
    let columns: Int
    let onControl: (HomeControl) -> Void

    var body: some View {
        let tiles = HomeControlLabels.tiles(snapshot)
        let gridColumns = Array(repeating: GridItem(.flexible(), spacing: 12), count: columns)

        LazyVGrid(columns: gridColumns, spacing: 12) {
            ForEach(0..<tiles.count, id: \.self) { index in
                let spec = tiles[index]
                ActionTile(
                    title: spec.title,
                    subtitle: spec.subtitle,
                    systemImage: HomeControlLabels.iconFor(spec.control, snapshot: snapshot),
                    tone: spec.tone,
                    isEnabled: spec.isEnabled,
                    isLoading: spec.isLoading,
                    onClick: { onControl(spec.control) }
                )
            }
        }
    }
}

// MARK: - Session Clips Section

private struct SessionClipsSection: View {
    let recentClips: [CompanionClipSummary]
    let thumbnails: [String: String]
    let onClipSelected: (CompanionClipSummary) -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            SectionHeader(title: "This Session") {
                if !recentClips.isEmpty {
                    Text("\(recentClips.count) saved")
                        .font(.system(size: 13, weight: .medium))
                        .foregroundColor(AppColors.textSecondary)
                }
            }

            if recentClips.isEmpty {
                GlassCard(cornerRadius: AppRadii.medium, tint: AppColors.surfaceGlassSubtle) {
                    Text("Clips you save appear here until you quit ReplayMac.")
                        .font(.system(size: 13))
                        .foregroundColor(AppColors.textSecondary)
                        .padding(16)
                        .frame(maxWidth: .infinity, alignment: .leading)
                }
            } else {
                GlassCard(cornerRadius: AppRadii.medium) {
                    VStack(spacing: 0) {
                        ForEach(Array(recentClips.enumerated()), id: \.element.id) { index, clip in
                            RecentClipItem(
                                clip: clip,
                                thumbnailBase64: thumbnails[clip.id],
                                onClick: { onClipSelected(clip) }
                            )

                            if index < recentClips.count - 1 {
                                RowSeparator(startIndent: 122)
                            }
                        }
                    }
                }
            }
        }
        .padding(.top, 4)
    }
}
