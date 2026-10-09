import SwiftUI

public struct GlassBackground<Content: View>: View {
    public let content: () -> Content

    public init(@ViewBuilder content: @escaping () -> Content) {
        self.content = content
    }

    public var body: some View {
        ZStack {
            AppColors.backgroundPrimary
                .ignoresSafeArea()

            LinearGradient(
                stops: [
                    .init(color: Color.white.opacity(0.05), location: 0),
                    .init(color: Color.clear, location: 0.5)
                ],
                startPoint: .top,
                endPoint: .bottom
            )
            .ignoresSafeArea()

            content()
        }
    }
}
