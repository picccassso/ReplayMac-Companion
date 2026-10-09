import SwiftUI
import UIKit

public func formatFileSize(_ bytes: Int64?) -> String {
    guard let bytes = bytes, bytes > 0 else { return "" }
    let mb = Double(bytes) / (1024.0 * 1024.0)
    let posix = Locale(identifier: "en_US_POSIX")
    if mb >= 1000.0 {
        return String(format: "%.1f GB", locale: posix, mb / 1024.0)
    } else if mb >= 1.0 {
        return String(format: "%.1f MB", locale: posix, mb)
    } else {
        return "\(bytes / 1024) KB"
    }
}

public func formatSavedTime(_ epochSeconds: Double) -> String {
    guard epochSeconds > 0 else { return "" }
    let date = Date(timeIntervalSince1970: epochSeconds)
    let formatter = DateFormatter()
    formatter.dateFormat = "HH:mm"
    return formatter.string(from: date)
}

public func clipKindTitle(_ kind: String) -> String {
    let lower = kind.lowercased().trimmingCharacters(in: .whitespacesAndNewlines)
    switch lower {
    case "replay":
        return "Replay"
    case "session":
        return "Session"
    case "extended", "extended replay":
        return "Extended Replay"
    default:
        return kind.prefix(1).uppercased() + kind.dropFirst()
    }
}

/// "Replay · 00:30 · 24.1 MB · Saved 14:02", the desktop's recent-clip summary.
public func clipDetailLine(_ clip: CompanionClipSummary) -> String {
    var parts: [String] = []
    parts.append(clipKindTitle(clip.kind))
    if let duration = clip.duration, duration > 0 {
        parts.append(formatDurationSeconds(duration))
    }
    let sizeStr = formatFileSize(clip.fileSize)
    if !sizeStr.isEmpty {
        parts.append(sizeStr)
    }
    if clip.savedAt > 0 {
        parts.append("Saved \(formatSavedTime(clip.savedAt))")
    }
    return parts.joined(separator: " · ")
}

public struct RecentClipItem: View {
    public var clip: CompanionClipSummary
    public var thumbnailBase64: String?
    public var onClick: () -> Void

    public init(
        clip: CompanionClipSummary,
        thumbnailBase64: String?,
        onClick: @escaping () -> Void
    ) {
        self.clip = clip
        self.thumbnailBase64 = thumbnailBase64
        self.onClick = onClick
    }

    private var thumbnailImage: UIImage? {
        guard let base64 = thumbnailBase64, !base64.isEmpty,
              let data = Data(base64Encoded: base64) else { return nil }
        return UIImage(data: data)
    }

    public var body: some View {
        Button(action: onClick) {
            HStack(spacing: 12) {
                // 16:9 Thumbnail (96x54)
                ZStack {
                    RoundedRectangle(cornerRadius: 10, style: .continuous)
                        .fill(Color.black.opacity(0.45))

                    if let uiImage = thumbnailImage {
                        Image(uiImage: uiImage)
                            .resizable()
                            .aspectRatio(contentMode: .fill)
                            .frame(width: 96, height: 54)
                            .clipped()
                    } else {
                        Image(systemName: "film")
                            .font(.system(size: 22))
                            .foregroundColor(AppColors.textTertiary)
                    }
                }
                .frame(width: 96, height: 54)
                .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))

                // Metadata
                VStack(alignment: .leading, spacing: 3) {
                    Text((clip.filename as NSString).deletingPathExtension)
                        .font(.system(size: 15, weight: .semibold))
                        .foregroundColor(AppColors.textPrimary)
                        .lineLimit(1)

                    Text(clipDetailLine(clip))
                        .font(.system(size: 13))
                        .foregroundColor(AppColors.textSecondary)
                        .lineLimit(1)
                }

                Spacer(minLength: 4)

                Image(systemName: "chevron.right")
                    .font(.system(size: 14, weight: .semibold))
                    .foregroundColor(AppColors.textTertiary)
            }
            .padding(.horizontal, 14)
            .padding(.vertical, 10)
            .contentShape(Rectangle())
        }
        .buttonStyle(PlainButtonStyle())
    }
}
