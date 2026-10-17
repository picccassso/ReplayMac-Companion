package com.replaymac.companion.data.demo

import com.replaymac.companion.data.model.CompanionAudioApp
import com.replaymac.companion.data.model.CompanionClipSummary
import com.replaymac.companion.data.model.CompanionDisplayInfo
import com.replaymac.companion.data.model.CompanionProfileSummary
import java.time.ZoneId

/** The demo Mac's fixed setup. Kept identical to the iOS app's so both demos show the same thing. */
object DemoSampleData {
    val audioApps = listOf(
        CompanionAudioApp(bundleId = "com.apple.Safari", name = "Safari"),
        CompanionAudioApp(bundleId = "com.apple.Music", name = "Music"),
        CompanionAudioApp(bundleId = "com.hnc.Discord", name = "Discord", isMuted = true),
        CompanionAudioApp(bundleId = "com.tinyspeck.slackmacgap", name = "Slack")
    )

    val displays = listOf(
        CompanionDisplayInfo(id = "demo-display-studio", name = "Studio Display", isActive = true),
        CompanionDisplayInfo(id = "demo-display-builtin", name = "Built-in Retina Display")
    )

    val profiles = listOf(
        CompanionProfileSummary(
            id = "demo-profile-gaming",
            name = "Gaming",
            summary = "1080p · 60 fps · System audio + mic",
            isActive = true
        ),
        CompanionProfileSummary(id = "demo-profile-tutorials", name = "Tutorials", summary = "1440p · 30 fps · Mic only"),
        CompanionProfileSummary(id = "demo-profile-battery", name = "Battery Saver", summary = "720p · 30 fps")
    )

    /** Ids 1 to 3 are taken by these, so clips saved in the demo start at 4. */
    const val FIRST_NEW_CLIP_NUMBER = 4

    /** Three clips saved earlier, newest first, timed relative to [now] (epoch seconds). */
    fun recentClips(now: Long, zone: ZoneId): List<CompanionClipSummary> = listOf(
        clip(1, DemoClipKind.REPLAY, duration = 30.0, fileSize = 48_000_000, savedAt = now - 300, zone = zone),
        clip(2, DemoClipKind.SESSION, duration = 754.0, fileSize = 1_210_000_000, savedAt = now - 3_600, zone = zone),
        clip(3, DemoClipKind.EXTENDED_REPLAY, duration = 300.0, fileSize = 482_000_000, savedAt = now - 86_400, zone = zone)
    )

    private fun clip(
        number: Int,
        kind: DemoClipKind,
        duration: Double,
        fileSize: Long,
        savedAt: Long,
        zone: ZoneId
    ) = CompanionClipSummary(
        id = "demo-clip-$number",
        filename = DemoClips.filename(kind, savedAt, zone),
        kind = kind.wireName,
        savedAt = savedAt.toDouble(),
        duration = duration,
        fileSize = fileSize,
        hasThumbnail = true
    )
}
