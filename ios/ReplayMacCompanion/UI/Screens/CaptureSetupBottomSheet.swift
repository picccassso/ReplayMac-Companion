import SwiftUI

public struct CaptureSetupBottomSheet: View {
    public var snapshot: CompanionStateSnapshot
    public var onSelectDisplay: (String) -> Void
    public var onApplyProfile: (String) -> Void
    public var onDismiss: () -> Void

    public init(
        snapshot: CompanionStateSnapshot,
        onSelectDisplay: @escaping (String) -> Void,
        onApplyProfile: @escaping (String) -> Void,
        onDismiss: @escaping () -> Void
    ) {
        self.snapshot = snapshot
        self.onSelectDisplay = onSelectDisplay
        self.onApplyProfile = onApplyProfile
        self.onDismiss = onDismiss
    }

    public var body: some View {
        VStack(spacing: 0) {
            Capsule()
                .fill(AppColors.textTertiary)
                .frame(width: 40, height: 5)
                .padding(.top, 10)
                .padding(.bottom, 12)

            HStack {
                Text("Capture Setup")
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
                    displaysSection
                    profilesSection
                }
                .padding(.horizontal, 20)
                .padding(.bottom, 28)
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .top)
        .background(AppColors.backgroundSecondary.ignoresSafeArea())
    }

    private var displaysSection: some View {
        VStack(alignment: .leading, spacing: 10) {
            SectionHeader(title: "Capture Display")

            if snapshot.displays.isEmpty {
                infoCard("No displays reported by your Mac.")
            } else {
                GlassCard(cornerRadius: AppRadii.medium) {
                    VStack(spacing: 0) {
                        ForEach(Array(snapshot.displays.enumerated()), id: \.element.id) { index, display in
                            displayRow(display)
                            if index < snapshot.displays.count - 1 {
                                RowSeparator(startIndent: 46)
                            }
                        }
                    }
                }
            }
        }
    }

    private func displayRow(_ display: CompanionDisplayInfo) -> some View {
        Button(action: {
            CompanionHaptics.tap()
            onSelectDisplay(display.id)
        }) {
            HStack(spacing: 12) {
                Image(systemName: "display")
                    .font(.system(size: 16, weight: .semibold))
                    .foregroundColor(display.isActive ? AppColors.accent : AppColors.textSecondary)
                    .frame(width: 22)

                VStack(alignment: .leading, spacing: 2) {
                    Text(display.name)
                        .font(.system(size: 15, weight: .medium))
                        .foregroundColor(AppColors.textPrimary)
                        .lineLimit(1)

                    if !display.isConnected {
                        Text("Offline")
                            .font(.system(size: 12))
                            .foregroundColor(AppColors.warning)
                    }
                }

                Spacer()

                if display.isActive {
                    HStack(spacing: 4) {
                        Image(systemName: "checkmark")
                            .font(.system(size: 11, weight: .bold))
                        Text("Active")
                            .font(.system(size: 12, weight: .semibold))
                    }
                    .foregroundColor(AppColors.accent)
                    .padding(.horizontal, 10)
                    .padding(.vertical, 5)
                    .background(AppColors.accent.opacity(0.16), in: Capsule())
                }
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 12)
            .contentShape(Rectangle())
        }
        .buttonStyle(GlassPressableStyle())
    }

    private var profilesSection: some View {
        VStack(alignment: .leading, spacing: 10) {
            SectionHeader(title: "Capture Profiles")

            if snapshot.profiles.isEmpty {
                infoCard("No capture profiles saved yet. Create profiles in ReplayCap Settings → Profiles on your Mac.")
            } else {
                GlassCard(cornerRadius: AppRadii.medium) {
                    VStack(spacing: 0) {
                        ForEach(Array(snapshot.profiles.enumerated()), id: \.element.id) { index, profile in
                            profileRow(profile)
                            if index < snapshot.profiles.count - 1 {
                                RowSeparator(startIndent: 46)
                            }
                        }
                    }
                }
            }
        }
    }

    private func profileRow(_ profile: CompanionProfileSummary) -> some View {
        Button(action: {
            CompanionHaptics.tap()
            onApplyProfile(profile.id)
        }) {
            HStack(spacing: 12) {
                Image(systemName: "rectangle.stack.badge.play")
                    .font(.system(size: 16, weight: .semibold))
                    .foregroundColor(profile.isActive ? AppColors.accent : AppColors.textSecondary)
                    .frame(width: 22)

                VStack(alignment: .leading, spacing: 3) {
                    Text(profile.name)
                        .font(.system(size: 15, weight: .semibold))
                        .foregroundColor(AppColors.textPrimary)
                        .lineLimit(1)

                    if !profile.summary.isEmpty {
                        Text(profile.summary)
                            .font(.system(size: 12))
                            .foregroundColor(AppColors.textSecondary)
                            .lineLimit(2)
                            .multilineTextAlignment(.leading)
                    }
                }

                Spacer()

                if profile.isActive {
                    HStack(spacing: 4) {
                        Image(systemName: "checkmark")
                            .font(.system(size: 11, weight: .bold))
                        Text("Active")
                            .font(.system(size: 12, weight: .semibold))
                    }
                    .foregroundColor(AppColors.accent)
                    .padding(.horizontal, 10)
                    .padding(.vertical, 5)
                    .background(AppColors.accent.opacity(0.16), in: Capsule())
                }
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 12)
            .contentShape(Rectangle())
        }
        .buttonStyle(GlassPressableStyle())
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
