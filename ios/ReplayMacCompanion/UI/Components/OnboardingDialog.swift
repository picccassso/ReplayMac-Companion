import SwiftUI

private struct TutorialHighlight: Identifiable {
    let id = UUID()
    let icon: String
    let title: String
    let detail: String
}

private struct TutorialPage: Identifiable {
    let id = UUID()
    let title: String
    let subtitle: String
    let highlights: [TutorialHighlight]
}

private let tutorialPages: [TutorialPage] = [
    TutorialPage(
        title: "ReplayCap, from your phone",
        subtitle: "Save clips and check on recording without leaving your game.",
        highlights: [
            TutorialHighlight(icon: "arrow.down.circle.fill", title: "Save replays", detail: "Tap once to save the last 30 seconds, or longer."),
            TutorialHighlight(icon: "gauge.with.needle.fill", title: "See what’s happening", detail: "Buffer progress, memory use and session time.")
        ]
    ),
    TutorialPage(
        title: "Connecting",
        subtitle: "Turn on Companion Server in ReplayCap’s settings first.",
        highlights: [
            TutorialHighlight(icon: "wifi", title: "Wi-Fi (Recommended)", detail: "Your Mac shows up automatically when you’re on the same Wi-Fi network."),
            TutorialHighlight(icon: "cable.connector", title: "USB Cable", detail: "Plug into your Mac and turn on Personal Hotspot. Your Mac then shows up the same way.")
        ]
    ),
    TutorialPage(
        title: "Replay buffer and sessions",
        subtitle: "Two ways to capture your screen.",
        highlights: [
            TutorialHighlight(icon: "clock.arrow.circlepath", title: "Replay buffer", detail: "Keeps the last few seconds or minutes, so you can save after something happens."),
            TutorialHighlight(icon: "record.circle", title: "Session recording", detail: "Records everything until you stop it.")
        ]
    ),
    TutorialPage(
        title: "A few more things",
        subtitle: "Handy when your phone is propped up next to you.",
        highlights: [
            TutorialHighlight(icon: "lightbulb.fill", title: "Deck mode", detail: "Keeps the screen on. Tap the bulb at the top of Home."),
            TutorialHighlight(icon: "speaker.wave.2.fill", title: "Audio", detail: "Mute the microphone or system audio."),
            TutorialHighlight(icon: "film.fill", title: "This session", detail: "Clips you save show up on Home, ready to reveal in Finder.")
        ]
    )
]

public struct OnboardingDialog: View {
    public var onDismiss: () -> Void

    @State private var currentPage: Int = 0

    public init(onDismiss: @escaping () -> Void) {
        self.onDismiss = onDismiss
    }

    public var body: some View {
        ZStack {
            Color.black.opacity(0.65)
                .ignoresSafeArea()
                .onTapGesture {
                    onDismiss()
                }

            VStack(spacing: 16) {
                // Top bar with page count and close button
                HStack {
                    Text("\(currentPage + 1) of \(tutorialPages.count)")
                        .font(.system(size: 13, weight: .medium))
                        .foregroundColor(AppColors.textTertiary)
                        .padding(.leading, 4)

                    Spacer()

                    Button(action: onDismiss) {
                        Image(systemName: "xmark")
                            .font(.system(size: 14, weight: .semibold))
                            .foregroundColor(AppColors.textSecondary)
                            .frame(width: 32, height: 32)
                            .background(Circle().fill(AppColors.surfaceGlass))
                    }
                    .buttonStyle(GlassPressableStyle())
                }

                // TabView carousel
                TabView(selection: $currentPage) {
                    ForEach(0..<tutorialPages.count, id: \.self) { index in
                        TutorialPageView(page: tutorialPages[index])
                            .tag(index)
                    }
                }
                .tabViewStyle(PageTabViewStyle(indexDisplayMode: .never))
                .frame(minHeight: 280)

                // Bottom dots and buttons
                HStack(spacing: 8) {
                    HStack(spacing: 6) {
                        ForEach(0..<tutorialPages.count, id: \.self) { index in
                            Capsule()
                                .fill(index == currentPage ? AppColors.textPrimary : AppColors.textTertiary)
                                .frame(width: index == currentPage ? 20 : 7, height: 7)
                                .animation(.spring(response: 0.3), value: currentPage)
                        }
                    }

                    Spacer()

                    if currentPage < tutorialPages.count - 1 {
                        Button(action: onDismiss) {
                            Text("Skip")
                                .font(.system(size: 15, weight: .medium))
                                .foregroundColor(AppColors.textSecondary)
                                .padding(.horizontal, 16)
                                .padding(.vertical, 10)
                        }
                        .buttonStyle(GlassPressableStyle())

                        AccentButton(text: "Next") {
                            withAnimation {
                                currentPage += 1
                            }
                        }
                    } else {
                        AccentButton(text: "Done") {
                            onDismiss()
                        }
                    }
                }
            }
            .padding(22)
            .frame(maxWidth: 500)
            .glass(cornerRadius: 24, tint: AppColors.surfaceElevated)
            .padding(.horizontal, 20)
        }
    }
}

private struct TutorialPageView: View {
    let page: TutorialPage

    var body: some View {
        VStack(alignment: .leading, spacing: 14) {
            VStack(alignment: .leading, spacing: 4) {
                Text(page.title)
                    .font(.system(size: 20, weight: .bold))
                    .foregroundColor(AppColors.textPrimary)

                Text(page.subtitle)
                    .font(.system(size: 14))
                    .foregroundColor(AppColors.textSecondary)
            }
            .padding(.top, 4)

            VStack(spacing: 14) {
                ForEach(page.highlights) { highlight in
                    HStack(alignment: .top, spacing: 14) {
                        Image(systemName: highlight.icon)
                            .font(.system(size: 20))
                            .foregroundColor(AppColors.accent)
                            .frame(width: 24)

                        VStack(alignment: .leading, spacing: 2) {
                            Text(highlight.title)
                                .font(.system(size: 15, weight: .semibold))
                                .foregroundColor(AppColors.textPrimary)

                            Text(highlight.detail)
                                .font(.system(size: 13))
                                .foregroundColor(AppColors.textSecondary)
                        }

                        Spacer(minLength: 0)
                    }
                }
            }
            .padding(.top, 4)

            Spacer()
        }
    }
}
