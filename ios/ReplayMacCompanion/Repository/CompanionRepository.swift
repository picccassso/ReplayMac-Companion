import Foundation
import Combine
import UIKit

public final class CompanionRepository: @unchecked Sendable {
    /// Where builds before Keychain storage kept the device id; cleared on launch.
    private static let legacyKeyDeviceId = "device_id"
    private static let keyLastHost = "last_host"
    private static let keyLastPort = "last_port"
    private static let keyKeepAwake = "keep_awake"
    private static let keyHasSeenOnboarding = "has_seen_onboarding"

    private let defaults: UserDefaults
    public let deviceId: String
    public let formattedDeviceName: String

    public let discoveryManager: BonjourDiscoveryManager
    public let wsClient: CompanionWebSocketClient
    public let rememberedMacStore: RememberedMacStore
    public let themeStore: ThemeStore
    public let reconnector: AutoReconnector
    public let demoSession: DemoCompanionSession
    private let secretStore: PairingSecretStoring

    private var cancellables = Set<AnyCancellable>()
    private let lock = NSLock()
    private var isForeground = false
    private var pendingThumbnailRequests = Set<String>()

    private let snapshotSubject = CurrentValueSubject<CompanionStateSnapshot, Never>(.empty)
    public var snapshot: AnyPublisher<CompanionStateSnapshot, Never> {
        snapshotSubject.eraseToAnyPublisher()
    }
    public var currentSnapshot: CompanionStateSnapshot {
        snapshotSubject.value
    }

    private let thumbnailsSubject = CurrentValueSubject<[String: String], Never>([:])
    public var thumbnails: AnyPublisher<[String: String], Never> {
        thumbnailsSubject.eraseToAnyPublisher()
    }
    public var currentThumbnails: [String: String] {
        thumbnailsSubject.value
    }

    private let keepScreenAwakeSubject: CurrentValueSubject<Bool, Never>
    public var keepScreenAwake: AnyPublisher<Bool, Never> {
        keepScreenAwakeSubject.eraseToAnyPublisher()
    }
    public var currentKeepScreenAwake: Bool {
        keepScreenAwakeSubject.value
    }

    private let hasSeenOnboardingSubject: CurrentValueSubject<Bool, Never>
    public var hasSeenOnboarding: AnyPublisher<Bool, Never> {
        hasSeenOnboardingSubject.eraseToAnyPublisher()
    }
    public var currentHasSeenOnboarding: Bool {
        hasSeenOnboardingSubject.value
    }

    private let isDemoActiveSubject = CurrentValueSubject<Bool, Never>(false)
    /// Whether the in-memory demo Mac stands in for ReplayCap.
    public var isDemoActive: AnyPublisher<Bool, Never> {
        isDemoActiveSubject.eraseToAnyPublisher()
    }
    public var currentIsDemoActive: Bool {
        isDemoActiveSubject.value
    }

    private let macThemeSubject: CurrentValueSubject<CompanionTheme?, Never>
    /// The theme of the Mac on screen, or of the remembered Mac before one connects.
    /// Nil when that Mac doesn't send one.
    public var macTheme: AnyPublisher<CompanionTheme?, Never> {
        macThemeSubject.eraseToAnyPublisher()
    }
    public var currentMacTheme: CompanionTheme? {
        macThemeSubject.value
    }

    private let matchMacThemeSubject: CurrentValueSubject<Bool, Never>
    public var matchMacTheme: AnyPublisher<Bool, Never> {
        matchMacThemeSubject.eraseToAnyPublisher()
    }
    public var currentMatchMacTheme: Bool {
        matchMacThemeSubject.value
    }

    /// The colours to draw with. The demo always shows its own theme.
    public var palette: AnyPublisher<ThemePalette, Never> {
        Publishers.CombineLatest3(macThemeSubject, matchMacThemeSubject, isDemoActiveSubject)
            .map { theme, matchMac, isDemo in
                ThemeResolver.resolve(theme: theme, matchMac: matchMac || isDemo)
            }
            .removeDuplicates()
            .eraseToAnyPublisher()
    }

    /// Where commands go: the demo Mac while it runs, otherwise the socket.
    private var activeLink: CompanionLink {
        isDemoActiveSubject.value ? demoSession : wsClient
    }

    public var discoveredMacs: AnyPublisher<[DiscoveredMac], Never> {
        discoveryManager.discoveredMacs
    }

    public var isDiscovering: AnyPublisher<Bool, Never> {
        discoveryManager.isDiscovering
    }

    public var localNetworkDenied: AnyPublisher<Bool, Never> {
        discoveryManager.localNetworkDenied
    }

    private let previouslyConnectedMacsSubject: CurrentValueSubject<[RememberedMac], Never>
    public var previouslyConnectedMacs: AnyPublisher<[RememberedMac], Never> {
        previouslyConnectedMacsSubject.eraseToAnyPublisher()
    }
    public var currentPreviouslyConnectedMacs: [RememberedMac] {
        previouslyConnectedMacsSubject.value
    }

    /// The socket's state, shown as `.reconnecting` while the remembered Mac is
    /// being found, or the demo Mac's while the demo runs.
    public var connectionState: AnyPublisher<ConnectionState, Never> {
        Publishers.CombineLatest3(wsClient.connectionState, reconnector.target, demoSession.connectionState)
            .map { state, target, demoState in
                if demoState.isConnected {
                    return demoState
                }
                let settled: Bool
                switch state {
                case .connected, .pendingApproval:
                    settled = true
                default:
                    settled = false
                }
                if let target = target, !settled {
                    return ConnectionState.reconnecting(serverName: target.name)
                }
                return state
            }
            .eraseToAnyPublisher()
    }

    public init(
        defaults: UserDefaults = .standard,
        secretStore: PairingSecretStoring = KeychainPairingSecretStore(),
        deviceId: String = DeviceIdentity.loadOrCreate(),
        demoSession: DemoCompanionSession = DemoCompanionSession()
    ) {
        self.defaults = defaults
        self.secretStore = secretStore
        self.deviceId = deviceId
        self.demoSession = demoSession
        // A UserDefaults copy would travel with backups to other phones.
        defaults.removeObject(forKey: Self.legacyKeyDeviceId)

        let deviceName = UIDevice.current.name.trimmingCharacters(in: .whitespacesAndNewlines)
        self.formattedDeviceName = deviceName.isEmpty ? "iOS Device" : deviceName

        let disc = BonjourDiscoveryManager()
        self.discoveryManager = disc

        let ws = CompanionWebSocketClient(deviceId: self.deviceId, deviceName: self.formattedDeviceName, secretStore: secretStore)
        self.wsClient = ws

        let store = RememberedMacStore(defaults: defaults)
        self.rememberedMacStore = store
        self.previouslyConnectedMacsSubject = CurrentValueSubject<[RememberedMac], Never>(store.loadAll())

        let themes = ThemeStore(defaults: defaults)
        self.themeStore = themes
        self.macThemeSubject = CurrentValueSubject<CompanionTheme?, Never>(store.loadLatest().flatMap(themes.load(for:)))
        self.matchMacThemeSubject = CurrentValueSubject<Bool, Never>(themes.matchMac)

        let savedAwake = defaults.bool(forKey: Self.keyKeepAwake)
        self.keepScreenAwakeSubject = CurrentValueSubject<Bool, Never>(savedAwake)

        let savedSeen = defaults.bool(forKey: Self.keyHasSeenOnboarding)
        self.hasSeenOnboardingSubject = CurrentValueSubject<Bool, Never>(savedSeen)

        let snapSubject = self.snapshotSubject
        let prevSubject = self.previouslyConnectedMacsSubject
        let themeSubject = self.macThemeSubject
        let recon = AutoReconnector(
            connectionState: ws.connectionState,
            discoveredMacs: disc.discoveredMacs,
            getCurrentDiscoveredMacs: { disc.currentDiscoveredMacs },
            connect: { [weak ws, defaults, weak snapSubject] endpoint in
                if !endpoint.isUsb {
                    defaults.set(endpoint.host, forKey: Self.keyLastHost)
                    defaults.set(endpoint.port, forKey: Self.keyLastPort)
                }
                snapSubject?.send(.empty)
                ws?.connect(host: endpoint.host, port: endpoint.port)
            },
            onPairingDenied: { [store, themes, weak prevSubject, weak themeSubject] in
                store.loadAll().forEach(themes.clear(for:))
                store.clear()
                prevSubject?.send([])
                themeSubject?.send(nil)
            }
        )
        self.reconnector = recon

        setupSubscriptions()
        updateIdleTimer(savedAwake)
    }

    private func setupSubscriptions() {
        // Handle incoming server events
        wsClient.events
            .sink { [weak self] event in
                self?.handleServerEvent(event)
            }
            .store(in: &cancellables)

        // The demo Mac speaks the same events. Its state stays out of the
        // socket handling below, so it never becomes a remembered Mac.
        demoSession.events
            .sink { [weak self] event in
                self?.handleServerEvent(event)
            }
            .store(in: &cancellables)

        // Handle socket state changes
        wsClient.connectionState
            .sink { [weak self] state in
                self?.onSocketStateChanged(state)
            }
            .store(in: &cancellables)
    }

    public func getLastConnectedHost() -> String? {
        defaults.string(forKey: Self.keyLastHost)
    }

    public func getLastConnectedPort() -> Int {
        let port = defaults.integer(forKey: Self.keyLastPort)
        return port > 0 ? port : CompanionWebSocketClient.defaultPort
    }

    public func setKeepScreenAwake(_ enabled: Bool) {
        keepScreenAwakeSubject.send(enabled)
        defaults.set(enabled, forKey: Self.keyKeepAwake)
        updateIdleTimer(enabled)
    }

    public func setHasSeenOnboarding(_ seen: Bool) {
        hasSeenOnboardingSubject.send(seen)
        defaults.set(seen, forKey: Self.keyHasSeenOnboarding)
    }

    public func setMatchMacTheme(_ match: Bool) {
        themeStore.matchMac = match
        matchMacThemeSubject.send(match)
    }

    /// Re-themes the demo Mac. Does nothing outside the demo.
    public func selectDemoAtmosphere(_ atmosphereId: String) {
        guard isDemoActiveSubject.value else { return }
        demoSession.selectAtmosphere(atmosphereId)
    }

    public func stopDiscovery() {
        discoveryManager.stopDiscovery()
    }

    public func restartDiscovery() {
        discoveryManager.restartDiscovery()
    }

    public func addManualTarget(name: String, host: String, port: Int) {
        discoveryManager.addManualTarget(name: name, host: host, port: port)
    }

    public func connect(host: String, port: Int = CompanionWebSocketClient.defaultPort) {
        reconnector.stop()
        stopDemo()
        openSocket(MacEndpoint(host: host, port: port))
    }

    /// Ends the socket or the demo, back to the Connect screen.
    public func disconnect() {
        reconnector.stop()
        wsClient.disconnect()
        stopDemo()
        clearMacState()
        showRememberedMacTheme()
    }

    /// Swaps any Mac for the in-memory demo one. Nothing reaches the network.
    public func startDemo() {
        reconnector.stop()
        wsClient.disconnect()
        clearMacState()
        isDemoActiveSubject.send(true)
        demoSession.start()
        updateDiscovery()
    }

    private func stopDemo() {
        guard isDemoActiveSubject.value else { return }
        demoSession.stop()
        isDemoActiveSubject.send(false)
        showRememberedMacTheme()
        updateDiscovery()
    }

    /// Back to the look of the Mac the app would reconnect to.
    private func showRememberedMacTheme() {
        macThemeSubject.send(rememberedMacStore.loadLatest().flatMap(themeStore.load(for:)))
    }

    /// Shows the theme a snapshot carries and, from a real Mac, remembers it.
    private func adoptTheme(from snapshot: CompanionStateSnapshot) {
        macThemeSubject.send(snapshot.theme)
        guard !isDemoActiveSubject.value,
              let theme = snapshot.theme,
              case .connected(let serverName, _, _, _, let serverId, _) = wsClient.currentConnectionState else {
            return
        }
        themeStore.save(theme, serverId: serverId, name: serverName)
    }

    private func clearMacState() {
        snapshotSubject.send(.empty)
        lock.lock()
        pendingThumbnailRequests.removeAll()
        lock.unlock()
        thumbnailsSubject.send([:])
    }

    public func forgetMac(_ mac: RememberedMac) {
        if let serverId = mac.serverId {
            secretStore.remove(forServerId: serverId)
        }
        rememberedMacStore.remove(mac)
        themeStore.clear(for: mac)
        previouslyConnectedMacsSubject.send(rememberedMacStore.loadAll())
        if !isDemoActiveSubject.value, !wsClient.currentConnectionState.isConnected {
            showRememberedMacTheme()
        }
    }

    public func clearError() {
        wsClient.clearError()
    }

    public func onAppForegrounded() {
        lock.lock()
        isForeground = true
        lock.unlock()

        if isDemoActiveSubject.value {
            _ = demoSession.resync()
            return
        }

        // Picks discovery back up if it failed while the app was away,
        // e.g. after the user granted Local Network access in Settings.
        updateDiscovery()

        switch wsClient.currentConnectionState {
        case .connected:
            _ = wsClient.resync()
        case .disconnected, .failed:
            reconnectToRememberedMac()
        default:
            break
        }
    }

    public func onAppBackgrounded() {
        lock.lock()
        isForeground = false
        lock.unlock()

        if reconnector.currentTarget != nil {
            reconnector.stop()
            wsClient.disconnect()
        }
    }

    private func reconnectToRememberedMac() {
        guard let mac = rememberedMacStore.load() else { return }
        lock.lock()
        let shouldStart = isForeground
        lock.unlock()
        if shouldStart {
            reconnector.start(mac)
        }
    }

    private func onSocketStateChanged(_ state: ConnectionState) {
        switch state {
        case .connected(let serverName, _, let host, let port, let serverId, _):
            rememberConnectedMac(serverName: serverName, host: host, port: port, serverId: serverId)
        case .failed(_, _, _, let wasConnected, let pairingDenied):
            if pairingDenied {
                if let target = reconnector.currentTarget {
                    rememberedMacStore.remove(target)
                    themeStore.clear(for: target)
                } else {
                    rememberedMacStore.loadAll().forEach(themeStore.clear(for:))
                    rememberedMacStore.clear()
                }
                previouslyConnectedMacsSubject.send(rememberedMacStore.loadAll())
                showRememberedMacTheme()
            } else if wasConnected {
                reconnectToRememberedMac()
            }
        default:
            break
        }
        updateDiscovery()
    }

    /// Discovery only serves the Connect screen and reconnects, so it stops while connected or in the demo.
    private func updateDiscovery() {
        if case .connected = wsClient.currentConnectionState {
            discoveryManager.stopDiscovery()
        } else if isDemoActiveSubject.value {
            discoveryManager.stopDiscovery()
        } else {
            discoveryManager.startDiscovery()
        }
    }

    private func rememberConnectedMac(serverName: String, host: String, port: Int, serverId: String?) {
        let endpoint = MacEndpoint(host: host, port: port)
        let prev = rememberedMacStore.load()
        let updated = RememberedMac.afterConnecting(
            previous: prev,
            serverId: serverId,
            serverName: serverName,
            endpoint: endpoint
        )
        rememberedMacStore.save(updated)
        previouslyConnectedMacsSubject.send(rememberedMacStore.loadAll())
    }

    private func openSocket(_ endpoint: MacEndpoint) {
        if !endpoint.isUsb {
            defaults.set(endpoint.host, forKey: Self.keyLastHost)
            defaults.set(endpoint.port, forKey: Self.keyLastPort)
        }
        snapshotSubject.send(.empty)
        wsClient.connect(host: endpoint.host, port: endpoint.port)
    }

    public func saveReplay() {
        _ = activeLink.sendAction(CompanionActions.saveReplay)
    }

    public func saveExtendedReplay() {
        _ = activeLink.sendAction(CompanionActions.saveExtendedReplay)
    }

    public func toggleBuffer() {
        _ = activeLink.sendAction(CompanionActions.toggleBuffer)
    }

    public func toggleSession() {
        _ = activeLink.sendAction(CompanionActions.toggleSession)
    }

    public func toggleMicrophone() {
        _ = activeLink.sendAction(CompanionActions.toggleMicrophone)
    }

    public func toggleSystemAudio() {
        _ = activeLink.sendAction(CompanionActions.toggleSystemAudio)
    }

    public func revealClip(_ clipId: String) {
        _ = activeLink.revealClip(clipId)
    }

    public func deleteClip(_ clipId: String) {
        _ = activeLink.deleteClip(clipId)
    }

    public func trimClip(_ clipId: String) {
        _ = activeLink.trimClip(clipId)
    }

    public func copyClip(_ clipId: String) {
        _ = activeLink.copyClip(clipId)
    }

    public func setMicrophoneVolume(_ volume: Double) {
        _ = activeLink.setVolume(target: "microphone", volume: volume)
    }

    public func setSystemAudioVolume(_ volume: Double) {
        _ = activeLink.setVolume(target: "systemAudio", volume: volume)
    }

    public func toggleAppMute(_ bundleId: String) {
        _ = activeLink.toggleAppMute(bundleId)
    }

    public func unmuteAllApps() {
        _ = activeLink.unmuteAllApps()
    }

    public func selectDisplay(_ displayId: String) {
        _ = activeLink.selectDisplay(displayId)
    }

    public func applyProfile(_ profileId: String) {
        _ = activeLink.applyProfile(profileId)
    }

    public func requestThumbnail(_ clipId: String) {
        lock.lock()
        let alreadyHas = thumbnailsSubject.value.keys.contains(clipId)
        let newlyInserted = pendingThumbnailRequests.insert(clipId).inserted
        lock.unlock()

        if !alreadyHas && newlyInserted {
            _ = activeLink.getThumbnail(clipId)
        }
    }

    private func handleServerEvent(_ event: CompanionServerEvent) {
        switch event {
        case .snapshot(let snap), .stateChanged(let snap):
            adoptTheme(from: snap)
            snapshotSubject.send(snap)
            pruneThumbnails(snap)
            fetchMissingThumbnails(snap)

        case .telemetry(let tick):
            var current = snapshotSubject.value
            current = CompanionStateSnapshot(
                type: current.type,
                isRecording: current.isRecording,
                isSessionRecording: current.isSessionRecording,
                saveStatus: current.saveStatus,
                recordingElapsedSeconds: tick.recordingElapsedSeconds,
                extendedBufferElapsedSeconds: tick.extendedBufferElapsedSeconds,
                sessionElapsedSeconds: tick.sessionElapsedSeconds,
                bufferedSeconds: tick.bufferedSeconds,
                bufferDurationSeconds: current.bufferDurationSeconds,
                bufferMemoryBytes: tick.bufferMemoryBytes,
                bufferMemoryFormatted: tick.bufferMemoryFormatted,
                longBufferEnabled: current.longBufferEnabled,
                longBufferDurationSeconds: current.longBufferDurationSeconds,
                capturedDisplayName: current.capturedDisplayName,
                isUsingFallbackDisplay: current.isUsingFallbackDisplay,
                isMicrophoneEnabled: current.isMicrophoneEnabled,
                isMicrophoneMuted: current.isMicrophoneMuted,
                isSystemAudioEnabled: current.isSystemAudioEnabled,
                isSystemAudioMuted: current.isSystemAudioMuted,
                microphoneVolume: current.microphoneVolume,
                systemAudioVolume: current.systemAudioVolume,
                perAppAudioEnabled: current.perAppAudioEnabled,
                audioApps: current.audioApps,
                displays: current.displays,
                profiles: current.profiles,
                recentClips: current.recentClips,
                theme: current.theme
            )
            snapshotSubject.send(current)

        case .thumbnail(let clipId, let base64):
            lock.lock()
            pendingThumbnailRequests.remove(clipId)
            let updatedThumbnails: [String: String]?
            if let base64 = base64 {
                var map = thumbnailsSubject.value
                map[clipId] = base64
                updatedThumbnails = map
            } else {
                updatedThumbnails = nil
            }
            lock.unlock()

            if let map = updatedThumbnails {
                thumbnailsSubject.send(map)
            }

        default:
            break
        }
    }

    private func pruneThumbnails(_ snapshot: CompanionStateSnapshot) {
        let activeIds = Set(snapshot.recentClips.map { $0.id })
        lock.lock()
        let current = thumbnailsSubject.value
        let filtered = current.filter { activeIds.contains($0.key) }
        lock.unlock()

        if filtered.count != current.count {
            thumbnailsSubject.send(filtered)
        }
    }

    private func fetchMissingThumbnails(_ snapshot: CompanionStateSnapshot) {
        for clip in snapshot.recentClips where clip.hasThumbnail {
            requestThumbnail(clip.id)
        }
    }

    private func updateIdleTimer(_ disabled: Bool) {
        Task { @MainActor in
            UIApplication.shared.isIdleTimerDisabled = disabled
        }
    }
}
