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

    public init(
        title: String,
        subtitle: String,
        systemImage: String,
        tone: TileTone = .normal,
        isEnabled: Bool = true,
        isLoading: Bool = false,
        minHeight: CGFloat = 64,
        onClick: @escaping () -> Void
    ) {
        self.title = title
        self.subtitle = subtitle
        self.systemImage = systemImage
        self.tone = tone
        self.isEnabled = isEnabled
        self.isLoading = isLoading
        self.minHeight = minHeight
        self.onClick = onClick
    }

    public var body: some View {
        let palette = paletteFor(tone)

        Button(action: onClick) {
            HStack(spacing: 12) {
                if isLoading {
                    ProgressView()
                        .progressViewStyle(CircularProgressViewStyle(tint: palette.icon))
                        .frame(width: 22, height: 22)
                } else {
                    Image(systemName: systemImage)
                        .font(.system(size: 20, weight: .semibold))
                        .foregroundColor(palette.icon)
                        .frame(width: 24, height: 24)
                }

                VStack(alignment: .leading, spacing: 2) {
                    Text(title)
                        .font(.system(size: 15, weight: .semibold))
                        .foregroundColor(palette.title)
                        .lineLimit(2)
                        .multilineTextAlignment(.leading)

                    Text(subtitle)
                        .font(.system(size: 13))
                        .foregroundColor(palette.subtitle)
                        .lineLimit(1)
                }

                Spacer(minLength: 0)
            }
            .padding(.horizontal, 14)
            .padding(.vertical, 12)
            .frame(maxWidth: .infinity, minHeight: minHeight, alignment: .leading)
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
