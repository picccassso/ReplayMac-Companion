import SwiftUI
import UIKit

public struct ClipDetailsBottomSheet: View {
    public var clip: CompanionClipSummary
    public var thumbnailBase64: String?
    public var onRevealInFinder: (String) -> Void
    public var onTrimClip: (String) -> Void
    public var onCopyClip: (String) -> Void
    public var onDeleteClip: (String) -> Void
    public var onDismiss: () -> Void

    @State private var showDeleteConfirmation = false
    @State private var copiedToMac = false

    public init(
        clip: CompanionClipSummary,
        thumbnailBase64: String?,
        onRevealInFinder: @escaping (String) -> Void,
        onTrimClip: @escaping (String) -> Void = { _ in },
        onCopyClip: @escaping (String) -> Void = { _ in },
        onDeleteClip: @escaping (String) -> Void,
        onDismiss: @escaping () -> Void
    ) {
        self.clip = clip
        self.thumbnailBase64 = thumbnailBase64
        self.onRevealInFinder = onRevealInFinder
        self.onTrimClip = onTrimClip
        self.onCopyClip = onCopyClip
        self.onDeleteClip = onDeleteClip
        self.onDismiss = onDismiss
    }

    private var thumbnailImage: UIImage? {
        guard let base64 = thumbnailBase64, !base64.isEmpty,
              let data = Data(base64Encoded: base64) else { return nil }
        return UIImage(data: data)
    }

    public var body: some View {
        GeometryReader { geo in
            let isLandscape = geo.size.width > geo.size.height

            VStack(spacing: 0) {
                // Drag handle indicator remains pinned at top
                Capsule()
                    .fill(AppColors.textTertiary)
                    .frame(width: 40, height: isLandscape ? 4 : 5)
                    .padding(.top, isLandscape ? 8 : 10)
                    .padding(.bottom, isLandscape ? 12 : 16)

                ScrollView(.vertical, showsIndicators: false) {
                    if isLandscape {
                        landscapeLayout(geo: geo)
                    } else {
                        portraitLayout
                    }
                }
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .top)
            .background(AppColors.backgroundSecondary.ignoresSafeArea())
        }
        .alert("Delete this clip?", isPresented: $showDeleteConfirmation) {
            Button("Delete", role: .destructive) {
                onDeleteClip(clip.id)
                onDismiss()
            }
            Button("Cancel", role: .cancel) {}
        } message: {
            Text("“\(clip.filename)” will be moved to the Trash on your Mac.")
        }
    }

    // MARK: - Layouts

    private func landscapeLayout(geo: GeometryProxy) -> some View {
        let maxThumbnailHeight = max(160, min(geo.size.height * 0.62, 340))

        return HStack(alignment: .center, spacing: 20) {
            thumbnailView
                .frame(
                    maxWidth: .infinity,
                    maxHeight: maxThumbnailHeight
                )

            VStack(alignment: .leading, spacing: 14) {
                metadataView(isCompact: true)
                landscapeActionButtons
            }
            .frame(maxWidth: .infinity, alignment: .leading)
        }
        .padding(.horizontal, 24)
        .padding(.bottom, 20)
        .frame(maxWidth: .infinity)
    }

    private var portraitLayout: some View {
        VStack(spacing: 18) {
            thumbnailView
                .frame(maxWidth: .infinity)

            metadataView(isCompact: false)

            portraitActionButtons

            Spacer(minLength: 0)
        }
        .padding(.horizontal, 20)
        .padding(.bottom, 24)
        .frame(maxWidth: .infinity)
    }

    // MARK: - Subviews

    private var thumbnailView: some View {
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
    }

    private func metadataView(isCompact: Bool) -> some View {
        VStack(alignment: .leading, spacing: 4) {
            Text((clip.filename as NSString).deletingPathExtension)
                .font(.system(size: isCompact ? 17 : 19, weight: .bold))
                .foregroundColor(AppColors.textPrimary)
                .lineLimit(1)
                .truncationMode(.middle)
                .frame(maxWidth: .infinity, alignment: .leading)

            Text(clipDetailLine(clip))
                .font(.system(size: isCompact ? 13 : 14))
                .foregroundColor(AppColors.textSecondary)
                .frame(maxWidth: .infinity, alignment: .leading)
        }
    }

    private var portraitActionButtons: some View {
        VStack(spacing: 10) {
            HStack(spacing: 12) {
                showInFinderButton(fullWidth: true)
                trimButton(fullWidth: true)
            }
            HStack(spacing: 12) {
                copyButton(fullWidth: true)
                deleteButton(fullWidth: true)
            }
        }
        .padding(.top, 4)
    }

    private var landscapeActionButtons: some View {
        VStack(spacing: 10) {
            HStack(spacing: 10) {
                showInFinderButton(fullWidth: true)
                trimButton(fullWidth: true)
            }
            HStack(spacing: 10) {
                copyButton(fullWidth: true)
                deleteButton(fullWidth: true)
            }
        }
        .padding(.top, 4)
    }

    private func showInFinderButton(fullWidth: Bool) -> some View {
        Button(action: {
            CompanionHaptics.tap()
            onRevealInFinder(clip.id)
            onDismiss()
        }) {
            HStack(spacing: 6) {
                Image(systemName: "folder")
                    .font(.system(size: 15, weight: .semibold))
                Text("Show in Finder")
                    .font(.system(size: 15, weight: .semibold))
                    .lineLimit(1)
                    .minimumScaleFactor(0.85)
            }
            .foregroundColor(.white)
            .padding(.horizontal, 14)
            .padding(.vertical, 12)
            .frame(maxWidth: fullWidth ? .infinity : nil)
            .background(AppColors.accent, in: RoundedRectangle(cornerRadius: AppRadii.small, style: .continuous))
        }
        .buttonStyle(GlassPressableStyle())
    }

    private func trimButton(fullWidth: Bool) -> some View {
        Button(action: {
            CompanionHaptics.tap()
            onTrimClip(clip.id)
            onDismiss()
        }) {
            HStack(spacing: 6) {
                Image(systemName: "scissors")
                    .font(.system(size: 15, weight: .semibold))
                Text("Trim on Mac")
                    .font(.system(size: 15, weight: .semibold))
                    .lineLimit(1)
                    .minimumScaleFactor(0.85)
            }
            .foregroundColor(AppColors.textPrimary)
            .padding(.horizontal, 14)
            .padding(.vertical, 12)
            .frame(maxWidth: fullWidth ? .infinity : nil)
            .glass(cornerRadius: AppRadii.small, tint: AppColors.surfaceGlass)
        }
        .buttonStyle(GlassPressableStyle())
    }

    private func copyButton(fullWidth: Bool) -> some View {
        Button(action: {
            CompanionHaptics.performOutcome(.success)
            onCopyClip(clip.id)
            copiedToMac = true
        }) {
            HStack(spacing: 6) {
                Image(systemName: copiedToMac ? "checkmark.circle.fill" : "doc.on.doc")
                    .font(.system(size: 15, weight: .medium))
                Text(copiedToMac ? "Copied to Mac" : "Copy to Mac")
                    .font(.system(size: 15, weight: .medium))
                    .lineLimit(1)
                    .minimumScaleFactor(0.85)
            }
            .foregroundColor(copiedToMac ? AppColors.success : AppColors.textPrimary)
            .padding(.horizontal, 14)
            .padding(.vertical, 12)
            .frame(maxWidth: fullWidth ? .infinity : nil)
            .glass(
                cornerRadius: AppRadii.small,
                tint: copiedToMac ? AppColors.success.opacity(0.14) : AppColors.surfaceGlass
            )
        }
        .buttonStyle(GlassPressableStyle())
    }

    private func deleteButton(fullWidth: Bool) -> some View {
        Button(action: {
            CompanionHaptics.tap()
            showDeleteConfirmation = true
        }) {
            HStack(spacing: 6) {
                Image(systemName: "trash")
                    .font(.system(size: 15, weight: .medium))
                Text("Delete")
                    .font(.system(size: 15, weight: .medium))
                    .lineLimit(1)
            }
            .foregroundColor(AppColors.danger)
            .padding(.horizontal, 14)
            .padding(.vertical, 12)
            .frame(maxWidth: fullWidth ? .infinity : nil)
            .glass(cornerRadius: AppRadii.small, tint: AppColors.danger.opacity(0.12))
        }
        .buttonStyle(GlassPressableStyle())
    }
}
