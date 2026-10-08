package com.replaymac.companion.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
public data class CompanionClientMessage(
    val type: String,
    val deviceId: String? = null,
    val deviceName: String? = null,
    val clientVersion: String? = null,
    val action: String? = null,
    val clipId: String? = null
) {
    companion object {
        fun hello(deviceId: String, deviceName: String, clientVersion: String = "1.0.0"): CompanionClientMessage {
            return CompanionClientMessage(
                type = "hello",
                deviceId = deviceId,
                deviceName = deviceName,
                clientVersion = clientVersion
            )
        }

        fun action(actionName: String): CompanionClientMessage {
            return CompanionClientMessage(
                type = "action",
                action = actionName
            )
        }

        fun getThumbnail(clipId: String): CompanionClientMessage {
            return CompanionClientMessage(
                type = "get_thumbnail",
                action = "get_thumbnail",
                clipId = clipId
            )
        }

        fun revealClip(clipId: String): CompanionClientMessage {
            return CompanionClientMessage(
                type = "reveal_clip",
                action = "reveal_clip",
                clipId = clipId
            )
        }

        fun deleteClip(clipId: String): CompanionClientMessage {
            return CompanionClientMessage(
                type = "delete_clip",
                action = "delete_clip",
                clipId = clipId
            )
        }

        fun ping(): CompanionClientMessage {
            return CompanionClientMessage(type = "ping")
        }
    }
}

object CompanionActions {
    const val SAVE_REPLAY = "saveReplay"
    const val SAVE_EXTENDED_REPLAY = "saveExtendedReplay"
    const val TOGGLE_BUFFER = "toggleBuffer"
    const val TOGGLE_SESSION = "toggleSession"
    const val TOGGLE_MICROPHONE = "toggleMicrophone"
    const val TOGGLE_SYSTEM_AUDIO = "toggleSystemAudio"
}

@Serializable
data class CompanionAuthResponse(
    val type: String = "auth_response",
    val status: String, // "approved", "pending", "denied"
    val serverName: String = "ReplayMac",
    val serverVersion: String = "1.0"
)

@Serializable
data class CompanionClipSummary(
    val id: String,
    val filename: String,
    val kind: String = "replay", // "replay", "session", "extended"
    val savedAt: Double = 0.0,
    val duration: Double? = null,
    val fileSize: Long? = null,
    val hasThumbnail: Boolean = false
)

@Serializable
data class CompanionStateSnapshot(
    val type: String = "state_snapshot",
    val isRecording: Boolean = false,
    val isSessionRecording: Boolean = false,
    val saveStatus: String = "idle",
    val recordingElapsedSeconds: Double = 0.0,
    val extendedBufferElapsedSeconds: Double = 0.0,
    val sessionElapsedSeconds: Double = 0.0,
    val bufferedSeconds: Double = 0.0,
    val bufferDurationSeconds: Int = 30,
    val bufferMemoryBytes: Long = 0,
    val bufferMemoryFormatted: String = "0 MB",
    val longBufferEnabled: Boolean = false,
    val longBufferDurationSeconds: Int = 300,
    val capturedDisplayName: String? = null,
    /** The chosen display was unavailable, so the Mac is capturing another one. */
    val isUsingFallbackDisplay: Boolean = false,
    val isMicrophoneEnabled: Boolean = false,
    val isMicrophoneMuted: Boolean = false,
    val isSystemAudioEnabled: Boolean = true,
    val isSystemAudioMuted: Boolean = false,
    val recentClips: List<CompanionClipSummary> = emptyList()
) {
    val isSaveInProgress: Boolean
        get() = saveStatus.equals("saving", ignoreCase = true) || saveStatus.equals("busy", ignoreCase = true)

    val isSaveSuccess: Boolean
        get() = saveStatus.equals("saved", ignoreCase = true)

    val isSaveFailed: Boolean
        get() = saveStatus.equals("failed", ignoreCase = true)

    val canSaveQuickReplay: Boolean
        get() = isRecording && bufferedSeconds >= 1.0 && !isSaveInProgress

    val canSaveExtendedReplay: Boolean
        get() = isRecording && longBufferEnabled && !isSaveInProgress
}

@Serializable
data class CompanionTelemetryTick(
    val type: String = "telemetry_tick",
    val recordingElapsedSeconds: Double = 0.0,
    val extendedBufferElapsedSeconds: Double = 0.0,
    val sessionElapsedSeconds: Double = 0.0,
    val bufferedSeconds: Double = 0.0,
    val bufferMemoryBytes: Long = 0,
    val bufferMemoryFormatted: String = "0 MB"
)

@Serializable
data class CompanionThumbnailResponse(
    val type: String = "thumbnail_response",
    val clipId: String,
    val thumbnailBase64: String? = null
)

@Serializable
data class CompanionPongMessage(
    val type: String = "pong"
)

@Serializable
data class CompanionErrorMessage(
    val type: String = "error",
    val message: String
)
