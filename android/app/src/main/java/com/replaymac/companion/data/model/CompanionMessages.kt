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
    val clipId: String? = null,
    val protocolVersion: Int? = null,
    val proof: String? = null,
    val bundleId: String? = null,
    val target: String? = null,
    val volume: Double? = null,
    val displayId: String? = null,
    val profileId: String? = null
) {
    companion object {
        fun hello(deviceId: String, deviceName: String, clientVersion: String = "1.0.0"): CompanionClientMessage {
            return CompanionClientMessage(
                type = "hello",
                deviceId = deviceId,
                deviceName = deviceName,
                clientVersion = clientVersion,
                protocolVersion = CompanionProtocol.VERSION
            )
        }

        /** The answer to an auth_challenge. A null [proof] asks the Mac to fall back to approval on screen. */
        fun authProof(proof: String?): CompanionClientMessage {
            return CompanionClientMessage(type = "auth_proof", proof = proof)
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

        fun trimClip(clipId: String): CompanionClientMessage {
            return CompanionClientMessage(
                type = "trim_clip",
                action = "trim_clip",
                clipId = clipId
            )
        }

        fun copyClip(clipId: String): CompanionClientMessage {
            return CompanionClientMessage(
                type = "copy_clip",
                action = "copy_clip",
                clipId = clipId
            )
        }

        fun setVolume(target: String, volume: Double): CompanionClientMessage {
            return CompanionClientMessage(
                type = "set_volume",
                action = "set_volume",
                target = target,
                volume = volume.coerceIn(0.0, 1.0)
            )
        }

        fun toggleAppMute(bundleId: String): CompanionClientMessage {
            return CompanionClientMessage(
                type = "toggle_app_mute",
                action = "toggle_app_mute",
                bundleId = bundleId
            )
        }

        fun unmuteAllApps(): CompanionClientMessage {
            return CompanionClientMessage(
                type = "unmute_all_apps",
                action = "unmute_all_apps"
            )
        }

        fun selectDisplay(displayId: String): CompanionClientMessage {
            return CompanionClientMessage(
                type = "select_display",
                action = "select_display",
                displayId = displayId
            )
        }

        fun applyProfile(profileId: String): CompanionClientMessage {
            return CompanionClientMessage(
                type = "apply_profile",
                action = "apply_profile",
                profileId = profileId
            )
        }

        fun ping(): CompanionClientMessage {
            return CompanionClientMessage(type = "ping")
        }
    }
}

object CompanionProtocol {
    const val VERSION = 2
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
    val serverVersion: String = "1.0",
    /** Stable identity of the Mac, the same on every network. Missing from older ReplayMac builds. */
    val serverId: String? = null,
    /** The friendly name of the paired device assigned or confirmed by the Mac. */
    val deviceName: String? = null,
    /** On "pending": the code ReplayMac shows next to Allow, so the user can check it's this phone. */
    val pairingCode: String? = null,
    /** On "approved", only right after the user clicked Allow: the secret that proves this phone next time. */
    val pairingSecret: String? = null,
    /** On "denied": "user_denied", "revoked" or "invalid_proof". */
    val reason: String? = null
)

/** Sent after hello when the Mac already knows this phone; answered with an auth_proof. */
@Serializable
data class CompanionAuthChallenge(
    val type: String = "auth_challenge",
    val nonce: String,
    val serverId: String,
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
data class CompanionAudioApp(
    val bundleId: String,
    val name: String,
    val isMuted: Boolean = false,
    val isRunning: Boolean = true,
    val iconBase64: String? = null
)

@Serializable
data class CompanionDisplayInfo(
    val id: String,
    val name: String,
    val isActive: Boolean = false,
    val isConnected: Boolean = true
)

@Serializable
data class CompanionProfileSummary(
    val id: String,
    val name: String,
    val summary: String = "",
    val isActive: Boolean = false
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
    val microphoneVolume: Double = 1.0,
    val systemAudioVolume: Double = 1.0,
    val perAppAudioEnabled: Boolean = false,
    val audioApps: List<CompanionAudioApp> = emptyList(),
    val displays: List<CompanionDisplayInfo> = emptyList(),
    val profiles: List<CompanionProfileSummary> = emptyList(),
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

    val mutedAudioAppsCount: Int
        get() = if (isSystemAudioEnabled && !perAppAudioEnabled) audioApps.count { it.isMuted } else 0

    val activeDisplayName: String?
        get() = displays.firstOrNull { it.isActive }?.name ?: capturedDisplayName

    val activeProfileName: String?
        get() = profiles.firstOrNull { it.isActive }?.name

    val hasCaptureSetupOptions: Boolean
        get() = displays.isNotEmpty() || profiles.isNotEmpty()
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
    val message: String,
    /** Set when the Mac is turning this connection away; see [CompanionErrorCodes]. */
    val code: String? = null
)

object CompanionErrorCodes {
    /** The only code after which the Mac keeps the socket open. */
    const val NOT_AUTHORIZED = "not_authorized"
}
