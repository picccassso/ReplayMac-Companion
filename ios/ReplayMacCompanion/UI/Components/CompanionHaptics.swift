import Foundation
import UIKit

public enum SaveHapticOutcome: Sendable, Equatable {
    case success
    case failure
}

public enum CompanionHaptics {
    /// Determines whether a save status change warrants a completion haptic.
    public static func saveTransitionOutcome(previousStatus: String, newStatus: String) -> SaveHapticOutcome? {
        let wasSaving = previousStatus.caseInsensitiveCompare("saving") == .orderedSame ||
            previousStatus.caseInsensitiveCompare("busy") == .orderedSame
        guard wasSaving else { return nil }

        if newStatus.caseInsensitiveCompare("saved") == .orderedSame {
            return .success
        }
        if newStatus.caseInsensitiveCompare("failed") == .orderedSame {
            return .failure
        }
        return nil
    }

    @MainActor
    public static func tap() {
        let generator = UIImpactFeedbackGenerator(style: .light)
        generator.impactOccurred()
    }

    @MainActor
    public static func performOutcome(_ outcome: SaveHapticOutcome) {
        let generator = UINotificationFeedbackGenerator()
        switch outcome {
        case .success:
            generator.notificationOccurred(.success)
        case .failure:
            generator.notificationOccurred(.error)
        }
    }
}
