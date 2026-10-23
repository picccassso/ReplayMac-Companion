import Foundation

/// One appearance's colours from ReplayCap, as hex strings already resolved
/// on the Mac (atmosphere, custom or system accent).
public struct CompanionThemePalette: Codable, Sendable, Equatable, Hashable {
    public let canvas: String
    public let card: String
    public let accent: String
    public let accentSecondary: String

    public init(canvas: String, card: String, accent: String, accentSecondary: String) {
        self.canvas = canvas
        self.card = card
        self.accent = accent
        self.accentSecondary = accentSecondary
    }

    private enum CodingKeys: String, CodingKey {
        case canvas, card, accent, accentSecondary
    }

    public init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        self.canvas = (try? container.decodeIfPresent(String.self, forKey: .canvas)) ?? ""
        self.card = (try? container.decodeIfPresent(String.self, forKey: .card)) ?? ""
        self.accent = (try? container.decodeIfPresent(String.self, forKey: .accent)) ?? ""
        self.accentSecondary = (try? container.decodeIfPresent(String.self, forKey: .accentSecondary)) ?? ""
    }
}

/// ReplayCap's Appearance settings, sent in each state snapshot. Builds of
/// ReplayCap from before theming don't send it.
public struct CompanionTheme: Codable, Sendable, Equatable, Hashable {
    public static let nativeAtmosphere = "native"

    public let atmosphere: String
    public let atmosphereName: String
    /// The Mac's own system/dark/light choice. The phone always stays dark.
    public let colorScheme: String
    public let accentMode: String
    public let surfaceStyle: String
    /// Whether the Mac draws its two-colour accent gradient.
    public let dualTone: Bool
    public let dark: CompanionThemePalette?
    public let light: CompanionThemePalette?

    public init(
        atmosphere: String,
        atmosphereName: String,
        colorScheme: String = "dark",
        accentMode: String = "atmosphere",
        surfaceStyle: String = "glass",
        dualTone: Bool = false,
        dark: CompanionThemePalette?,
        light: CompanionThemePalette? = nil
    ) {
        self.atmosphere = atmosphere
        self.atmosphereName = atmosphereName
        self.colorScheme = colorScheme
        self.accentMode = accentMode
        self.surfaceStyle = surfaceStyle
        self.dualTone = dualTone
        self.dark = dark
        self.light = light
    }

    private enum CodingKeys: String, CodingKey {
        case atmosphere, atmosphereName, colorScheme, accentMode, surfaceStyle, dualTone, dark, light
    }

    public init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        self.atmosphere = (try? container.decodeIfPresent(String.self, forKey: .atmosphere)) ?? ""
        self.atmosphereName = (try? container.decodeIfPresent(String.self, forKey: .atmosphereName)) ?? ""
        self.colorScheme = (try? container.decodeIfPresent(String.self, forKey: .colorScheme)) ?? ""
        self.accentMode = (try? container.decodeIfPresent(String.self, forKey: .accentMode)) ?? ""
        self.surfaceStyle = (try? container.decodeIfPresent(String.self, forKey: .surfaceStyle)) ?? ""
        self.dualTone = (try? container.decodeIfPresent(Bool.self, forKey: .dualTone)) ?? false
        self.dark = try? container.decodeIfPresent(CompanionThemePalette.self, forKey: .dark)
        self.light = try? container.decodeIfPresent(CompanionThemePalette.self, forKey: .light)
    }
}
