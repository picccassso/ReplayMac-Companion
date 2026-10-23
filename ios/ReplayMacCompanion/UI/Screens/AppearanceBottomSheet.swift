import SwiftUI

/// Shows the Mac's theme and whether the phone matches it. In the demo it
/// lists ReplayCap's atmospheres so the theming can be tried without a Mac.
public struct AppearanceBottomSheet: View {
    @Environment(\.themePalette) private var palette

    public var macTheme: CompanionTheme?
    public var matchMac: Bool
    public var isDemo: Bool
    public var onSetMatchMac: (Bool) -> Void
    public var onSelectAtmosphere: (String) -> Void
    public var onDismiss: () -> Void

    public init(
        macTheme: CompanionTheme?,
        matchMac: Bool,
        isDemo: Bool,
        onSetMatchMac: @escaping (Bool) -> Void,
        onSelectAtmosphere: @escaping (String) -> Void,
        onDismiss: @escaping () -> Void
    ) {
        self.macTheme = macTheme
        self.matchMac = matchMac
        self.isDemo = isDemo
        self.onSetMatchMac = onSetMatchMac
        self.onSelectAtmosphere = onSelectAtmosphere
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
                Text("Appearance")
                    .font(.system(size: 20, weight: .bold))
                    .foregroundColor(AppColors.textPrimary)

                Spacer()

                Button("Done") {
                    onDismiss()
                }
                .font(.system(size: 15, weight: .semibold))
                .foregroundColor(palette.accentColor)
            }
            .padding(.horizontal, 20)
            .padding(.bottom, 12)

            ScrollView(.vertical, showsIndicators: false) {
                VStack(alignment: .leading, spacing: 18) {
                    if isDemo {
                        atmospheresSection
                    } else {
                        matchMacSection
                        macThemeSection
                    }
                }
                .padding(.horizontal, 20)
                .padding(.bottom, 28)
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .top)
        .background(palette.cardColor.ignoresSafeArea())
    }

    // MARK: - Real Mac

    private var matchMacSection: some View {
        GlassCard(cornerRadius: AppRadii.medium) {
            Toggle(isOn: Binding(get: { matchMac }, set: onSetMatchMac)) {
                VStack(alignment: .leading, spacing: 3) {
                    Text("Match my Mac")
                        .font(.system(size: 15, weight: .semibold))
                        .foregroundColor(AppColors.textPrimary)
                    Text("Use the colours from ReplayCap on your Mac.")
                        .font(.system(size: 12))
                        .foregroundColor(AppColors.textSecondary)
                }
            }
            .tint(palette.accentColor)
            .padding(.horizontal, 16)
            .padding(.vertical, 12)
        }
    }

    @ViewBuilder
    private var macThemeSection: some View {
        VStack(alignment: .leading, spacing: 10) {
            SectionHeader(title: "Your Mac's Theme")

            if let theme = macTheme, let colours = theme.dark {
                GlassCard(cornerRadius: AppRadii.medium) {
                    HStack(spacing: 12) {
                        VStack(alignment: .leading, spacing: 3) {
                            Text(theme.atmosphereName.isEmpty ? "Custom" : theme.atmosphereName)
                                .font(.system(size: 15, weight: .semibold))
                                .foregroundColor(AppColors.textPrimary)
                            Text(ThemeSurfaceStyle(wireValue: theme.surfaceStyle).title)
                                .font(.system(size: 12))
                                .foregroundColor(AppColors.textSecondary)
                        }

                        Spacer()

                        SwatchRow(hexes: [colours.canvas, colours.card, colours.accent, colours.accentSecondary])
                    }
                    .padding(.horizontal, 16)
                    .padding(.vertical, 12)
                }
                .opacity(matchMac ? 1 : 0.55)
            } else {
                GlassCard(cornerRadius: AppRadii.medium, tint: AppColors.surfaceGlassSubtle) {
                    Text("Update ReplayCap on your Mac to share its theme.")
                        .font(.system(size: 13))
                        .foregroundColor(AppColors.textSecondary)
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .padding(.horizontal, 16)
                        .padding(.vertical, 14)
                }
            }
        }
    }

    // MARK: - Demo

    private var atmospheresSection: some View {
        VStack(alignment: .leading, spacing: 10) {
            SectionHeader(title: "Atmospheres")

            GlassCard(cornerRadius: AppRadii.medium) {
                VStack(spacing: 0) {
                    ForEach(Array(DemoAtmospheres.all.enumerated()), id: \.element.id) { index, atmosphere in
                        atmosphereRow(atmosphere)
                        if index < DemoAtmospheres.all.count - 1 {
                            RowSeparator(startIndent: 16)
                        }
                    }
                }
            }
        }
    }

    private func atmosphereRow(_ atmosphere: DemoAtmosphere) -> some View {
        let isSelected = macTheme?.atmosphere == atmosphere.id
        return Button(action: {
            CompanionHaptics.tap()
            onSelectAtmosphere(atmosphere.id)
        }) {
            HStack(spacing: 12) {
                Text(atmosphere.name)
                    .font(.system(size: 15, weight: .medium))
                    .foregroundColor(AppColors.textPrimary)
                    .lineLimit(1)

                Spacer()

                SwatchRow(hexes: [atmosphere.accent, atmosphere.accentSecondary])

                Image(systemName: "checkmark")
                    .font(.system(size: 13, weight: .bold))
                    .foregroundColor(palette.accentColor)
                    .opacity(isSelected ? 1 : 0)
                    .frame(width: 16)
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 12)
            .contentShape(Rectangle())
        }
        .buttonStyle(GlassPressableStyle())
        .accessibilityAddTraits(isSelected ? .isSelected : [])
    }
}

/// Small circles showing a theme's colours.
private struct SwatchRow: View {
    let hexes: [String]

    var body: some View {
        HStack(spacing: 6) {
            ForEach(Array(hexes.enumerated()), id: \.offset) { _, hex in
                Circle()
                    .fill((ThemeRGB(hex: hex) ?? ThemePalette.default.card).color)
                    .frame(width: 20, height: 20)
                    .overlay(Circle().strokeBorder(Color.white.opacity(0.2), lineWidth: 0.75))
            }
        }
        .accessibilityHidden(true)
    }
}
