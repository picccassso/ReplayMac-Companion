import SwiftUI

private struct ThemePaletteKey: EnvironmentKey {
    static let defaultValue = ThemePalette.default
}

public extension EnvironmentValues {
    /// The palette every themed view draws with, set once at each root.
    var themePalette: ThemePalette {
        get { self[ThemePaletteKey.self] }
        set { self[ThemePaletteKey.self] = newValue }
    }
}

public extension View {
    /// Applies the palette to a screen or sheet: environment, control tint and
    /// the always-dark scheme. Sheets need it too, as they present separately.
    func themed(_ palette: ThemePalette) -> some View {
        environment(\.themePalette, palette)
            .tint(palette.accentColor)
            .preferredColorScheme(.dark)
    }
}
