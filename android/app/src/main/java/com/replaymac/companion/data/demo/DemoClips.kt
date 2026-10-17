package com.replaymac.companion.data.demo

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** The kinds of clip the demo Mac saves: ReplayMac's wire name, and the title its filenames start with. */
enum class DemoClipKind(val wireName: String, val title: String) {
    REPLAY("replay", "Replay"),
    SESSION("session", "Session"),
    EXTENDED_REPLAY("extended replay", "Extended Replay")
}

/** How the demo names and sizes clips, close enough to ReplayMac's own recordings to look real. */
object DemoClips {
    /** Roughly what ReplayMac's default 1080p60 HEVC recording writes per second. */
    const val BYTES_PER_SECOND = 1_600_000L

    private const val EXTENSION = ".mp4"

    private val timestampFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd 'at' HH.mm.ss", Locale.US)

    /** "Replay 2026-10-17 at 14.02.31.mp4", from [savedAt] (epoch seconds) in [zone]. */
    fun filename(kind: DemoClipKind, savedAt: Long, zone: ZoneId): String {
        val timestamp = timestampFormat.format(Instant.ofEpochSecond(savedAt).atZone(zone))
        return "${kind.title} $timestamp$EXTENSION"
    }

    /** "Replay … .mp4" becomes "Replay … (Trimmed).mp4". */
    fun trimmedFilename(original: String): String = "${original.removeSuffix(EXTENSION)} (Trimmed)$EXTENSION"

    fun fileSize(durationSeconds: Double): Long = (durationSeconds * BYTES_PER_SECOND).toLong()
}
