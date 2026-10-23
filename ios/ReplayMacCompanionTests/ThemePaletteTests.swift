import XCTest
@testable import ReplayCapCompanion

final class ThemePaletteTests: XCTestCase {
    private func theme(
        atmosphere: String = "catppuccin_mocha",
        surfaceStyle: String = "tinted_glass",
        dualTone: Bool = true,
        canvas: String = "#1E1E2E",
        card: String = "#313244",
        accent: String = "#CBA6F7",
        accentSecondary: String = "#B4BEFE"
    ) -> CompanionTheme {
        CompanionTheme(
            atmosphere: atmosphere,
            atmosphereName: "Catppuccin Mocha",
            surfaceStyle: surfaceStyle,
            dualTone: dualTone,
            dark: CompanionThemePalette(canvas: canvas, card: card, accent: accent, accentSecondary: accentSecondary)
        )
    }

    // MARK: - Hex

    func testHexParsing() {
        XCTAssertEqual(ThemeRGB(hex: "#FF0000"), ThemeRGB(red: 1, green: 0, blue: 0))
        XCTAssertEqual(ThemeRGB(hex: "00ff00"), ThemeRGB(red: 0, green: 1, blue: 0))
        XCTAssertEqual(ThemeRGB(hex: " #0000FF "), ThemeRGB(red: 0, green: 0, blue: 1))
        XCTAssertEqual(ThemeRGB(hex: "#0000FF80"), ThemeRGB(red: 0, green: 0, blue: 1), "alpha comes last and is ignored")
        XCTAssertNil(ThemeRGB(hex: ""))
        XCTAssertNil(ThemeRGB(hex: "#FFF"))
        XCTAssertNil(ThemeRGB(hex: "#GGGGGG"))
        XCTAssertNil(ThemeRGB(hex: "blue"))
    }

    // MARK: - Resolving

    func testDefaultLookWhenNotMatchingOrNothingSent() {
        XCTAssertEqual(ThemeResolver.resolve(theme: theme(), matchMac: false), .default)
        XCTAssertEqual(ThemeResolver.resolve(theme: nil, matchMac: true), .default)
        let noDarkPalette = CompanionTheme(atmosphere: "deep_ocean", atmosphereName: "Deep Ocean", dark: nil)
        XCTAssertEqual(ThemeResolver.resolve(theme: noDarkPalette, matchMac: true), .default)
    }

    func testAtmosphereColoursAreUsed() {
        let palette = ThemeResolver.resolve(theme: theme(), matchMac: true)
        XCTAssertEqual(palette.canvas, ThemeRGB(hex: "#1E1E2E"))
        XCTAssertEqual(palette.card, ThemeRGB(hex: "#313244"))
        XCTAssertEqual(palette.accent, ThemeRGB(hex: "#CBA6F7"))
        XCTAssertEqual(palette.accentSecondary, ThemeRGB(hex: "#B4BEFE"))
        XCTAssertTrue(palette.useGradient)
        XCTAssertEqual(palette.surfaceStyle, .tintedGlass)
    }

    func testNativeKeepsTheCompanionSurfacesButTakesTheMacsAccent() {
        let native = theme(atmosphere: "native", surfaceStyle: "glass", dualTone: false,
                           canvas: "#1E1E1E", card: "#2A2A2C", accent: "#FF6B2C", accentSecondary: "#FF6B2C")
        let palette = ThemeResolver.resolve(theme: native, matchMac: true)
        XCTAssertEqual(palette.canvas, ThemePalette.default.canvas)
        XCTAssertEqual(palette.card, ThemePalette.default.card)
        XCTAssertEqual(palette.accent, ThemeRGB(hex: "#FF6B2C"))
    }

    func testNativeWithTheSystemBlueIsExactlyTheDefaultLook() {
        let native = theme(atmosphere: "native", surfaceStyle: "glass", dualTone: false,
                           canvas: "#1E1E1E", card: "#2A2A2C", accent: "#0A84FF", accentSecondary: "#0A84FF")
        XCTAssertEqual(ThemeResolver.resolve(theme: native, matchMac: true), .default)
    }

    func testABadHexFallsBackForThatColourOnly() {
        let palette = ThemeResolver.resolve(theme: theme(canvas: "nope", accentSecondary: ""), matchMac: true)
        XCTAssertEqual(palette.canvas, ThemePalette.default.canvas)
        XCTAssertEqual(palette.card, ThemeRGB(hex: "#313244"))
        XCTAssertEqual(palette.accent, ThemeRGB(hex: "#CBA6F7"))
        XCTAssertEqual(palette.accentSecondary, palette.accent, "a missing second accent repeats the first")
        XCTAssertFalse(palette.useGradient)
    }

    func testGradientOnlyWhenTheMacDrawsTwoColours() {
        XCTAssertFalse(ThemeResolver.resolve(theme: theme(dualTone: false), matchMac: true).useGradient)
        XCTAssertFalse(ThemeResolver.resolve(theme: theme(accentSecondary: "#CBA6F7"), matchMac: true).useGradient)
    }

    func testUnknownSurfaceStyleIsGlass() {
        XCTAssertEqual(ThemeResolver.resolve(theme: theme(surfaceStyle: "frosted"), matchMac: true).surfaceStyle, .glass)
        XCTAssertEqual(ThemeResolver.resolve(theme: theme(surfaceStyle: "matte"), matchMac: true).surfaceStyle, .matte)
    }

    // MARK: - Labels on accent fills

    func testLabelsOnAccentFillsStayReadable() {
        func label(_ hex: String) -> ThemeRGB {
            ThemeResolver.resolve(theme: theme(dualTone: false, accent: hex, accentSecondary: hex), matchMac: true).onAccent
        }
        XCTAssertEqual(label("#0A84FF"), .white)
        XCTAssertEqual(label("#F43F5E"), .white)
        XCTAssertEqual(label("#F5C2E7"), .nearBlack)
        XCTAssertEqual(label("#88C0D0"), .nearBlack)
    }

    func testAGradientWithOneLightStopGetsDarkLabels() {
        let palette = ThemeResolver.resolve(theme: theme(accent: "#6366F1", accentSecondary: "#F5C2E7"), matchMac: true)
        XCTAssertEqual(palette.onAccent, .nearBlack)
    }
}

final class ThemeStoreTests: XCTestCase {
    private var defaults: UserDefaults!
    private var store: ThemeStore!

    private let mocha = CompanionTheme(
        atmosphere: "catppuccin_mocha",
        atmosphereName: "Catppuccin Mocha",
        dark: CompanionThemePalette(canvas: "#1E1E2E", card: "#313244", accent: "#CBA6F7", accentSecondary: "#B4BEFE")
    )
    private let ocean = CompanionTheme(
        atmosphere: "deep_ocean",
        atmosphereName: "Deep Ocean",
        dark: CompanionThemePalette(canvas: "#0A101D", card: "#121C30", accent: "#38BDF8", accentSecondary: "#6366F1")
    )

    override func setUp() {
        defaults = UserDefaults(suiteName: "test.theme.\(UUID().uuidString)")!
        store = ThemeStore(defaults: defaults)
    }

    func testEachMacKeepsItsOwnTheme() {
        store.save(mocha, serverId: "mac-1", name: "Studio")
        store.save(ocean, serverId: "mac-2", name: "Studio")
        XCTAssertEqual(store.load(serverId: "mac-1", name: "Studio"), mocha)
        XCTAssertEqual(store.load(serverId: "mac-2", name: "Studio"), ocean)
    }

    func testMacsWithoutAnIdAreKeyedByName() {
        store.save(mocha, serverId: nil, name: "Old Mac")
        XCTAssertEqual(store.load(serverId: nil, name: "Old Mac"), mocha)
        XCTAssertNil(store.load(serverId: nil, name: "Other Mac"))
    }

    func testClearForgetsOnlyThatMac() {
        store.save(mocha, serverId: "mac-1", name: "Studio")
        store.save(ocean, serverId: "mac-2", name: "Laptop")
        store.clear(serverId: "mac-1", name: "Studio")
        XCTAssertNil(store.load(serverId: "mac-1", name: "Studio"))
        XCTAssertEqual(store.load(serverId: "mac-2", name: "Laptop"), ocean)
    }

    func testMatchMacDefaultsOnAndPersists() {
        XCTAssertTrue(store.matchMac)
        store.matchMac = false
        XCTAssertFalse(ThemeStore(defaults: defaults).matchMac)
    }

    func testCorruptDataLoadsAsNil() {
        defaults.set(Data("not json".utf8), forKey: "theme.byMac.mac-1")
        XCTAssertNil(store.load(serverId: "mac-1", name: "Studio"))
    }
}
