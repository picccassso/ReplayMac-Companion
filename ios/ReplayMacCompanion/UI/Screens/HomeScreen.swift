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
        if case .connected(let sName, _, _, _, _, _) = connectionState {
            return sName
        }
        return "ReplayMac"
    }

    private var deviceName: String? {
        if case .connected(_, _, _, _, _, let dName) = connectionState {
            return dName
        }
        return nil
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
                let isCompactLandscape = isLandscape && (geo.size.height < 500 || geo.size.width < 960)
                let isCompact = isLandscape ? isCompactLandscape : (geo.size.width < 430)
                let horizontalPadding: CGFloat = isLandscape ? 24 : 16

                ScrollView {
                    VStack(spacing: 16) {
                        HomeTopBar(
                            serverName: serverName,
                            deviceName: deviceName,
                            keepScreenAwake: keepScreenAwake,
                            onToggleKeepScreenAwake: onToggleKeepScreenAwake,
                            onOpenTutorial: onOpenTutorial,
                            onDisconnect: onDisconnect
                        )

                        if isLandscape {
                            let availableWidth = max(0, geo.size.width - (horizontalPadding * 2) - 16)
                            let statusWidth: CGFloat = isCompactLandscape
                                ? max(270, min(340, availableWidth * 0.43))
                                : max(300, min(420, availableWidth * 0.38))

                            HStack(alignment: .top, spacing: 16) {
                                StatusCard(snapshot: snapshot, onToggleBuffer: onToggleBuffer)
                                    .frame(width: statusWidth)

                                ControlGrid(
                                    snapshot: snapshot,
                                    columns: isCompactLandscape ? 2 : 3,
                                    isCompact: isCompact,
                                    onControl: handleControl
                                )
                                .frame(maxWidth: .infinity)
                            }
                        } else {
                            VStack(spacing: 16) {
                                StatusCard(snapshot: snapshot, onToggleBuffer: onToggleBuffer)

                                ControlGrid(
                                    snapshot: snapshot,
                                    columns: 2,
                                    isCompact: isCompact,
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
                    .padding(.horizontal, horizontalPadding)
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
    var deviceName: String? = nil
    let keepScreenAwake: Bool
    let onToggleKeepScreenAwake: () -> Void
    let onOpenTutorial: () -> Void
    let onDisconnect: () -> Void

    private var deviceStatusText: String {
        if let deviceName, !deviceName.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
            return "Connected • \(deviceName)"
        }
        return "Connected"
    }

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

                    Text(deviceStatusText)
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
                HStack(alignment: .top, spacing: 12) {
                    StatusDot(isLive: snapshot.isRecording || snapshot.isSessionRecording)
                        .padding(.top, 4)

                    VStack(alignment: .leading, spacing: 2) {
                        Text(HomeControlLabels.headline(snapshot))
                            .font(.system(size: 16, weight: .semibold))
                            .foregroundColor(AppColors.textPrimary)
                            .lineLimit(1)
                            .minimumScaleFactor(0.85)

                        Text(HomeControlLabels.subheadline(snapshot))
                            .font(.system(size: 13))
                            .foregroundColor(AppColors.textSecondary)
                            .lineLimit(nil)
                            .minimumScaleFactor(0.85)
                            .fixedSize(horizontal: false, vertical: true)
                    }

                    Spacer(minLength: 4)

                    BufferToggleButton(isRunning: snapshot.isRecording, onClick: onToggleBuffer)
                        .padding(.top, 1)
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
    var isCompact: Bool = false
    let onControl: (HomeControl) -> Void

    var body: some View {
        let tiles = HomeControlLabels.tiles(snapshot)
        let spacing: CGFloat = isCompact ? 10 : 12
        let gridColumns = Array(
            repeating: GridItem(.flexible(minimum: isCompact ? 110 : 130), spacing: spacing),
            count: columns
        )

        LazyVGrid(columns: gridColumns, spacing: spacing) {
            ForEach(0..<tiles.count, id: \.self) { index in
                let spec = tiles[index]
                ActionTile(
                    title: spec.title,
                    subtitle: spec.subtitle,
                    systemImage: HomeControlLabels.iconFor(spec.control, snapshot: snapshot),
                    tone: spec.tone,
                    isEnabled: spec.isEnabled,
                    isLoading: spec.isLoading,
                    minHeight: isCompact ? 54 : 64,
                    isCompact: isCompact,
                    compactTitle: spec.compactTitle,
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
