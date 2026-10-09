import SwiftUI
import UIKit

public struct ClipDetailsBottomSheet: View {
    public var clip: CompanionClipSummary
    public var thumbnailBase64: String?
    public var onRevealInFinder: (String) -> Void
    public var onDeleteClip: (String) -> Void
    public var onDismiss: () -> Void

    @State private var showDeleteConfirmation = false

    public init(
        clip: CompanionClipSummary,
        thumbnailBase64: String?,
        onRevealInFinder: @escaping (String) -> Void,
        onDeleteClip: @escaping (String) -> Void,
        onDismiss: @escaping () -> Void
    ) {
        self.clip = clip
        self.thumbnailBase64 = thumbnailBase64
        self.onRevealInFinder = onRevealInFinder
        self.onDeleteClip = onDeleteClip
        self.onDismiss = onDismiss
    }

    private var thumbnailImage: UIImage? {
        guard let base64 = thumbnailBase64, !base64.isEmpty,
              let data = Data(base64Encoded: base64) else { return nil }
        return UIImage(data: data)
    }

    public var body: some View {
        VStack(spacing: 18) {
            // Drag handle indicator
            Capsule()
                .fill(AppColors.textTertiary)
                .frame(width: 40, height: 5)
                .padding(.top, 10)

            // 16:9 Thumbnail
            ZStack {
                RoundedRectangle(cornerRadius: 18, style: .continuous)
                    .fill(Color.black)

                if let uiImage = thumbnailImage {
                    Image(uiImage: uiImage)
                        .resizable()
                        .aspectRatio(contentMode: .fit)
                        .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
                } else {
                    Image(systemName: "film")
                        .font(.system(size: 40))
                        .foregroundColor(AppColors.textTertiary)
                }
            }
            .aspectRatio(16.0 / 9.0, contentMode: .fit)
            .frame(maxWidth: .infinity)

            // Metadata
            VStack(alignment: .leading, spacing: 4) {
                Text((clip.filename as NSString).deletingPathExtension)
                    .font(.system(size: 19, weight: .bold))
                    .foregroundColor(AppColors.textPrimary)
                    .frame(maxWidth: .infinity, alignment: .leading)

                Text(clipDetailLine(clip))
                    .font(.system(size: 14))
                    .foregroundColor(AppColors.textSecondary)
                    .frame(maxWidth: .infinity, alignment: .leading)
            }

            // Action Buttons
            HStack(spacing: 12) {
                AccentButton(
                    text: "Show in Finder",
                    icon: "folder"
                ) {
                    onRevealInFinder(clip.id)
                    onDismiss()
                }
                .frame(maxWidth: .infinity)

                Button(action: {
                    showDeleteConfirmation = true
                }) {
                    HStack(spacing: 6) {
                        Image(systemName: "trash")
                            .font(.system(size: 15, weight: .medium))
                        Text("Delete")
                            .font(.system(size: 15, weight: .medium))
                    }
                    .foregroundColor(AppColors.danger)
                    .padding(.horizontal, 18)
                    .padding(.vertical, 12)
                    .glass(cornerRadius: AppRadii.small, tint: AppColors.danger.opacity(0.12))
                }
                .buttonStyle(GlassPressableStyle())
            }
            .padding(.top, 4)

            Spacer(minLength: 0)
        }
        .padding(.horizontal, 20)
        .padding(.bottom, 24)
        .background(AppColors.backgroundSecondary.ignoresSafeArea())
        .alert("Delete this clip?", isPresented: $showDeleteConfirmation) {
            Button("Delete", role: .destructive) {
                onDeleteClip(clip.id)
                onDismiss()
            }
            Button("Cancel", role: .cancel) {}
        } message: {
            Text("“\(clip.filename)” will be deleted from your Mac. This can’t be undone.")
        }
    }
}
