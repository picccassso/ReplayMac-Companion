import SwiftUI
import UIKit

public struct GlassPressableStyle: ButtonStyle {
    public var enabled: Bool
    public var haptic: Bool

    public init(enabled: Bool = true, haptic: Bool = true) {
        self.enabled = enabled
        self.haptic = haptic
    }

    public func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .scaleEffect(configuration.isPressed && enabled ? 0.96 : 1.0)
            .animation(.spring(response: 0.25, dampingFraction: 0.65), value: configuration.isPressed)
            .opacity(enabled ? 1.0 : 0.45)
            .onChange(of: configuration.isPressed) { isPressed in
                if isPressed && enabled && haptic {
                    UIImpactFeedbackGenerator(style: .light).impactOccurred()
                }
            }
    }
}

public struct AccentButton: View {
    public var text: String
    public var icon: String?
    public var onClick: () -> Void
    public var enabled: Bool
    public var color: Color
    public var cornerRadius: CGFloat

    public init(
        text: String,
        icon: String? = nil,
        enabled: Bool = true,
        color: Color = AppColors.accent,
        cornerRadius: CGFloat = AppRadii.small,
        onClick: @escaping () -> Void
    ) {
        self.text = text
        self.icon = icon
        self.enabled = enabled
        self.color = color
        self.cornerRadius = cornerRadius
        self.onClick = onClick
    }

    public var body: some View {
        Button(action: onClick) {
            HStack(spacing: 6) {
                if let icon = icon {
                    Image(systemName: icon)
                        .font(.system(size: 15, weight: .semibold))
                }
                Text(text)
                    .font(.system(size: 15, weight: .semibold))
            }
            .foregroundColor(.white)
            .padding(.horizontal, 18)
            .padding(.vertical, 12)
            .background(color, in: RoundedRectangle(cornerRadius: cornerRadius, style: .continuous))
        }
        .disabled(!enabled)
        .buttonStyle(GlassPressableStyle(enabled: enabled))
    }
}

public struct GlassButton: View {
    public var text: String
    public var icon: String?
    public var onClick: () -> Void
    public var enabled: Bool
    public var contentColor: Color
    public var tint: Color
    public var cornerRadius: CGFloat

    public init(
        text: String,
        icon: String? = nil,
        enabled: Bool = true,
        contentColor: Color = AppColors.textPrimary,
        tint: Color = AppColors.surfaceGlass,
        cornerRadius: CGFloat = AppRadii.small,
        onClick: @escaping () -> Void
    ) {
        self.text = text
        self.icon = icon
        self.enabled = enabled
        self.contentColor = contentColor
        self.tint = tint
        self.cornerRadius = cornerRadius
        self.onClick = onClick
    }

    public var body: some View {
        Button(action: onClick) {
            HStack(spacing: 6) {
                if let icon = icon {
                    Image(systemName: icon)
                        .font(.system(size: 15, weight: .medium))
                }
                Text(text)
                    .font(.system(size: 15, weight: .medium))
            }
            .foregroundColor(contentColor)
            .padding(.horizontal, 18)
            .padding(.vertical, 12)
            .glass(cornerRadius: cornerRadius, tint: tint)
        }
        .disabled(!enabled)
        .buttonStyle(GlassPressableStyle(enabled: enabled))
    }
}

public struct GlassIconButton: View {
    public var systemImage: String
    public var contentDescription: String
    public var onClick: () -> Void
    public var tint: Color
    public var selected: Bool
    public var size: CGFloat

    public init(
        systemImage: String,
        contentDescription: String,
        tint: Color = AppColors.textPrimary,
        selected: Bool = false,
        size: CGFloat = 40,
        onClick: @escaping () -> Void
    ) {
        self.systemImage = systemImage
        self.contentDescription = contentDescription
        self.tint = tint
        self.selected = selected
        self.size = size
        self.onClick = onClick
    }

    public var body: some View {
        Button(action: onClick) {
            ZStack {
                Circle()
                    .fill(selected ? AppColors.accent : AppColors.surfaceGlass)
                    .frame(width: size, height: size)
                    .overlay(
                        Circle()
                            .strokeBorder(Color.white.opacity(0.12), lineWidth: 0.75)
                    )

                Image(systemName: systemImage)
                    .font(.system(size: size * 0.44, weight: .medium))
                    .foregroundColor(selected ? .white : tint)
            }
            .frame(width: max(size, 44), height: max(size, 44))
        }
        .buttonStyle(GlassPressableStyle())
        .accessibilityLabel(contentDescription)
    }
}

public struct GlassButtonGroup<Content: View>: View {
    public let content: () -> Content

    public init(@ViewBuilder content: @escaping () -> Content) {
        self.content = content
    }

    public var body: some View {
        HStack(spacing: 4) {
            content()
        }
        .padding(.horizontal, 4)
        .padding(.vertical, 4)
        .background(Capsule().fill(AppColors.surfaceGlass))
        .overlay(
            Capsule().strokeBorder(Color.white.opacity(0.12), lineWidth: 0.75)
        )
    }
}

public struct GroupIconButton: View {
    public var systemImage: String
    public var contentDescription: String
    public var onClick: () -> Void
    public var tint: Color
    public var selected: Bool

    public init(
        systemImage: String,
        contentDescription: String,
        tint: Color = AppColors.textPrimary,
        selected: Bool = false,
        onClick: @escaping () -> Void
    ) {
        self.systemImage = systemImage
        self.contentDescription = contentDescription
        self.tint = tint
        self.selected = selected
        self.onClick = onClick
    }

    public var body: some View {
        Button(action: onClick) {
            ZStack {
                Circle()
                    .fill(selected ? AppColors.accent : Color.clear)
                    .frame(width: 36, height: 36)

                Image(systemName: systemImage)
                    .font(.system(size: 17, weight: .medium))
                    .foregroundColor(selected ? .white : tint)
            }
        }
        .buttonStyle(GlassPressableStyle())
        .accessibilityLabel(contentDescription)
    }
}
