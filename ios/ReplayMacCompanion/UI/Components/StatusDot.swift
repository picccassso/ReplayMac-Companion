import SwiftUI

public struct StatusDot: View {
    public var isLive: Bool
    public var size: CGFloat
    public var activeColor: Color
    public var idleColor: Color

    @State private var isPulsing = false

    public init(
        isLive: Bool,
        size: CGFloat = 10,
        activeColor: Color = AppColors.indicatorActive,
        idleColor: Color = AppColors.indicatorIdle
    ) {
        self.isLive = isLive
        self.size = size
        self.activeColor = activeColor
        self.idleColor = idleColor
    }

    public var body: some View {
        ZStack {
            if isLive {
                Circle()
                    .fill(activeColor.opacity(isPulsing ? 0.0 : 0.6))
                    .frame(width: size, height: size)
                    .scaleEffect(isPulsing ? 2.4 : 1.0)
                    .animation(
                        .easeOut(duration: 1.2).repeatForever(autoreverses: false),
                        value: isPulsing
                    )

                Circle()
                    .fill(activeColor)
                    .frame(width: size, height: size)
            } else {
                Circle()
                    .fill(idleColor)
                    .frame(width: size, height: size)
            }
        }
        .frame(width: size * 2.4, height: size * 2.4)
        .onAppear {
            if isLive {
                isPulsing = true
            }
        }
        .onChange(of: isLive) { live in
            isPulsing = live
        }
    }
}
