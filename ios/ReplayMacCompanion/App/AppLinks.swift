import Foundation

/// Web addresses the app opens. Kept in one place so the App Store listing,
/// the in-app links and the docs all point at the same pages.
enum AppLinks {
    /// ReplayCap on the Mac App Store, the app this companion controls.
    static let replayCapForMac = URL(string: "https://apps.apple.com/app/replaycap/id6789296427")!

    static let privacyPolicy = URL(string: "https://github.com/picccassso/ReplayMac-Companion/blob/main/PRIVACY.md")!

    static let support = URL(string: "https://github.com/picccassso/ReplayMac-Companion/issues")!
}
