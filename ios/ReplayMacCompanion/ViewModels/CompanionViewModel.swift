import Foundation
import Combine
import UIKit

@MainActor
public final class CompanionViewModel: ObservableObject {
    public let repository: CompanionRepository

    @Published public var connectionState: ConnectionState = .disconnected
    @Published public var snapshot: CompanionStateSnapshot = .empty
    @Published public var thumbnails: [String: String] = [:]
    @Published public var discoveredMacs: [DiscoveredMac] = []
    @Published public var previouslyConnectedItems: [PreviouslyConnectedItem] = []
    @Published public var filteredDiscoveredMacs: [DiscoveredMac] = []
    @Published public var isDiscovering: Bool = false
    @Published public var keepScreenAwake: Bool = false
    @Published public var hasSeenOnboarding: Bool = false
    @Published public var showTutorialDialog: Bool = false
    @Published public var selectedClip: CompanionClipSummary? = nil
    @Published public var manualHost: String = ""
    @Published public var manualPort: String = "7375"

    private var cancellables = Set<AnyCancellable>()

    public init(repository: CompanionRepository) {
        self.repository = repository

        self.manualHost = repository.getLastConnectedHost() ?? ""
        self.manualPort = String(repository.getLastConnectedPort())
        self.keepScreenAwake = repository.currentKeepScreenAwake
        self.hasSeenOnboarding = repository.currentHasSeenOnboarding
        self.showTutorialDialog = !repository.currentHasSeenOnboarding

        bindRepository()
        bindAppLifecycle()

        repository.startDiscovery()
    }

    private func bindRepository() {
        repository.connectionState
            .receive(on: DispatchQueue.main)
            .assign(to: &$connectionState)

        repository.snapshot
            .receive(on: DispatchQueue.main)
            .assign(to: &$snapshot)

        repository.thumbnails
            .receive(on: DispatchQueue.main)
            .assign(to: &$thumbnails)

        repository.discoveredMacs
            .receive(on: DispatchQueue.main)
            .assign(to: &$discoveredMacs)

        Publishers.CombineLatest(repository.previouslyConnectedMacs, repository.discoveredMacs)
            .receive(on: DispatchQueue.main)
            .sink { [weak self] savedMacs, discovered in
                guard let self = self else { return }

                self.previouslyConnectedItems = savedMacs.map { mac in
                    let onlineMatch = discovered.first { mac.isSameMacAs($0) }
                    let onlineEndpoint = onlineMatch.map { MacEndpoint(host: $0.host, port: $0.port) }
                    return PreviouslyConnectedItem(
                        mac: mac,
                        isOnline: onlineMatch != nil,
                        onlineEndpoint: onlineEndpoint
                    )
                }

                self.filteredDiscoveredMacs = discovered.filter { disc in
                    !savedMacs.contains { $0.isSameMacAs(disc) }
                }
            }
            .store(in: &cancellables)

        repository.isDiscovering
            .receive(on: DispatchQueue.main)
            .assign(to: &$isDiscovering)

        repository.keepScreenAwake
            .receive(on: DispatchQueue.main)
            .assign(to: &$keepScreenAwake)

        repository.hasSeenOnboarding
            .receive(on: DispatchQueue.main)
            .assign(to: &$hasSeenOnboarding)
    }

    private func bindAppLifecycle() {
        NotificationCenter.default.publisher(for: UIApplication.willEnterForegroundNotification)
            .sink { [weak self] _ in
                self?.repository.onAppForegrounded()
            }
            .store(in: &cancellables)

        NotificationCenter.default.publisher(for: UIApplication.didEnterBackgroundNotification)
            .sink { [weak self] _ in
                self?.repository.onAppBackgrounded()
            }
            .store(in: &cancellables)
    }

    public func openTutorial() {
        showTutorialDialog = true
    }

    public func dismissTutorial() {
        showTutorialDialog = false
        repository.setHasSeenOnboarding(true)
    }

    public func setManualHost(_ host: String) {
        manualHost = host
    }

    public func setManualPort(_ port: String) {
        manualPort = port
    }

    public func dismissError() {
        repository.clearError()
    }

    public func refreshDiscovery() {
        repository.clearError()
        repository.restartDiscovery()
    }

    public func connect(host: String, portStr: String) {
        let port = Int(portStr.trimmingCharacters(in: .whitespacesAndNewlines))
            .flatMap { (1...65535).contains($0) ? $0 : nil } ?? CompanionWebSocketClient.defaultPort
        let trimmedHost = host.trimmingCharacters(in: .whitespacesAndNewlines)
        manualHost = trimmedHost
        manualPort = String(port)
        repository.connect(host: trimmedHost, port: port)
    }

    public func connectWired(host: String = "127.0.0.1", portStr: String = "7375") {
        let port = Int(portStr) ?? 7375
        manualHost = host
        manualPort = String(port)
        repository.connect(host: host, port: port)
    }

    public func connectRememberedMac(_ item: PreviouslyConnectedItem) {
        if let endpoint = item.onlineEndpoint ?? item.mac.networkEndpoint {
            connect(host: endpoint.host, portStr: String(endpoint.port))
        }
    }

    public func forgetMac(_ mac: RememberedMac) {
        repository.forgetMac(mac)
    }

    public func disconnect() {
        repository.disconnect()
    }

    public func saveReplay() {
        repository.saveReplay()
    }

    public func saveExtendedReplay() {
        repository.saveExtendedReplay()
    }

    public func toggleBuffer() {
        repository.toggleBuffer()
    }

    public func toggleSession() {
        repository.toggleSession()
    }

    public func toggleMicrophone() {
        repository.toggleMicrophone()
    }

    public func toggleSystemAudio() {
        repository.toggleSystemAudio()
    }

    public func selectClip(_ clip: CompanionClipSummary?) {
        selectedClip = clip
        if let clip = clip {
            repository.requestThumbnail(clip.id)
        }
    }

    public func revealClip(_ clipId: String) {
        repository.revealClip(clipId)
    }

    public func deleteClip(_ clipId: String) {
        repository.deleteClip(clipId)
        if selectedClip?.id == clipId {
            selectedClip = nil
        }
    }

    public func setKeepScreenAwake(_ enabled: Bool) {
        repository.setKeepScreenAwake(enabled)
    }
}
