import SwiftUI

/// An sRGB colour parsed from the Mac's hex, kept as components so the
/// palette can be compared and its contrast measured.
public struct ThemeRGB: Equatable, Hashable, Sendable {
    public let red: Double
    public let green: Double
    public let blue: Double

    public init(red: Double, green: Double, blue: Double) {
        self.red = red
        self.green = green
        self.blue = blue
    }

    /// Accepts `#RRGGBB`, `RRGGBB` or `#RRGGBBAA` (alpha last, as ReplayCap writes it).
    /// Alpha is ignored: theme colours are drawn opaque.
    public init?(hex: String) {
        var digits = hex.trimmingCharacters(in: .whitespacesAndNewlines)
        if digits.hasPrefix("#") {
            digits.removeFirst()
        }
        guard digits.count == 6 || digits.count == 8,
              digits.allSatisfy(\.isHexDigit),
              let value = UInt64(digits, radix: 16) else {
            return nil
        }
        let rgb = digits.count == 8 ? value >> 8 : value
        self.init(
            red: Double((rgb >> 16) & 0xFF) / 255,
            green: Double((rgb >> 8) & 0xFF) / 255,
            blue: Double(rgb & 0xFF) / 255
        )
    }

    public var color: Color {
        Color(.sRGB, red: red, green: green, blue: blue, opacity: 1)
    }

    /// WCAG 2 relative luminance.
    public var luminance: Double {
        func linear(_ channel: Double) -> Double {
            channel <= 0.03928 ? channel / 12.92 : pow((channel + 0.055) / 1.055, 2.4)
        }
        return 0.2126 * linear(red) + 0.7152 * linear(green) + 0.0722 * linear(blue)
    }

    public func contrast(with other: ThemeRGB) -> Double {
        let lighter = max(luminance, other.luminance)
        let darker = min(luminance, other.luminance)
        return (lighter + 0.05) / (darker + 0.05)
    }

    public static let white = ThemeRGB(red: 1, green: 1, blue: 1)
    /// The app's canvas grey, used for labels on light accents.
    public static let nearBlack = ThemeRGB(hex: "#111113")!
}

public enum ThemeSurfaceStyle: String, Sendable {
    case glass
    case tintedGlass = "tinted_glass"
    case matte

    public init(wireValue: String) {
        self = ThemeSurfaceStyle(rawValue: wireValue) ?? .glass
    }

    public var title: String {
        switch self {
        case .glass: return "Liquid Glass"
        case .tintedGlass: return "Tinted Glass"
        case .matte: return "Solid Matte"
        }
    }
}

/// The colours the interface draws with: the Mac's theme when matching it,
/// otherwise the companion's own look. Text and status colours stay in `AppColors`.
public struct ThemePalette: Equatable, Sendable {
    public var canvas: ThemeRGB
    public var card: ThemeRGB
    public var accent: ThemeRGB
    public var accentSecondary: ThemeRGB
    /// Label colour for anything filled with the accent.
    public var onAccent: ThemeRGB
    /// Whether accent fills draw the Mac's two-colour gradient.
    public var useGradient: Bool
    public var surfaceStyle: ThemeSurfaceStyle

    public static let `default` = ThemePalette(
        canvas: ThemeRGB(hex: "#111113")!,
        card: ThemeRGB(hex: "#1C1C1E")!,
        accent: ThemeRGB(hex: "#0A84FF")!,
        accentSecondary: ThemeRGB(hex: "#0A84FF")!,
        onAccent: .white,
        useGradient: false,
        surfaceStyle: .glass
    )

    // MARK: - Drawing helpers

    public var accentColor: Color { accent.color }
    public var canvasColor: Color { canvas.color }
    public var cardColor: Color { card.color }
    public var onAccentColor: Color { onAccent.color }

    /// Fill for accent-coloured controls: the gradient when the Mac draws one.
    public var accentFill: LinearGradient {
        LinearGradient(
            colors: [accent.color, (useGradient ? accentSecondary : accent).color],
            startPoint: .leading,
            endPoint: .trailing
        )
    }

    /// Sheets and dialogs float over busy content, so they are near-opaque.
    public var elevatedSurface: Color { card.color.opacity(0.95) }
}

/// Turns what the Mac sent into the palette the phone draws with.
public enum ThemeResolver {
    /// White labels need at least the WCAG contrast for large text and controls.
    static let minimumWhiteContrast = 3.0

    public static func resolve(theme: CompanionTheme?, matchMac: Bool) -> ThemePalette {
        let base = ThemePalette.default
        guard matchMac, let theme = theme, let dark = theme.dark else {
            return base
        }

        let isNative = theme.atmosphere == CompanionTheme.nativeAtmosphere
        let canvas = isNative ? base.canvas : (ThemeRGB(hex: dark.canvas) ?? base.canvas)
        let card = isNative ? base.card : (ThemeRGB(hex: dark.card) ?? base.card)
        let accent = ThemeRGB(hex: dark.accent) ?? base.accent
        let accentSecondary = ThemeRGB(hex: dark.accentSecondary) ?? accent
        let useGradient = theme.dualTone && accent != accentSecondary

        return ThemePalette(
            canvas: canvas,
            card: card,
            accent: accent,
            accentSecondary: accentSecondary,
            onAccent: labelColor(on: useGradient ? [accent, accentSecondary] : [accent]),
            useGradient: useGradient,
            surfaceStyle: ThemeSurfaceStyle(wireValue: theme.surfaceStyle)
        )
    }

    /// White unless some part of the fill is too light for it.
    static func labelColor(on fills: [ThemeRGB]) -> ThemeRGB {
        let whiteReads = fills.allSatisfy { $0.contrast(with: .white) >= minimumWhiteContrast }
        return whiteReads ? .white : .nearBlack
    }
}
