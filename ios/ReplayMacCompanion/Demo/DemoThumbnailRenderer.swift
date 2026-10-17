import UIKit

/// Draws the stand-in thumbnails for demo clips: a gradient tinted per clip
/// with a play glyph, as base64 JPEG like ReplayCap sends.
public enum DemoThumbnailRenderer {
    static let size = CGSize(width: 320, height: 180)

    public static func render(_ clip: CompanionClipSummary) -> String? {
        let hue = hue(for: clip.id)
        let format = UIGraphicsImageRendererFormat()
        format.scale = 1
        let image = UIGraphicsImageRenderer(size: size, format: format).image { context in
            let colors = [
                UIColor(hue: hue, saturation: 0.65, brightness: 0.55, alpha: 1).cgColor,
                UIColor(hue: (hue + 0.12).truncatingRemainder(dividingBy: 1), saturation: 0.75, brightness: 0.25, alpha: 1).cgColor
            ] as CFArray
            if let gradient = CGGradient(colorsSpace: CGColorSpaceCreateDeviceRGB(), colors: colors, locations: [0, 1]) {
                context.cgContext.drawLinearGradient(gradient, start: .zero, end: CGPoint(x: size.width, y: size.height), options: [])
            }

            let config = UIImage.SymbolConfiguration(pointSize: 44, weight: .regular)
            if let glyph = UIImage(systemName: "play.circle.fill", withConfiguration: config)?
                .withTintColor(UIColor.white.withAlphaComponent(0.85), renderingMode: .alwaysOriginal) {
                let origin = CGPoint(x: (size.width - glyph.size.width) / 2, y: (size.height - glyph.size.height) / 2)
                glyph.draw(at: origin)
            }
        }
        return image.jpegData(compressionQuality: 0.7)?.base64EncodedString()
    }

    /// The same clip always gets the same colour.
    static func hue(for clipId: String) -> CGFloat {
        let sum = clipId.unicodeScalars.reduce(0) { $0 &+ Int($1.value) &* 31 }
        return CGFloat(sum % 360) / 360
    }
}
