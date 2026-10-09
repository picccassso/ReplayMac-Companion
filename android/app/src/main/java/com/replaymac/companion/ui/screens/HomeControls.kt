package com.replaymac.companion.ui.screens

import com.replaymac.companion.data.model.CompanionStateSnapshot
import com.replaymac.companion.ui.components.TileTone
import com.replaymac.companion.ui.components.formatDurationSeconds

/** The six actions on the Home grid, in the desktop Home page's order. */
enum class HomeControl { SaveReplay, SaveExtendedReplay, ReplayBuffer, Session, Microphone, SystemAudio }

data class ControlTileSpec(
    val control: HomeControl,
    val title: String,
    val subtitle: String,
    val tone: TileTone,
    val isEnabled: Boolean,
    val isLoading: Boolean = false,
    val compactTitle: String? = null
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

    fun subheadline(snapshot: CompanionStateSnapshot): String {
        if (!snapshot.isRecording && !snapshot.isSessionRecording) {
            return "Start the replay buffer to keep your last ${snapshot.bufferDurationSeconds} seconds ready to save."
        }
        val elapsed = if (snapshot.isSessionRecording) snapshot.sessionElapsedSeconds else snapshot.recordingElapsedSeconds
        val display = snapshot.capturedDisplayName?.let { if (snapshot.isUsingFallbackDisplay) "$it (Fallback)" else it }
        return listOfNotNull(formatDurationSeconds(elapsed), display).joinToString(" · ")
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
        audioSource(HomeControl.Microphone, "Microphone", snapshot.isMicrophoneEnabled, snapshot.isMicrophoneMuted),
        audioSource(HomeControl.SystemAudio, "System Audio", snapshot.isSystemAudioEnabled, snapshot.isSystemAudioMuted)
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
    private fun audioSource(control: HomeControl, title: String, isEnabled: Boolean, isMuted: Boolean) =
        ControlTileSpec(
            control = control,
            title = title,
            subtitle = when {
                !isEnabled -> "Off"
                isMuted -> "Muted"
                else -> "On"
            },
            tone = when {
                !isEnabled -> TileTone.Off
                isMuted -> TileTone.Muted
                else -> TileTone.Normal
            },
            isEnabled = true
        )
}
