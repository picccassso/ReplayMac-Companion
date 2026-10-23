import SwiftUI

/// A floating panel. With no tint it draws the theme's surface, mirroring the
/// desktop: translucent glass, accent-tinted glass, or an opaque matte card.
public struct GlassPanelModifier: ViewModifier {
    @Environment(\.themePalette) private var palette

    public var cornerRadius: CGFloat
    /// An explicit fill, such as a status colour. Nil follows the theme.
    public var tint: Color?
    public var rimAlpha: Double
    public var borderWidth: CGFloat

    public init(
        cornerRadius: CGFloat = AppRadii.large,
        tint: Color? = nil,
        rimAlpha: Double = 1.0,
        borderWidth: CGFloat = 0.75
    ) {
        self.cornerRadius = cornerRadius
        self.tint = tint
        self.rimAlpha = rimAlpha
        self.borderWidth = borderWidth
    }

    public func body(content: Content) -> some View {
        let shape = RoundedRectangle(cornerRadius: cornerRadius, style: .continuous)
        content
            .background(fill, in: shape)
            .overlay(shape.strokeBorder(border, lineWidth: tint == nil && palette.surfaceStyle == .matte ? 1 : borderWidth))
            .clipShape(shape)
    }

    private var fill: AnyShapeStyle {
        if let tint = tint {
            return AnyShapeStyle(tint)
        }
        switch palette.surfaceStyle {
        case .glass:
            return AnyShapeStyle(AppColors.surfaceGlass)
        case .tintedGlass:
            return AnyShapeStyle(LinearGradient(
                colors: [palette.cardColor.opacity(0.65), palette.accentColor.opacity(0.08)],
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            ))
        case .matte:
            return AnyShapeStyle(palette.cardColor)
        }
    }

    private var border: AnyShapeStyle {
        if tint == nil {
            switch palette.surfaceStyle {
            case .glass:
                break
            case .tintedGlass:
                return AnyShapeStyle(palette.accentColor.opacity(0.24 * rimAlpha))
            case .matte:
                return AnyShapeStyle(Color.white.opacity(0.09 * rimAlpha))
            }
        }
        return AnyShapeStyle(LinearGradient(
            colors: [
                Color.white.opacity(0.16 * rimAlpha),
                Color.white.opacity(0.06 * rimAlpha)
            ],
            startPoint: .top,
            endPoint: .bottom
        ))
    }
}

/// A panel filled with the accent, or the desktop's accent gradient when it draws one.
private struct AccentPanelModifier: ViewModifier {
    @Environment(\.themePalette) private var palette
    let cornerRadius: CGFloat

    func body(content: Content) -> some View {
        let shape = RoundedRectangle(cornerRadius: cornerRadius, style: .continuous)
        content
            .background(palette.accentFill, in: shape)
            .overlay(
                shape.strokeBorder(
                    LinearGradient(
                        colors: [Color.white.opacity(0.064), Color.white.opacity(0.024)],
                        startPoint: .top,
                        endPoint: .bottom
                    ),
                    lineWidth: 0.75
                )
            )
            .clipShape(shape)
    }
}

public extension View {
    func glass(
        cornerRadius: CGFloat = AppRadii.large,
        tint: Color? = nil,
        rimAlpha: Double = 1.0,
        borderWidth: CGFloat = 0.75
    ) -> some View {
        modifier(GlassPanelModifier(
            cornerRadius: cornerRadius,
            tint: tint,
            rimAlpha: rimAlpha,
            borderWidth: borderWidth
        ))
    }

    func accentPanel(cornerRadius: CGFloat = AppRadii.large) -> some View {
        modifier(AccentPanelModifier(cornerRadius: cornerRadius))
    }
}

public struct GlassCard<Content: View>: View {
    public var cornerRadius: CGFloat
    public var tint: Color?
    public var onClick: (() -> Void)?
    public let content: () -> Content

    public init(
        cornerRadius: CGFloat = AppRadii.large,
        tint: Color? = nil,
        onClick: (() -> Void)? = nil,
        @ViewBuilder content: @escaping () -> Content
    ) {
        self.cornerRadius = cornerRadius
        self.tint = tint
        self.onClick = onClick
        self.content = content
    }

    public var body: some View {
        Group {
            if let onClick = onClick {
                Button(action: onClick) {
                    content()
                }
                .buttonStyle(PlainButtonStyle())
            } else {
                content()
            }
        }
        .glass(cornerRadius: cornerRadius, tint: tint)
    }
}
