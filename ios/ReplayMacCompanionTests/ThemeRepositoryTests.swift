import XCTest
import Combine
@testable import ReplayCapCompanion

/// The Mac's theme through the repository: shown, remembered per Mac, kept
/// across telemetry, and the demo's own atmospheres.
final class ThemeRepositoryTests: XCTestCase {
    private var defaults: UserDefaults!
    private var server: FakeMacServer?
    private var repository: CompanionRepository!
    private var cancellables = Set<AnyCancellable>()

    private static let mochaJSON = ##"{"atmosphere":"catppuccin_mocha","atmosphereName":"Catppuccin Mocha","colorScheme":"dark","accentMode":"atmosphere","surfaceStyle":"tinted_glass","dualTone":true,"dark":{"canvas":"#1E1E2E","card":"#313244","accent":"#CBA6F7","accentSecondary":"#B4BEFE"},"light":{"canvas":"#EFF1F5","card":"#CCD0DA","accent":"#CBA6F7","accentSecondary":"#B4BEFE"}}"##
    private let mocha = CompanionTheme(
        atmosphere: "catppuccin_mocha",
        atmosphereName: "Catppuccin Mocha",
        colorScheme: "dark",
        accentMode: "atmosphere",
        surfaceStyle: "tinted_glass",
        dualTone: true,
        dark: CompanionThemePalette(canvas: "#1E1E2E", card: "#313244", accent: "#CBA6F7", accentSecondary: "#B4BEFE"),
        light: CompanionThemePalette(canvas: "#EFF1F5", card: "#CCD0DA", accent: "#CBA6F7", accentSecondary: "#B4BEFE")
    )
    private let studio = RememberedMac(serverId: "mac-1", name: "Studio", networkEndpoint: nil, usbPort: nil, lastUsedUsb: false)

    override func setUp() {
        defaults = UserDefaults(suiteName: "test.theme-repo.\(UUID().uuidString)")!
    }

    override func tearDown() {
        repository?.disconnect()
        repository?.stopDiscovery()
        server?.stop()
        cancellables.removeAll()
    }

    private func makeRepository() -> CompanionRepository {
        CompanionRepository(defaults: defaults, secretStore: InMemorySecretStore(), deviceId: "phone-1")
    }

    /// Connects to a fake Mac that approves the phone and then sends `messages`.
    private func connect(sending messages: [String]) throws {
        server = try FakeMacServer { message in
            guard message["type"] as? String == "hello" else { return [] }
            let approval = #"{"type":"auth_response","status":"approved","serverName":"Studio","serverVersion":"1.8","serverId":"mac-1","pairingSecret":"\#(Data(repeating: 4, count: 32).base64EncodedString())"}"#
            return ([approval] + messages).map { .send($0) }
        }
        repository = makeRepository()
        repository.connect(host: "127.0.0.1", port: server!.port)
    }

    private func waitForSnapshot(_ predicate: @escaping (CompanionStateSnapshot) -> Bool) {
        let exp = expectation(description: "snapshot")
        exp.assertForOverFulfill = false
        repository.snapshot
            .filter(predicate)
            .sink { _ in exp.fulfill() }
            .store(in: &cancellables)
        wait(for: [exp], timeout: 5)
    }

    func testAMacsThemeIsShownAndRememberedForThatMac() throws {
        try connect(sending: [#"{"type":"state_snapshot","isRecording":true,"theme":\#(Self.mochaJSON)}"#])
        waitForSnapshot { $0.isRecording }

        XCTAssertEqual(repository.currentMacTheme, mocha)
        XCTAssertEqual(ThemeStore(defaults: defaults).load(serverId: "mac-1", name: "Studio"), mocha)
    }

    func testTelemetryKeepsTheTheme() throws {
        try connect(sending: [
            #"{"type":"state_snapshot","isRecording":true,"theme":\#(Self.mochaJSON)}"#,
            #"{"type":"telemetry_tick","bufferedSeconds":12}"#
        ])
        waitForSnapshot { $0.bufferedSeconds == 12 }

        XCTAssertEqual(repository.currentSnapshot.theme, mocha)
        XCTAssertEqual(repository.currentMacTheme, mocha)
    }

    func testAMacWithoutAThemeShowsTheDefaultLookButKeepsTheRememberedOne() throws {
        RememberedMacStore(defaults: defaults).save(studio)
        ThemeStore(defaults: defaults).save(mocha, serverId: "mac-1", name: "Studio")

        try connect(sending: [#"{"type":"state_snapshot","isRecording":true}"#])
        waitForSnapshot { $0.isRecording }

        XCTAssertNil(repository.currentMacTheme)
        XCTAssertEqual(ThemeStore(defaults: defaults).load(serverId: "mac-1", name: "Studio"), mocha)
    }

    func testTheRememberedMacsThemeShowsBeforeItConnects() {
        RememberedMacStore(defaults: defaults).save(studio)
        ThemeStore(defaults: defaults).save(mocha, serverId: "mac-1", name: "Studio")

        repository = makeRepository()

        XCTAssertEqual(repository.currentMacTheme, mocha)
        var palette: ThemePalette?
        repository.palette.sink { palette = $0 }.store(in: &cancellables)
        XCTAssertEqual(palette?.canvas, ThemeRGB(hex: "#1E1E2E"))
    }

    func testForgettingAMacForgetsItsTheme() {
        RememberedMacStore(defaults: defaults).save(studio)
        ThemeStore(defaults: defaults).save(mocha, serverId: "mac-1", name: "Studio")
        repository = makeRepository()

        repository.forgetMac(studio)

        XCTAssertNil(ThemeStore(defaults: defaults).load(serverId: "mac-1", name: "Studio"))
        XCTAssertNil(repository.currentMacTheme)
    }

    func testTurningOffMatchMacShowsTheDefaultLookAndIsRemembered() {
        RememberedMacStore(defaults: defaults).save(studio)
        ThemeStore(defaults: defaults).save(mocha, serverId: "mac-1", name: "Studio")
        repository = makeRepository()
        var palette: ThemePalette?
        repository.palette.sink { palette = $0 }.store(in: &cancellables)

        repository.setMatchMacTheme(false)

        XCTAssertEqual(palette, .default)
        XCTAssertFalse(makeRepository().currentMatchMacTheme)
    }

    // MARK: - Demo

    func testPickingADemoAtmosphereRethemesTheDemo() {
        repository = makeRepository()
        var palette: ThemePalette?
        repository.palette.sink { palette = $0 }.store(in: &cancellables)
        repository.startDemo()

        repository.selectDemoAtmosphere("deep_ocean")

        XCTAssertEqual(repository.currentMacTheme?.atmosphere, "deep_ocean")
        XCTAssertEqual(palette?.canvas, ThemeRGB(hex: "#0A101D"))
        XCTAssertEqual(palette?.accent, ThemeRGB(hex: "#38BDF8"))
        XCTAssertTrue(palette?.useGradient ?? false)
    }

    func testTheDemoIgnoresMatchMacAndLeavesNoThemeBehind() {
        RememberedMacStore(defaults: defaults).save(studio)
        ThemeStore(defaults: defaults).save(mocha, serverId: "mac-1", name: "Studio")
        repository = makeRepository()
        repository.setMatchMacTheme(false)
        var palette: ThemePalette?
        repository.palette.sink { palette = $0 }.store(in: &cancellables)

        repository.startDemo()
        repository.selectDemoAtmosphere("arena_boost")
        XCTAssertEqual(palette?.accent, ThemeRGB(hex: "#F43F5E"), "the demo always shows its own theme")

        repository.disconnect()
        XCTAssertEqual(repository.currentMacTheme, mocha, "back to the remembered Mac's look")
        XCTAssertEqual(ThemeStore(defaults: defaults).load(serverId: "mac-1", name: "Studio"), mocha)
    }
}
