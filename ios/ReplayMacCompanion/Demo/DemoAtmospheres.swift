import Foundation

/// ReplayCap's atmospheres, copied from the Mac so the demo can show theming
/// without one. A real Mac sends its colours already resolved, so nothing
/// outside the demo reads this table.
public struct DemoAtmosphere: Identifiable, Equatable, Sendable {
    public let id: String
    public let name: String
    public let canvas: String
    public let card: String
    public let accent: String
    public let accentSecondary: String

    public var isNative: Bool { id == CompanionTheme.nativeAtmosphere }

    /// The theme the demo Mac sends while this atmosphere is chosen.
    public var theme: CompanionTheme {
        CompanionTheme(
            atmosphere: id,
            atmosphereName: name,
            colorScheme: "dark",
            accentMode: "atmosphere",
            surfaceStyle: isNative ? "glass" : "tinted_glass",
            dualTone: !isNative,
            dark: CompanionThemePalette(canvas: canvas, card: card, accent: accent, accentSecondary: accentSecondary)
        )
    }
}

public enum DemoAtmospheres {
    public static let all: [DemoAtmosphere] = [
        DemoAtmosphere(id: "native", name: "Native", canvas: "#1E1E1E", card: "#2A2A2C", accent: "#0A84FF", accentSecondary: "#0A84FF"),
        DemoAtmosphere(id: "pitch_black", name: "Pitch Black", canvas: "#07080A", card: "#111318", accent: "#00E5CC", accentSecondary: "#00A3FF"),
        DemoAtmosphere(id: "carbon_studio", name: "Carbon Studio", canvas: "#131315", card: "#1D1E22", accent: "#FF6B2C", accentSecondary: "#FFB800"),
        DemoAtmosphere(id: "deep_ocean", name: "Deep Ocean", canvas: "#0A101D", card: "#121C30", accent: "#38BDF8", accentSecondary: "#6366F1"),
        DemoAtmosphere(id: "arena_boost", name: "Arena Boost", canvas: "#110D1C", card: "#1B152C", accent: "#F43F5E", accentSecondary: "#A855F7"),
        DemoAtmosphere(id: "nordic_frost", name: "Nordic Frost", canvas: "#171C24", card: "#222936", accent: "#88C0D0", accentSecondary: "#5E81AC"),
        DemoAtmosphere(id: "catppuccin_mocha", name: "Catppuccin Mocha", canvas: "#1E1E2E", card: "#313244", accent: "#CBA6F7", accentSecondary: "#B4BEFE"),
        DemoAtmosphere(id: "catppuccin_macchiato", name: "Catppuccin Macchiato", canvas: "#24273A", card: "#363A4F", accent: "#F5BDE6", accentSecondary: "#C6A0F6"),
        DemoAtmosphere(id: "catppuccin_frappe", name: "Catppuccin Frappé", canvas: "#303446", card: "#414559", accent: "#BABBF1", accentSecondary: "#85C1DC")
    ]

    public static let native = all[0]

    public static func atmosphere(id: String) -> DemoAtmosphere? {
        all.first { $0.id == id }
    }
}
