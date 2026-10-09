import SwiftUI

public struct GlassPanelModifier: ViewModifier {
    public var cornerRadius: CGFloat
    public var tint: Color
    public var rimAlpha: Double
    public var borderWidth: CGFloat

    public init(
        cornerRadius: CGFloat = AppRadii.large,
        tint: Color = AppColors.surfaceGlass,
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
            .background(tint, in: shape)
            .overlay(
                shape.strokeBorder(
                    LinearGradient(
                        colors: [
                            Color.white.opacity(0.16 * rimAlpha),
                            Color.white.opacity(0.06 * rimAlpha)
                        ],
                        startPoint: .top,
                        endPoint: .bottom
                    ),
                    lineWidth: borderWidth
                )
            )
            .clipShape(shape)
    }
}

public extension View {
    func glass(
        cornerRadius: CGFloat = AppRadii.large,
        tint: Color = AppColors.surfaceGlass,
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
}

public struct GlassCard<Content: View>: View {
    public var cornerRadius: CGFloat
    public var tint: Color
    public var onClick: (() -> Void)?
    public let content: () -> Content

    public init(
        cornerRadius: CGFloat = AppRadii.large,
        tint: Color = AppColors.surfaceGlass,
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
