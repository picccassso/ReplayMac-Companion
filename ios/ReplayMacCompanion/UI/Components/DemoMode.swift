import SwiftUI

/// The Connect screen's way into demo mode, for trying the app without a Mac.
public struct TryDemoButton: View {
    public var onClick: () -> Void

    public init(onClick: @escaping () -> Void) {
        self.onClick = onClick
    }

    public var body: some View {
        VStack(spacing: 8) {
            GlassButton(
                text: "Try without a Mac",
                icon: "play.rectangle.on.rectangle",
                contentColor: AppColors.accent,
                onClick: onClick
            )

            Text("Explore every control with sample data. Nothing is sent anywhere.")
                .font(.system(size: 12))
                .foregroundColor(AppColors.textSecondary)
                .multilineTextAlignment(.center)
                .fixedSize(horizontal: false, vertical: true)
        }
        .frame(maxWidth: .infinity)
    }
}

/// Stays on top of Home for the whole demo, so sample data can't pass for a real Mac.
public struct DemoModeBanner: View {
    public var onExit: () -> Void

    public init(onExit: @escaping () -> Void) {
        self.onExit = onExit
    }

    public var body: some View {
        HStack(spacing: 12) {
            Image(systemName: "play.rectangle.on.rectangle.fill")
                .font(.system(size: 20, weight: .semibold))
                .foregroundColor(AppColors.warning)
                .accessibilityHidden(true)

            VStack(alignment: .leading, spacing: 2) {
                Text("Demo mode")
                    .font(.system(size: 15, weight: .semibold))
                    .foregroundColor(AppColors.textPrimary)
                Text("Sample data — no Mac is connected.")
                    .font(.system(size: 12))
                    .foregroundColor(AppColors.textSecondary)
                    .fixedSize(horizontal: false, vertical: true)
            }
            .accessibilityElement(children: .combine)

            Spacer(minLength: 8)

            GlassButton(
                text: "Exit Demo",
                contentColor: AppColors.warning,
                tint: AppColors.warning.opacity(0.16),
                onClick: onExit
            )
        }
        .padding(.horizontal, 14)
        .padding(.vertical, 10)
        .glass(cornerRadius: AppRadii.medium, tint: AppColors.warning.opacity(0.12))
    }
}
