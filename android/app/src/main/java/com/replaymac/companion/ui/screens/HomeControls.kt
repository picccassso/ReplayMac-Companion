package com.replaymac.companion.ui.screens

import com.replaymac.companion.data.model.CompanionStateSnapshot
import com.replaymac.companion.ui.components.TileTone
import com.replaymac.companion.ui.components.formatDurationSeconds
import kotlin.math.roundToInt

/** The six actions on the Home grid, in the desktop Home page's order. */
enum class HomeControl { SaveReplay, SaveExtendedReplay, ReplayBuffer, Session, Microphone, SystemAudio }

data class ControlTileSpec(
    val control: HomeControl,
    val title: String,
    val subtitle: String,
    val tone: TileTone,
    val isEnabled: Boolean,
    val isLoading: Boolean = false,
    val compactTitle: String? = null,
    val hasSecondaryAction: Boolean = false
)

/**
 * Wording and state for the Home screen, kept in one place (like the desktop's
 * ReplayControlLabels) so the portrait and landscape layouts always agree.
 */
object HomeControlLabels {

    fun headline(snapshot: CompanionStateSnapshot): String = when {
        snapshot.isSessionRecording -> "Session recording"
        snapshot.isRecording -> "Replay buffer running"
        else -> "Not recording"
    }

    fun displayedRecordingSeconds(snapshot: CompanionStateSnapshot): Double {
        if (snapshot.isSessionRecording) {
            return snapshot.sessionElapsedSeconds
        }
        val quickCap = snapshot.bufferDurationSeconds.toDouble()
        val cap = if (snapshot.longBufferEnabled) {
            maxOf(quickCap, snapshot.longBufferDurationSeconds.toDouble())
        } else {
            quickCap
        }
        return minOf(snapshot.recordingElapsedSeconds, cap)
    }

    fun subheadline(snapshot: CompanionStateSnapshot): String {
        if (!snapshot.isRecording && !snapshot.isSessionRecording) {
            return "Start the replay buffer to keep your last ${snapshot.bufferDurationSeconds} seconds ready to save."
        }
        val elapsed = displayedRecordingSeconds(snapshot)
        val display = snapshot.capturedDisplayName?.let { if (snapshot.isUsingFallbackDisplay) "$it (Fallback)" else it }
        return listOfNotNull(formatDurationSeconds(elapsed), display).joinToString(" · ")
    }

    fun volumePercentageLabel(volume: Double): String =
        "${(volume.coerceIn(0.0, 1.0) * 100.0).roundToInt()}%"

    fun setupPillLabel(snapshot: CompanionStateSnapshot): String? {
        if (!snapshot.hasCaptureSetupOptions) return null
        val parts = listOfNotNull(
            snapshot.activeDisplayName?.takeIf { it.isNotBlank() },
            snapshot.activeProfileName?.takeIf { it.isNotBlank() }
        )
        return if (parts.isNotEmpty()) parts.joinToString(" · ") else "Display & Profiles"
    }

    /** "Save Last 5m" for whole minutes, otherwise "Save Last 90s". */
    fun extendedReplayTitle(seconds: Int): String =
        if (seconds >= 60 && seconds % 60 == 0) "Save Last ${seconds / 60}m" else "Save Last ${seconds}s"

    /** Compact title for whole minutes ("Save 5m") or seconds ("Save 90s"). */
    fun extendedReplayCompactTitle(seconds: Int): String =
        if (seconds >= 60 && seconds % 60 == 0) "Save ${seconds / 60}m" else "Save ${seconds}s"

    fun tiles(snapshot: CompanionStateSnapshot): List<ControlTileSpec> = listOf(
        saveReplay(snapshot),
        saveExtendedReplay(snapshot),
        replayBuffer(snapshot),
        session(snapshot),
        microphone(snapshot),
        systemAudio(snapshot)
    )

    private fun saveReplay(snapshot: CompanionStateSnapshot) = ControlTileSpec(
        control = HomeControl.SaveReplay,
        title = "Save Last ${snapshot.bufferDurationSeconds}s",
        subtitle = when {
            snapshot.isSaveInProgress -> "Saving…"
            snapshot.isSaveSuccess -> "Saved"
            snapshot.isSaveFailed -> "Save failed"
            !snapshot.isRecording -> "Buffer stopped"
            snapshot.bufferedSeconds < 1.0 -> "Buffering…"
            else -> "Ready"
        },
        tone = TileTone.Prominent,
        isEnabled = snapshot.canSaveQuickReplay,
        isLoading = snapshot.isSaveInProgress,
        compactTitle = "Save ${snapshot.bufferDurationSeconds}s"
    )

    private fun saveExtendedReplay(snapshot: CompanionStateSnapshot) = ControlTileSpec(
        control = HomeControl.SaveExtendedReplay,
        title = extendedReplayTitle(snapshot.longBufferDurationSeconds),
        subtitle = when {
            !snapshot.longBufferEnabled -> "Turned off"
            snapshot.isSaveInProgress -> "Saving…"
            !snapshot.isRecording -> "Buffer stopped"
            else -> "Extended replay"
        },
        tone = TileTone.Normal,
        isEnabled = snapshot.canSaveExtendedReplay,
        compactTitle = extendedReplayCompactTitle(snapshot.longBufferDurationSeconds)
    )

    private fun replayBuffer(snapshot: CompanionStateSnapshot) = ControlTileSpec(
        control = HomeControl.ReplayBuffer,
        title = if (snapshot.isRecording) "Stop Buffer" else "Start Buffer",
        subtitle = if (snapshot.isRecording) "Running" else "Stopped",
        tone = TileTone.Normal,
        isEnabled = true,
        compactTitle = if (snapshot.isRecording) "Stop Buffer" else "Start Buffer"
    )

    private fun session(snapshot: CompanionStateSnapshot) = ControlTileSpec(
        control = HomeControl.Session,
        title = if (snapshot.isSessionRecording) "Stop & Save Session" else "Record Session",
        subtitle = if (snapshot.isSessionRecording) {
            formatDurationSeconds(snapshot.sessionElapsedSeconds)
        } else {
            "Full recording"
        },
        tone = if (snapshot.isSessionRecording) TileTone.Live else TileTone.Normal,
        isEnabled = !snapshot.isSaveInProgress,
        compactTitle = if (snapshot.isSessionRecording) "Stop Session" else "Record Session"
    )

    // The Mac enables and unmutes a switched-off source when toggled, so the
    // tile stays tappable in every state.
    private fun microphone(snapshot: CompanionStateSnapshot) = ControlTileSpec(
        control = HomeControl.Microphone,
        title = "Microphone",
        subtitle = when {
            !snapshot.isMicrophoneEnabled -> "Off"
            snapshot.isMicrophoneMuted -> "Muted"
            else -> "On"
        },
        tone = when {
            !snapshot.isMicrophoneEnabled -> TileTone.Off
            snapshot.isMicrophoneMuted -> TileTone.Muted
            else -> TileTone.Normal
        },
        isEnabled = true,
        hasSecondaryAction = true
    )

    private fun systemAudio(snapshot: CompanionStateSnapshot) = ControlTileSpec(
        control = HomeControl.SystemAudio,
        title = "System Audio",
        subtitle = when {
            !snapshot.isSystemAudioEnabled -> "Off"
            snapshot.isSystemAudioMuted -> "Muted"
            snapshot.perAppAudioEnabled -> "Selected app"
            snapshot.mutedAudioAppsCount > 0 -> "On · ${snapshot.mutedAudioAppsCount} muted"
            else -> "On"
        },
        tone = when {
            !snapshot.isSystemAudioEnabled -> TileTone.Off
            snapshot.isSystemAudioMuted -> TileTone.Muted
            else -> TileTone.Normal
        },
        isEnabled = true,
        hasSecondaryAction = true
    )
}
