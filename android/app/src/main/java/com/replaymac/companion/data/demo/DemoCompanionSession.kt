package com.replaymac.companion.data.demo

import com.replaymac.companion.data.model.CompanionActions
import com.replaymac.companion.data.model.CompanionServerEvent
import com.replaymac.companion.data.model.ConnectionState
import com.replaymac.companion.data.network.CompanionLink
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.ZoneId

/**
 * Plays a Mac running ReplayMac, so the app can be tried without one. It
 * answers the same commands as the socket and sends back state_changed
 * snapshots, but never touches the network: all it has is [DemoCompanionState].
 *
 * Time comes from [scope] (its delays tick the recording and pace saves) and
 * [clock] (epoch seconds, for when clips were saved), so tests can drive both.
 */
class DemoCompanionSession(
    private val scope: CoroutineScope,
    private val clock: () -> Long = { System.currentTimeMillis() / 1_000 },
    private val zone: () -> ZoneId = { ZoneId.systemDefault() },
    private val thumbnailRenderer: DemoThumbnailRenderer = GradientThumbnailRenderer
) : CompanionLink {
    companion object {
        val CONNECTED = ConnectionState.Connected(
            serverName = "Demo Mac",
            serverVersion = "Demo",
            host = "demo",
            port = 0,
            serverId = null,
            deviceName = null
        )
        const val TICK_MILLIS = 1_000L
        /** How long a save takes before it shows as saved. */
        const val SAVE_DURATION_MILLIS = 1_200L
        /** How long "saved" or "failed" stays up before going back to idle. */
        const val SAVE_RESULT_MILLIS = 2_000L
    }

    /** Guards [state] and the jobs: commands come from the UI while ticks and saves run on [scope]. */
    private val lock = Any()
    private var state: DemoCompanionState? = null
    private var tickJob: Job? = null
    private var saveJob: Job? = null

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    val isRunning: Boolean
        get() = _connectionState.value is ConnectionState.Connected

    private val _events = MutableSharedFlow<CompanionServerEvent>(extraBufferCapacity = 64)
    override val events: SharedFlow<CompanionServerEvent> = _events.asSharedFlow()

    /** The demo Mac's state, or null while the demo is stopped. */
    val currentState: DemoCompanionState?
        get() = synchronized(lock) { state }

    /** Starts from the sample data, as if a Mac had just been connected. */
    fun start() = synchronized(lock) {
        cancelJobs()
        val initial = DemoCompanionState.initial(clock(), zone())
        state = initial
        // Connected first, so the snapshot's thumbnail requests are routed here.
        _connectionState.value = CONNECTED
        publish(initial)
        tickJob = scope.launch {
            while (isActive) {
                delay(TICK_MILLIS)
                mutateIfActive { it.tick(TICK_MILLIS / 1_000.0) }
            }
        }
    }

    fun stop() = synchronized(lock) {
        cancelJobs()
        state = null
        _connectionState.value = ConnectionState.Disconnected
    }

    override fun resync(): Boolean = synchronized(lock) {
        val current = state ?: return false
        publish(current)
        true
    }

    override fun sendAction(actionName: String): Boolean = when (actionName) {
        CompanionActions.SAVE_REPLAY -> save { it.saveReplay() }
        CompanionActions.SAVE_EXTENDED_REPLAY -> save { it.saveExtendedReplay() }
        CompanionActions.TOGGLE_BUFFER -> mutate { it.toggleBuffer() }
        CompanionActions.TOGGLE_SESSION -> mutate { it.toggleSession(clock()) }
        CompanionActions.TOGGLE_MICROPHONE -> mutate { it.toggleMicrophone() }
        CompanionActions.TOGGLE_SYSTEM_AUDIO -> mutate { it.toggleSystemAudio() }
        else -> false
    }

    /** Answers like the Mac does, with a thumbnail_response; an unknown clip gets no image. */
    override fun getThumbnail(clipId: String): Boolean {
        val clip = synchronized(lock) {
            val current = state ?: return false
            current.clip(clipId)
        }
        val image = clip?.let { thumbnailRenderer.render(it.id) }
        _events.tryEmit(CompanionServerEvent.Thumbnail(clipId, image))
        return true
    }

    override fun revealClip(clipId: String) = mutate { it.revealClip(clipId) }
    override fun deleteClip(clipId: String) = mutate { it.deleteClip(clipId) }
    override fun trimClip(clipId: String) = mutate { it.trimClip(clipId, clock()) }
    override fun copyClip(clipId: String) = mutate { it.copyClip(clipId) }
    override fun setVolume(target: String, volume: Double) = mutate { it.setVolume(target, volume) }
    override fun toggleAppMute(bundleId: String) = mutate { it.toggleAppMute(bundleId) }
    override fun unmuteAllApps() = mutate { it.unmuteAllApps() }
    override fun selectDisplay(displayId: String) = mutate { it.selectDisplay(displayId) }
    override fun applyProfile(profileId: String) = mutate { it.applyProfile(profileId) }

    /** Applies a rule, telling the app if the snapshot changed. False while the demo is stopped. */
    private fun mutate(rule: (DemoCompanionState) -> DemoCompanionState): Boolean = synchronized(lock) {
        val before = state ?: return false
        val after = rule(before)
        state = after
        if (after.toSnapshot() != before.toSnapshot()) publish(after)
        true
    }

    /** Starts a save, then plays out its steps: saving, saved, back to idle (or failed, then idle). */
    private fun save(rule: (DemoCompanionState) -> DemoCompanionState): Boolean = synchronized(lock) {
        val wasSaving = state?.saveStatus == DemoSaveStatus.SAVING
        if (!mutate(rule)) return false
        if (wasSaving) return true
        // A newer save replaces the previous one's pending reset.
        saveJob?.cancel()
        saveJob = when (state?.saveStatus) {
            DemoSaveStatus.SAVING -> scope.launch {
                delay(SAVE_DURATION_MILLIS)
                mutateIfActive { it.finishSave(clock()) }
                delay(SAVE_RESULT_MILLIS)
                mutateIfActive { it.resetSaveStatus() }
            }
            DemoSaveStatus.FAILED -> scope.launch {
                delay(SAVE_RESULT_MILLIS)
                mutateIfActive { it.resetSaveStatus() }
            }
            else -> null
        }
        true
    }

    /** For timed steps: checked under the lock, so a job cancelled by stop() or a newer save can't apply late. */
    private fun CoroutineScope.mutateIfActive(rule: (DemoCompanionState) -> DemoCompanionState) {
        synchronized(lock) {
            if (isActive) mutate(rule)
        }
    }

    private fun publish(current: DemoCompanionState) {
        _events.tryEmit(CompanionServerEvent.StateChanged(current.toSnapshot()))
    }

    private fun cancelJobs() {
        tickJob?.cancel()
        tickJob = null
        saveJob?.cancel()
        saveJob = null
    }
}
