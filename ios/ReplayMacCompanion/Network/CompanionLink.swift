import Foundation
import Combine

/// What the repository drives: ReplayCap over the socket, or the in-memory
/// demo Mac. Each command returns whether it was sent.
public protocol CompanionLink: AnyObject {
    var events: AnyPublisher<CompanionServerEvent, Never> { get }

    func resync() -> Bool
    func sendAction(_ actionName: String) -> Bool
    func getThumbnail(_ clipId: String) -> Bool
    func revealClip(_ clipId: String) -> Bool
    func deleteClip(_ clipId: String) -> Bool
    func trimClip(_ clipId: String) -> Bool
    func copyClip(_ clipId: String) -> Bool
    func setVolume(target: String, volume: Double) -> Bool
    func toggleAppMute(_ bundleId: String) -> Bool
    func unmuteAllApps() -> Bool
    func selectDisplay(_ displayId: String) -> Bool
    func applyProfile(_ profileId: String) -> Bool
}

extension CompanionWebSocketClient: CompanionLink {}
