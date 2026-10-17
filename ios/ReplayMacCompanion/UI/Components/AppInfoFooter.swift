import SwiftUI

/// States that the app needs ReplayCap on a Mac, and links to it and to the
/// privacy policy. Shown under the connection options and in the quick guide,
/// so both are reachable whether or not a Mac is connected.
public struct AppInfoFooter: View {
    public var showsRequirement: Bool

    public init(showsRequirement: Bool = true) {
        self.showsRequirement = showsRequirement
    }

    public var body: some View {
        VStack(spacing: 8) {
            if showsRequirement {
                Text("Works with ReplayCap for Mac, available on the Mac App Store.")
                    .font(.system(size: 12))
                    .foregroundColor(AppColors.textSecondary)
                    .multilineTextAlignment(.center)
                    .fixedSize(horizontal: false, vertical: true)
            }

            HStack(spacing: 6) {
                Link("Get ReplayCap for Mac", destination: AppLinks.replayCapForMac)
                Text(verbatim: "·")
                    .foregroundColor(AppColors.textTertiary)
                    .accessibilityHidden(true)
                Link("Privacy Policy", destination: AppLinks.privacyPolicy)
            }
            .font(.system(size: 12, weight: .medium))
            .foregroundColor(AppColors.accent)
        }
        .frame(maxWidth: .infinity)
    }
}
