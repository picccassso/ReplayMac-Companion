import SwiftUI

public struct AudioMixerBottomSheet: View {
    public var snapshot: CompanionStateSnapshot
    public var onToggleMicrophone: () -> Void
    public var onToggleSystemAudio: () -> Void
    public var onSetMicrophoneVolume: (Double) -> Void
    public var onSetSystemAudioVolume: (Double) -> Void
    public var onToggleAppMute: (String) -> Void
    public var onUnmuteAllApps: () -> Void
    public var onDismiss: () -> Void

    @State private var micSliderValue: Double = 1.0
    @State private var isEditingMic: Bool = false
    @State private var systemSliderValue: Double = 1.0
    @State private var isEditingSystem: Bool = false

    public init(
        snapshot: CompanionStateSnapshot,
        onToggleMicrophone: @escaping () -> Void,
        onToggleSystemAudio: @escaping () -> Void,
        onSetMicrophoneVolume: @escaping (Double) -> Void,
        onSetSystemAudioVolume: @escaping (Double) -> Void,
        onToggleAppMute: @escaping (String) -> Void,
        onUnmuteAllApps: @escaping () -> Void,
        onDismiss: @escaping () -> Void
    ) {
        self.snapshot = snapshot
        self.onToggleMicrophone = onToggleMicrophone
        self.onToggleSystemAudio = onToggleSystemAudio
        self.onSetMicrophoneVolume = onSetMicrophoneVolume
        self.onSetSystemAudioVolume = onSetSystemAudioVolume
        self.onToggleAppMute = onToggleAppMute
        self.onUnmuteAllApps = onUnmuteAllApps
        self.onDismiss = onDismiss
    }

    private var hasMutedApps: Bool {
        snapshot.audioApps.contains(where: \.isMuted)
    }

    public var body: some View {
        VStack(spacing: 0) {
            Capsule()
                .fill(AppColors.textTertiary)
                .frame(width: 40, height: 5)
                .padding(.top, 10)
                .padding(.bottom, 12)

            HStack {
                Text("Audio Mixer")
                    .font(.system(size: 20, weight: .bold))
                    .foregroundColor(AppColors.textPrimary)

                Spacer()

                Button("Done") {
                    onDismiss()
                }
                .font(.system(size: 15, weight: .semibold))
                .foregroundColor(AppColors.accent)
            }
            .padding(.horizontal, 20)
            .padding(.bottom, 12)

            ScrollView(.vertical, showsIndicators: false) {
                VStack(alignment: .leading, spacing: 18) {
                    volumeSection
                    applicationAudioSection
                }
                .padding(.horizontal, 20)
                .padding(.bottom, 28)
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .top)
        .background(AppColors.backgroundSecondary.ignoresSafeArea())
        .onAppear {
            micSliderValue = snapshot.microphoneVolume
            systemSliderValue = snapshot.systemAudioVolume
        }
        .onChange(of: snapshot.microphoneVolume) { newValue in
            if !isEditingMic {
                micSliderValue = newValue
            }
        }
        .onChange(of: snapshot.systemAudioVolume) { newValue in
            if !isEditingSystem {
                systemSliderValue = newValue
            }
        }
    }

    private var volumeSection: some View {
        VStack(alignment: .leading, spacing: 10) {
            SectionHeader(title: "Volume Levels")

            GlassCard(cornerRadius: AppRadii.medium) {
                VStack(spacing: 16) {
                    volumeRow(
                        title: "Microphone",
                        icon: (snapshot.isMicrophoneEnabled && !snapshot.isMicrophoneMuted) ? "mic.fill" : "mic.slash.fill",
                        isEnabled: snapshot.isMicrophoneEnabled,
                        isMuted: snapshot.isMicrophoneMuted,
                        value: $micSliderValue,
                        onToggleMute: {
                            CompanionHaptics.tap()
                            onToggleMicrophone()
                        },
                        onEditingChanged: { editing in
                            isEditingMic = editing
                            if !editing {
                                onSetMicrophoneVolume(micSliderValue)
                            }
                        },
                        onValueChange: { val in
                            onSetMicrophoneVolume(val)
                        }
                    )

                    RowSeparator(startIndent: 0)

                    volumeRow(
                        title: "System Audio",
                        icon: (snapshot.isSystemAudioEnabled && !snapshot.isSystemAudioMuted) ? "speaker.wave.2.fill" : "speaker.slash.fill",
                        isEnabled: snapshot.isSystemAudioEnabled,
                        isMuted: snapshot.isSystemAudioMuted,
                        value: $systemSliderValue,
                        onToggleMute: {
                            CompanionHaptics.tap()
                            onToggleSystemAudio()
                        },
                        onEditingChanged: { editing in
                            isEditingSystem = editing
                            if !editing {
                                onSetSystemAudioVolume(systemSliderValue)
                            }
                        },
                        onValueChange: { val in
                            onSetSystemAudioVolume(val)
                        }
                    )
                }
                .padding(16)
            }
        }
    }

    private func volumeRow(
        title: String,
        icon: String,
        isEnabled: Bool,
        isMuted: Bool,
        value: Binding<Double>,
        onToggleMute: @escaping () -> Void,
        onEditingChanged: @escaping (Bool) -> Void,
        onValueChange: @escaping (Double) -> Void
    ) -> some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack(spacing: 10) {
                Image(systemName: icon)
                    .font(.system(size: 16, weight: .semibold))
                    .foregroundColor(!isEnabled ? AppColors.textSecondary : (isMuted ? AppColors.warning : AppColors.accent))
                    .frame(width: 22)

                Text(title)
                    .font(.system(size: 15, weight: .semibold))
                    .foregroundColor(AppColors.textPrimary)

                Spacer()

                Text(isEnabled ? HomeControlLabels.volumePercentageLabel(value.wrappedValue) : "Off on Mac")
                    .font(.system(size: 13, weight: .medium).monospacedDigit())
                    .foregroundColor(AppColors.textSecondary)

                if isEnabled {
                    Button(action: onToggleMute) {
                        Text(isMuted ? "Unmute" : "Mute")
                            .font(.system(size: 12, weight: .semibold))
                            .foregroundColor(isMuted ? AppColors.warning : AppColors.textPrimary)
                            .padding(.horizontal, 10)
                            .padding(.vertical, 5)
                            .background(
                                (isMuted ? AppColors.warning.opacity(0.18) : Color.white.opacity(0.08)),
                                in: Capsule()
                            )
                    }
                    .buttonStyle(GlassPressableStyle())
                }
            }

            Slider(
                value: Binding(
                    get: { value.wrappedValue },
                    set: { newValue in
                        value.wrappedValue = newValue
                        onValueChange(newValue)
                    }
                ),
                in: 0.0...1.0,
                step: 0.01,
                onEditingChanged: onEditingChanged
            )
            .tint(isMuted ? AppColors.warning : AppColors.accent)
            .disabled(!isEnabled)
            .opacity(isEnabled ? 1.0 : 0.45)
        }
    }

    private var applicationAudioSection: some View {
        VStack(alignment: .leading, spacing: 10) {
            SectionHeader(title: "Application Audio") {
                if snapshot.isSystemAudioEnabled && !snapshot.perAppAudioEnabled && hasMutedApps {
                    Button(action: {
                        CompanionHaptics.tap()
                        onUnmuteAllApps()
                    }) {
                        Text("Unmute All")
                            .font(.system(size: 13, weight: .semibold))
                            .foregroundColor(AppColors.accent)
                    }
                }
            }

            if !snapshot.isSystemAudioEnabled {
                infoCard("System audio is turned off in ReplayCap on your Mac.")
            } else if snapshot.perAppAudioEnabled {
                infoCard("ReplayCap is set to capture a single selected app in Audio Settings on your Mac.")
            } else if snapshot.audioApps.isEmpty {
                infoCard("No running applications found on your Mac.")
            } else {
                GlassCard(cornerRadius: AppRadii.medium) {
                    VStack(spacing: 0) {
                        ForEach(Array(snapshot.audioApps.enumerated()), id: \.element.id) { index, app in
                            appRow(app)
                            if index < snapshot.audioApps.count - 1 {
                                RowSeparator(startIndent: 46)
                            }
                        }
                    }
                }
            }
        }
    }

    private func appRow(_ app: CompanionAudioApp) -> some View {
        Button(action: {
            CompanionHaptics.tap()
            onToggleAppMute(app.bundleId)
        }) {
            HStack(spacing: 12) {
                appIconView(app)

                VStack(alignment: .leading, spacing: 2) {
                    Text(app.name)
                        .font(.system(size: 15, weight: .medium))
                        .foregroundColor(AppColors.textPrimary)
                        .lineLimit(1)

                    if !app.isRunning {
                        Text("Not running")
                            .font(.system(size: 12))
                            .foregroundColor(AppColors.textSecondary)
                    }
                }

                Spacer()

                Text(app.isMuted ? "Muted" : "Audible")
                    .font(.system(size: 12, weight: .semibold))
                    .foregroundColor(app.isMuted ? AppColors.warning : AppColors.textSecondary)
                    .padding(.horizontal, 10)
                    .padding(.vertical, 5)
                    .background(
                        (app.isMuted ? AppColors.warning.opacity(0.18) : Color.white.opacity(0.06)),
                        in: Capsule()
                    )
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 12)
            .contentShape(Rectangle())
        }
        .buttonStyle(GlassPressableStyle())
    }

    @ViewBuilder
    private func appIconView(_ app: CompanionAudioApp) -> some View {
        if let base64 = app.iconBase64,
           let data = Data(base64Encoded: base64),
           let uiImage = UIImage(data: data) {
            ZStack(alignment: .bottomTrailing) {
                Image(uiImage: uiImage)
                    .resizable()
                    .interpolation(.high)
                    .aspectRatio(contentMode: .fit)
                    .frame(width: 30, height: 30)
                    .clipShape(RoundedRectangle(cornerRadius: 7, style: .continuous))
                    .opacity(app.isMuted ? 0.65 : 1.0)

                if app.isMuted {
                    Image(systemName: "speaker.slash.fill")
                        .font(.system(size: 9, weight: .bold))
                        .foregroundColor(AppColors.warning)
                        .padding(3)
                        .background(AppColors.backgroundSecondary, in: Circle())
                        .offset(x: 4, y: 4)
                }
            }
            .frame(width: 32, height: 32)
        } else {
            Image(systemName: app.isMuted ? "speaker.slash.fill" : "speaker.wave.2.fill")
                .font(.system(size: 15, weight: .semibold))
                .foregroundColor(app.isMuted ? AppColors.warning : AppColors.accent)
                .frame(width: 32, height: 32)
                .background(
                    (app.isMuted ? AppColors.warning.opacity(0.16) : Color.white.opacity(0.06)),
                    in: RoundedRectangle(cornerRadius: 8, style: .continuous)
                )
        }
    }

    private func infoCard(_ message: String) -> some View {
        GlassCard(cornerRadius: AppRadii.medium, tint: AppColors.surfaceGlassSubtle) {
            Text(message)
                .font(.system(size: 13))
                .foregroundColor(AppColors.textSecondary)
                .padding(16)
                .frame(maxWidth: .infinity, alignment: .leading)
        }
    }
}
