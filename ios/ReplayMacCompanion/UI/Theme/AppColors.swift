import SwiftUI

/// Mirrors the macOS dark appearance ReplayCap uses on the desktop: system
/// blue accent, Apple's label greys, and translucent white fills for glass.
public enum AppColors {
    public static let accent = Color(red: 10/255, green: 132/255, blue: 255/255) // #0A84FF
    public static let accentSecondary = Color(red: 0/255, green: 113/255, blue: 227/255) // #0071E3
    public static let accentVariant = Color(red: 100/255, green: 210/255, blue: 255/255) // #64D2FF

    public static let danger = Color(red: 255/255, green: 69/255, blue: 58/255) // #FF453A
    public static let dangerContainer = Color(red: 255/255, green: 69/255, blue: 58/255).opacity(0.16)

    public static let success = Color(red: 48/255, green: 209/255, blue: 88/255) // #30D158
    public static let successContainer = Color(red: 48/255, green: 209/255, blue: 88/255).opacity(0.16)

    public static let warning = Color(red: 255/255, green: 159/255, blue: 10/255) // #FF9F0A

    public static let backgroundPrimary = Color(red: 17/255, green: 17/255, blue: 19/255) // #111113
    public static let backgroundSecondary = Color(red: 28/255, green: 28/255, blue: 30/255) // #1C1C1E

    /// Base tint for floating glass panels, layered over the ambient backdrop.
    public static let surfaceGlass = Color.white.opacity(0.07) // 0x12FFFFFF
    /// Quieter fill for controls that sit inside a glass panel.
    public static let surfaceGlassSubtle = Color.white.opacity(0.04) // 0x0AFFFFFF
    /// Near-opaque glass for sheets and dialogs, which float over busy content.
    public static let surfaceElevated = Color(red: 30/255, green: 30/255, blue: 32/255).opacity(0.95)

    public static let glassBorder = Color.white.opacity(0.12)
    public static let glassBorderHighlight = Color.white.opacity(0.25)
    public static let separator = Color.white.opacity(0.10)

    public static let textPrimary = Color(red: 245/255, green: 245/255, blue: 247/255)
    public static let textSecondary = Color(red: 235/255, green: 235/255, blue: 245/255).opacity(0.60)
    public static let textTertiary = Color(red: 235/255, green: 235/255, blue: 245/255).opacity(0.30)

    public static let indicatorActive = danger
    public static let indicatorIdle = Color(red: 99/255, green: 99/255, blue: 102/255)
}
