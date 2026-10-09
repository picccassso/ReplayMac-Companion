import SwiftUI

/// Matches the desktop's MenuBarState.formattedDuration: "00:12", or "01:02:03" past an hour.
public func formatDurationSeconds(_ seconds: Double) -> String {
    let totalSec = max(0, Int(seconds))
    let hours = totalSec / 3600
    let mins = (totalSec % 3600) / 60
    let secs = totalSec % 60
    if hours > 0 {
        return String(format: "%02d:%02d:%02d", hours, mins, secs)
    } else {
        return String(format: "%02d:%02d", mins, secs)
    }
}

/// Matches the desktop's meter suffix: "Filling…" until the window is full, then "Ready".
public func meterFillSuffix(availableSeconds: Double, capSeconds: Int) -> String {
    if availableSeconds < Double(capSeconds) {
        return "Filling…"
    } else {
        return "Ready"
    }
}

public struct ProgressMeter: View {
    public var title: String
    public var availableSeconds: Double
    public var capSeconds: Int
    public var detail: String

    public init(
        title: String,
        availableSeconds: Double,
        capSeconds: Int,
        detail: String
    ) {
        self.title = title
        self.availableSeconds = availableSeconds
        self.capSeconds = capSeconds
        self.detail = detail
    }

    public var body: some View {
        let safeCap = max(1, capSeconds)
        let fraction = CGFloat(min(1.0, max(0.0, availableSeconds / Double(safeCap))))
        let suffix = meterFillSuffix(availableSeconds: availableSeconds, capSeconds: safeCap)
        let isReady = suffix == "Ready"
        let barColor = isReady ? AppColors.success : AppColors.accent

        VStack(alignment: .leading, spacing: 7) {
            HStack {
                Text(title)
                    .font(.system(size: 14, weight: .medium))
                    .foregroundColor(AppColors.textPrimary)

                Spacer()

                Text("\(formatDurationSeconds(availableSeconds)) / \(formatDurationSeconds(Double(safeCap)))")
                    .font(.system(size: 13, weight: .regular).monospacedDigit())
                    .foregroundColor(AppColors.textPrimary)

                Text("  \(suffix)")
                    .font(.system(size: 13, weight: .semibold))
                    .foregroundColor(isReady ? AppColors.success : AppColors.textSecondary)
            }

            GeometryReader { geo in
                ZStack(alignment: .leading) {
                    Capsule()
                        .fill(Color.white.opacity(0.12))
                        .frame(height: 6)

                    Capsule()
                        .fill(
                            LinearGradient(
                                colors: [barColor.opacity(0.75), barColor],
                                startPoint: .leading,
                                endPoint: .trailing
                            )
                        )
                        .frame(width: max(0, geo.size.width * fraction), height: 6)
                        .animation(.easeInOut(duration: 0.35), value: fraction)
                }
            }
            .frame(height: 6)

            Text(detail)
                .font(.system(size: 12))
                .foregroundColor(AppColors.textSecondary)
        }
    }
}

public struct DualProgressMeters: View {
    public var bufferedSeconds: Double
    public var bufferDurationSeconds: Int
    public var bufferMemoryFormatted: String
    public var longBufferEnabled: Bool
    public var extendedBufferElapsedSeconds: Double
    public var longBufferDurationSeconds: Int

    public init(
        bufferedSeconds: Double,
        bufferDurationSeconds: Int,
        bufferMemoryFormatted: String,
        longBufferEnabled: Bool,
        extendedBufferElapsedSeconds: Double,
        longBufferDurationSeconds: Int
    ) {
        self.bufferedSeconds = bufferedSeconds
        self.bufferDurationSeconds = bufferDurationSeconds
        self.bufferMemoryFormatted = bufferMemoryFormatted
        self.longBufferEnabled = longBufferEnabled
        self.extendedBufferElapsedSeconds = extendedBufferElapsedSeconds
        self.longBufferDurationSeconds = longBufferDurationSeconds
    }

    public var body: some View {
        VStack(spacing: 16) {
            ProgressMeter(
                title: "Quick replay",
                availableSeconds: min(bufferedSeconds, Double(bufferDurationSeconds)),
                capSeconds: bufferDurationSeconds,
                detail: "\(bufferMemoryFormatted) in memory"
            )

            if longBufferEnabled {
                ProgressMeter(
                    title: "Extended replay",
                    availableSeconds: min(extendedBufferElapsedSeconds, Double(longBufferDurationSeconds)),
                    capSeconds: longBufferDurationSeconds,
                    detail: "Kept on disk"
                )
            }
        }
    }
}
