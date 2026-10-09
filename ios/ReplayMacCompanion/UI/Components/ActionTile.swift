import SwiftUI

public enum TileTone: Sendable, Equatable {
    case normal
    /// Accent-filled primary action, like the desktop's prominent Save tile.
    case prominent
    /// Red while something is actively capturing.
    case live
    /// Orange for a muted audio source.
    case muted
    /// Grey for a source that is switched off on the Mac.
    case off
}

private struct TilePalette {
    let fill: Color
    let icon: Color
    let title: Color
    let subtitle: Color
}

private func paletteFor(_ tone: TileTone) -> TilePalette {
    switch tone {
    case .prominent:
        return TilePalette(
            fill: AppColors.accent,
            icon: .white,
            title: .white,
            subtitle: Color.white.opacity(0.75)
        )
    case .live:
        return TilePalette(
            fill: AppColors.danger,
            icon: .white,
            title: .white,
            subtitle: Color.white.opacity(0.80)
        )
    case .muted:
        return TilePalette(
            fill: AppColors.surfaceGlass,
            icon: AppColors.warning,
            title: AppColors.textPrimary,
            subtitle: AppColors.textSecondary
        )
    case .off:
        return TilePalette(
            fill: AppColors.surfaceGlass,
            icon: AppColors.textSecondary,
            title: AppColors.textPrimary,
            subtitle: AppColors.textSecondary
        )
    case .normal:
        return TilePalette(
            fill: AppColors.surfaceGlass,
            icon: AppColors.accent,
            title: AppColors.textPrimary,
            subtitle: AppColors.textSecondary
        )
    }
}

public struct ActionTile: View {
    public var title: String
    public var subtitle: String
    public var systemImage: String
    public var onClick: () -> Void
    public var tone: TileTone
    public var isEnabled: Bool
    public var isLoading: Bool
    public var minHeight: CGFloat
    public var isCompact: Bool
    public var compactTitle: String?

    public init(
        title: String,
        subtitle: String,
        systemImage: String,
        tone: TileTone = .normal,
        isEnabled: Bool = true,
        isLoading: Bool = false,
        minHeight: CGFloat = 64,
        isCompact: Bool = false,
        compactTitle: String? = nil,
        onClick: @escaping () -> Void
    ) {
        self.title = title
        self.subtitle = subtitle
        self.systemImage = systemImage
        self.tone = tone
        self.isEnabled = isEnabled
        self.isLoading = isLoading
        self.minHeight = minHeight
        self.isCompact = isCompact
        self.compactTitle = compactTitle
        self.onClick = onClick
    }

    public var body: some View {
        let palette = paletteFor(tone)
        let resolvedTitle = (isCompact ? compactTitle : nil) ?? title
        let resolvedMinHeight = isCompact && minHeight == 64 ? 54 : minHeight
        let iconSize: CGFloat = isCompact ? 17 : 20
        let iconFrame: CGFloat = isCompact ? 20 : 24
        let progressFrame: CGFloat = isCompact ? 18 : 22
        let spacing: CGFloat = isCompact ? 8 : 12
        let hPadding: CGFloat = isCompact ? 10 : 14
        let vPadding: CGFloat = isCompact ? 8 : 12
        let titleFontSize: CGFloat = isCompact ? 13.5 : 15
        let subtitleFontSize: CGFloat = isCompact ? 11.5 : 13

        Button(action: onClick) {
            HStack(spacing: spacing) {
                if isLoading {
                    ProgressView()
                        .progressViewStyle(CircularProgressViewStyle(tint: palette.icon))
                        .frame(width: progressFrame, height: progressFrame)
                } else {
                    Image(systemName: systemImage)
                        .font(.system(size: iconSize, weight: .semibold))
                        .foregroundColor(palette.icon)
                        .frame(width: iconFrame, height: iconFrame)
                }

                VStack(alignment: .leading, spacing: 2) {
                    Text(resolvedTitle)
                        .font(.system(size: titleFontSize, weight: .semibold))
                        .foregroundColor(palette.title)
                        .lineLimit(2)
                        .minimumScaleFactor(0.72)
                        .allowsTightening(true)
                        .multilineTextAlignment(.leading)
                        .fixedSize(horizontal: false, vertical: true)

                    Text(subtitle)
                        .font(.system(size: subtitleFontSize))
                        .foregroundColor(palette.subtitle)
                        .lineLimit(1)
                        .minimumScaleFactor(0.75)
                        .allowsTightening(true)
                }

                Spacer(minLength: 0)
            }
            .padding(.horizontal, hPadding)
            .padding(.vertical, vPadding)
            .frame(maxWidth: .infinity, minHeight: resolvedMinHeight, alignment: .leading)
            .glass(
                cornerRadius: AppRadii.medium,
                tint: palette.fill,
                rimAlpha: palette.fill == AppColors.surfaceGlass ? 1.0 : 0.4
            )
        }
        .disabled(!isEnabled || isLoading)
        .buttonStyle(GlassPressableStyle(enabled: isEnabled && !isLoading))
    }
}
