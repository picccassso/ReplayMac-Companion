package com.replaymac.companion.data.demo

import com.replaymac.companion.data.model.CompanionAudioApp
import com.replaymac.companion.data.model.CompanionClipSummary
import com.replaymac.companion.data.model.CompanionDisplayInfo
import com.replaymac.companion.data.model.CompanionProfileSummary
import com.replaymac.companion.data.model.CompanionStateSnapshot
import com.replaymac.companion.data.model.CompanionVolumeTargets
import java.time.ZoneId
import kotlin.math.max
import kotlin.math.min

/** The save statuses ReplayMac reports. */
object DemoSaveStatus {
    const val IDLE = "idle"
    const val SAVING = "saving"
    const val SAVED = "saved"
    const val FAILED = "failed"
}

/**
 * The pretend Mac behind demo mode: what it is recording, its audio, displays,
 * profiles and clips. Every rule is a pure function returning the next state,
 * with no timers, so the rules can be tested directly; [DemoCompanionSession]
 * supplies the passing time. Anything aimed at an unknown id leaves the state as is.
 */
data class DemoCompanionState(
    val isRecording: Boolean,
    val isSessionRecording: Boolean,
    val saveStatus: String,
    /** What the save in progress will produce, while [saveStatus] is "saving". */
    val pendingSave: DemoClipKind?,
    val recordingElapsedSeconds: Double,
    val extendedBufferElapsedSeconds: Double,
    val sessionElapsedSeconds: Double,
    val bufferedSeconds: Double,
    val bufferDurationSeconds: Int,
    val longBufferEnabled: Boolean,
    val longBufferDurationSeconds: Int,
    val isMicrophoneMuted: Boolean,
    val microphoneVolume: Double,
    val isSystemAudioMuted: Boolean,
    val systemAudioVolume: Double,
    val audioApps: List<CompanionAudioApp>,
    val displays: List<CompanionDisplayInfo>,
    val profiles: List<CompanionProfileSummary>,
    val recentClips: List<CompanionClipSummary>,
    val nextClipNumber: Int,
    /** The real Mac copies the clip to its clipboard; the demo only remembers which one. */
    val lastCopiedClipId: String? = null,
    /** The real Mac reveals the clip in Finder; the demo only remembers which one. */
    val lastRevealedClipId: String? = null,
    /** Where new clips' filenames get their local time from. */
    val zone: ZoneId
) {
    companion object {
        /** Roughly what a 1080p60 buffer holds in memory per second. */
        const val BUFFER_BYTES_PER_SECOND = 13_700_000L
        const val MAX_RECENT_CLIPS = 10

        /** The demo Mac as it is when the demo starts, [now] being epoch seconds. */
        fun initial(now: Long, zone: ZoneId): DemoCompanionState = DemoCompanionState(
            isRecording = true,
            isSessionRecording = false,
            saveStatus = DemoSaveStatus.IDLE,
            pendingSave = null,
            recordingElapsedSeconds = 754.0,
            extendedBufferElapsedSeconds = 300.0,
            sessionElapsedSeconds = 0.0,
            bufferedSeconds = 30.0,
            bufferDurationSeconds = 30,
            longBufferEnabled = true,
            longBufferDurationSeconds = 300,
            isMicrophoneMuted = false,
            microphoneVolume = 0.8,
            isSystemAudioMuted = false,
            systemAudioVolume = 1.0,
            audioApps = DemoSampleData.audioApps,
            displays = DemoSampleData.displays,
            profiles = DemoSampleData.profiles,
            recentClips = DemoSampleData.recentClips(now, zone),
            nextClipNumber = DemoSampleData.FIRST_NEW_CLIP_NUMBER,
            zone = zone
        )

        /** "411 MB": whole megabytes, as ReplayMac shows the buffer's memory use. */
        fun formatMemory(bytes: Long): String = "${Math.round(bytes / 1_000_000.0)} MB"
    }

    val bufferMemoryBytes: Long
        get() = (bufferedSeconds * BUFFER_BYTES_PER_SECOND).toLong()

    // ==================== Recording ====================

    /** [seconds] of recording pass: the buffers fill up to their length and the session runs on. */
    fun tick(seconds: Double): DemoCompanionState {
        var next = this
        if (isRecording) {
            next = next.copy(
                recordingElapsedSeconds = recordingElapsedSeconds + seconds,
                bufferedSeconds = min(bufferedSeconds + seconds, bufferDurationSeconds.toDouble()),
                extendedBufferElapsedSeconds = min(
                    extendedBufferElapsedSeconds + seconds,
                    longBufferDurationSeconds.toDouble()
                )
            )
        }
        if (isSessionRecording) {
            next = next.copy(sessionElapsedSeconds = sessionElapsedSeconds + seconds)
        }
        return next
    }

    /** Stopping the buffer throws away what it held; starting it again fills it from empty. */
    fun toggleBuffer(): DemoCompanionState = copy(
        isRecording = !isRecording,
        recordingElapsedSeconds = 0.0,
        bufferedSeconds = 0.0,
        extendedBufferElapsedSeconds = 0.0
    )

    /** Stopping a session of at least a second saves it as a clip. */
    fun toggleSession(now: Long): DemoCompanionState {
        if (!isSessionRecording) return copy(isSessionRecording = true, sessionElapsedSeconds = 0.0)
        val stopped = copy(isSessionRecording = false, sessionElapsedSeconds = 0.0)
        return if (sessionElapsedSeconds >= 1) {
            stopped.addingClip(DemoClipKind.SESSION, sessionElapsedSeconds, now)
        } else {
            stopped
        }
    }

    // ==================== Saving ====================

    fun saveReplay(): DemoCompanionState =
        startSave(DemoClipKind.REPLAY, toSnapshot().canSaveQuickReplay)

    fun saveExtendedReplay(): DemoCompanionState =
        startSave(DemoClipKind.EXTENDED_REPLAY, toSnapshot().canSaveExtendedReplay)

    private fun startSave(kind: DemoClipKind, canSave: Boolean): DemoCompanionState = when {
        saveStatus == DemoSaveStatus.SAVING -> this
        !canSave -> copy(saveStatus = DemoSaveStatus.FAILED, pendingSave = null)
        else -> copy(saveStatus = DemoSaveStatus.SAVING, pendingSave = kind)
    }

    /** The save in progress lands as a new clip of whatever the buffer holds now. */
    fun finishSave(now: Long): DemoCompanionState {
        val kind = pendingSave ?: return this
        if (saveStatus != DemoSaveStatus.SAVING) return this
        val duration = when (kind) {
            DemoClipKind.EXTENDED_REPLAY -> min(extendedBufferElapsedSeconds, longBufferDurationSeconds.toDouble())
            else -> bufferedSeconds
        }
        return copy(saveStatus = DemoSaveStatus.SAVED, pendingSave = null).addingClip(kind, duration, now)
    }

    /** Clears "saved" or "failed". A save still in progress is left alone. */
    fun resetSaveStatus(): DemoCompanionState =
        if (saveStatus == DemoSaveStatus.SAVING) this else copy(saveStatus = DemoSaveStatus.IDLE)

    // ==================== Audio ====================

    fun toggleMicrophone(): DemoCompanionState = copy(isMicrophoneMuted = !isMicrophoneMuted)

    fun toggleSystemAudio(): DemoCompanionState = copy(isSystemAudioMuted = !isSystemAudioMuted)

    fun setVolume(target: String, volume: Double): DemoCompanionState {
        val clamped = volume.coerceIn(0.0, 1.0)
        return when (target) {
            CompanionVolumeTargets.MICROPHONE -> copy(microphoneVolume = clamped)
            CompanionVolumeTargets.SYSTEM_AUDIO -> copy(systemAudioVolume = clamped)
            else -> this
        }
    }

    fun toggleAppMute(bundleId: String): DemoCompanionState {
        if (audioApps.none { it.bundleId == bundleId }) return this
        return copy(audioApps = audioApps.map { if (it.bundleId == bundleId) it.copy(isMuted = !it.isMuted) else it })
    }

    fun unmuteAllApps(): DemoCompanionState = copy(audioApps = audioApps.map { it.copy(isMuted = false) })

    // ==================== Capture setup ====================

    fun selectDisplay(displayId: String): DemoCompanionState {
        if (displays.none { it.id == displayId }) return this
        return copy(displays = displays.map { it.copy(isActive = it.id == displayId) })
    }

    fun applyProfile(profileId: String): DemoCompanionState {
        if (profiles.none { it.id == profileId }) return this
        return copy(profiles = profiles.map { it.copy(isActive = it.id == profileId) })
    }

    // ==================== Clips ====================

    fun deleteClip(clipId: String): DemoCompanionState {
        if (clip(clipId) == null) return this
        return copy(recentClips = recentClips.filterNot { it.id == clipId })
    }

    /** Stands in for saving a trim in ReplayCap's editor: a new clip, half as long, at the top. */
    fun trimClip(clipId: String, now: Long): DemoCompanionState {
        val original = clip(clipId) ?: return this
        val duration = max(1L, Math.round((original.duration ?: 0.0) / 2)).toDouble()
        return addingClip(
            kind = original.kind,
            filename = DemoClips.trimmedFilename(original.filename),
            duration = duration,
            fileSize = (original.fileSize ?: 0L) / 2,
            now = now
        )
    }

    fun copyClip(clipId: String): DemoCompanionState =
        if (clip(clipId) == null) this else copy(lastCopiedClipId = clipId)

    fun revealClip(clipId: String): DemoCompanionState =
        if (clip(clipId) == null) this else copy(lastRevealedClipId = clipId)

    fun clip(clipId: String): CompanionClipSummary? = recentClips.firstOrNull { it.id == clipId }

    private fun addingClip(kind: DemoClipKind, duration: Double, now: Long): DemoCompanionState = addingClip(
        kind = kind.wireName,
        filename = DemoClips.filename(kind, now, zone),
        duration = duration,
        fileSize = DemoClips.fileSize(duration),
        now = now
    )

    /** Newest first, keeping only the latest [MAX_RECENT_CLIPS] like ReplayMac's recent list. */
    private fun addingClip(kind: String, filename: String, duration: Double, fileSize: Long, now: Long): DemoCompanionState {
        val clip = CompanionClipSummary(
            id = "demo-clip-$nextClipNumber",
            filename = filename,
            kind = kind,
            savedAt = now.toDouble(),
            duration = duration,
            fileSize = fileSize,
            hasThumbnail = true
        )
        return copy(
            recentClips = (listOf(clip) + recentClips).take(MAX_RECENT_CLIPS),
            nextClipNumber = nextClipNumber + 1
        )
    }

    // ==================== Snapshot ====================

    /** The state as the Mac would send it. */
    fun toSnapshot(): CompanionStateSnapshot = CompanionStateSnapshot(
        isRecording = isRecording,
        isSessionRecording = isSessionRecording,
        saveStatus = saveStatus,
        recordingElapsedSeconds = recordingElapsedSeconds,
        extendedBufferElapsedSeconds = extendedBufferElapsedSeconds,
        sessionElapsedSeconds = sessionElapsedSeconds,
        bufferedSeconds = bufferedSeconds,
        bufferDurationSeconds = bufferDurationSeconds,
        bufferMemoryBytes = bufferMemoryBytes,
        bufferMemoryFormatted = formatMemory(bufferMemoryBytes),
        longBufferEnabled = longBufferEnabled,
        longBufferDurationSeconds = longBufferDurationSeconds,
        capturedDisplayName = displays.firstOrNull { it.isActive }?.name,
        isUsingFallbackDisplay = false,
        isMicrophoneEnabled = true,
        isMicrophoneMuted = isMicrophoneMuted,
        isSystemAudioEnabled = true,
        isSystemAudioMuted = isSystemAudioMuted,
        microphoneVolume = microphoneVolume,
        systemAudioVolume = systemAudioVolume,
        perAppAudioEnabled = false,
        audioApps = audioApps,
        displays = displays,
        profiles = profiles,
        recentClips = recentClips
    )
}
