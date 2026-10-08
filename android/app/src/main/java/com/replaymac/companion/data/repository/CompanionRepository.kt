package com.replaymac.companion.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.util.Log
import com.replaymac.companion.data.model.CompanionActions
import com.replaymac.companion.data.model.CompanionServerEvent
import com.replaymac.companion.data.model.CompanionStateSnapshot
import com.replaymac.companion.data.model.ConnectionState
import com.replaymac.companion.data.model.DiscoveredMac
import com.replaymac.companion.data.network.CompanionWebSocketClient
import com.replaymac.companion.data.network.NsdDiscoveryManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class CompanionRepository(
    context: Context,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {
    companion object {
        private const val TAG = "CompanionRepository"
        private const val PREFS_NAME = "replaymac_companion_prefs"
        private const val KEY_DEVICE_ID = "device_id"
        private const val KEY_LAST_HOST = "last_host"
        private const val KEY_LAST_PORT = "last_port"
        private const val KEY_KEEP_AWAKE = "keep_awake"
        private const val KEY_HAS_SEEN_ONBOARDING = "has_seen_onboarding"
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    val deviceId: String = prefs.getString(KEY_DEVICE_ID, null) ?: run {
        val newId = UUID.randomUUID().toString()
        prefs.edit().putString(KEY_DEVICE_ID, newId).apply()
        newId
    }

    private val formattedDeviceName: String = run {
        val manufacturer = Build.MANUFACTURER.orEmpty().replaceFirstChar { it.uppercase() }
        val model = Build.MODEL.orEmpty()
        if (model.startsWith(manufacturer, ignoreCase = true)) {
            model
        } else {
            "$manufacturer $model".trim().ifEmpty { "Android Device" }
        }
    }

    private val nsdManager = NsdDiscoveryManager(context, scope)
    private val wsClient = CompanionWebSocketClient(
        deviceId = deviceId,
        deviceName = formattedDeviceName,
        scope = scope
    )

    val discoveredMacs: StateFlow<List<DiscoveredMac>> = nsdManager.discoveredMacs
    val isDiscovering: StateFlow<Boolean> = nsdManager.isDiscovering
    val connectionState: StateFlow<ConnectionState> = wsClient.connectionState

    private val _snapshot = MutableStateFlow(CompanionStateSnapshot())
    val snapshot: StateFlow<CompanionStateSnapshot> = _snapshot.asStateFlow()

    private val _thumbnails = MutableStateFlow<Map<String, String>>(emptyMap())
    val thumbnails: StateFlow<Map<String, String>> = _thumbnails.asStateFlow()

    private val pendingThumbnailRequests = ConcurrentHashMap.newKeySet<String>()

    private val _keepScreenAwake = MutableStateFlow(prefs.getBoolean(KEY_KEEP_AWAKE, false))
    val keepScreenAwake: StateFlow<Boolean> = _keepScreenAwake.asStateFlow()

    private val _hasSeenOnboarding = MutableStateFlow(prefs.getBoolean(KEY_HAS_SEEN_ONBOARDING, false))
    val hasSeenOnboarding: StateFlow<Boolean> = _hasSeenOnboarding.asStateFlow()

    init {
        scope.launch {
            wsClient.events.collect { event ->
                handleServerEvent(event)
            }
        }
    }

    fun getLastConnectedHost(): String? = prefs.getString(KEY_LAST_HOST, null)
    fun getLastConnectedPort(): Int = prefs.getInt(KEY_LAST_PORT, CompanionWebSocketClient.DEFAULT_PORT)

    fun setKeepScreenAwake(enabled: Boolean) {
        _keepScreenAwake.value = enabled
        prefs.edit().putBoolean(KEY_KEEP_AWAKE, enabled).apply()
    }

    fun setHasSeenOnboarding(seen: Boolean) {
        _hasSeenOnboarding.value = seen
        prefs.edit().putBoolean(KEY_HAS_SEEN_ONBOARDING, seen).apply()
    }

    fun startDiscovery() {
        nsdManager.startDiscovery()
    }

    fun stopDiscovery() {
        nsdManager.stopDiscovery()
    }

    fun restartDiscovery() {
        nsdManager.restartDiscovery()
    }

    fun addManualTarget(name: String, host: String, port: Int) {
        nsdManager.addManualTarget(name, host, port)
    }

    fun connect(host: String, port: Int = CompanionWebSocketClient.DEFAULT_PORT) {
        prefs.edit()
            .putString(KEY_LAST_HOST, host)
            .putInt(KEY_LAST_PORT, port)
            .apply()
        _snapshot.value = CompanionStateSnapshot()
        wsClient.connect(host, port)
    }

    fun disconnect() {
        wsClient.disconnect()
        _snapshot.value = CompanionStateSnapshot()
        _thumbnails.value = emptyMap()
        pendingThumbnailRequests.clear()
    }

    fun clearError() {
        wsClient.clearError()
    }

    /** Re-reads the Mac's state, e.g. when the app returns to the foreground. */
    fun resync() {
        wsClient.resync()
    }

    fun saveReplay() {
        wsClient.sendAction(CompanionActions.SAVE_REPLAY)
    }

    fun saveExtendedReplay() {
        wsClient.sendAction(CompanionActions.SAVE_EXTENDED_REPLAY)
    }

    fun toggleBuffer() {
        wsClient.sendAction(CompanionActions.TOGGLE_BUFFER)
    }

    fun toggleSession() {
        wsClient.sendAction(CompanionActions.TOGGLE_SESSION)
    }

    fun toggleMicrophone() {
        wsClient.sendAction(CompanionActions.TOGGLE_MICROPHONE)
    }

    fun toggleSystemAudio() {
        wsClient.sendAction(CompanionActions.TOGGLE_SYSTEM_AUDIO)
    }

    fun revealClip(clipId: String) {
        wsClient.revealClip(clipId)
    }

    fun deleteClip(clipId: String) {
        wsClient.deleteClip(clipId)
    }

    fun requestThumbnail(clipId: String) {
        if (!_thumbnails.value.containsKey(clipId) && pendingThumbnailRequests.add(clipId)) {
            wsClient.getThumbnail(clipId)
        }
    }

    private fun handleServerEvent(event: CompanionServerEvent) {
        when (event) {
            is CompanionServerEvent.Snapshot -> {
                _snapshot.value = event.snapshot
                pruneThumbnails(event.snapshot)
                fetchMissingThumbnails(event.snapshot)
            }
            is CompanionServerEvent.StateChanged -> {
                _snapshot.value = event.snapshot
                pruneThumbnails(event.snapshot)
                fetchMissingThumbnails(event.snapshot)
            }
            is CompanionServerEvent.Telemetry -> {
                _snapshot.update { current ->
                    current.copy(
                        recordingElapsedSeconds = event.tick.recordingElapsedSeconds,
                        extendedBufferElapsedSeconds = event.tick.extendedBufferElapsedSeconds,
                        sessionElapsedSeconds = event.tick.sessionElapsedSeconds,
                        bufferedSeconds = event.tick.bufferedSeconds,
                        bufferMemoryBytes = event.tick.bufferMemoryBytes,
                        bufferMemoryFormatted = event.tick.bufferMemoryFormatted
                    )
                }
            }
            is CompanionServerEvent.Thumbnail -> {
                pendingThumbnailRequests.remove(event.clipId)
                if (event.base64 != null) {
                    _thumbnails.update { current ->
                        current + (event.clipId to event.base64)
                    }
                }
            }
            else -> {}
        }
    }

    private fun pruneThumbnails(snapshot: CompanionStateSnapshot) {
        val activeIds = snapshot.recentClips.map { it.id }.toSet()
        _thumbnails.update { current ->
            current.filterKeys { activeIds.contains(it) }
        }
    }

    private fun fetchMissingThumbnails(snapshot: CompanionStateSnapshot) {
        for (clip in snapshot.recentClips) {
            if (clip.hasThumbnail && !_thumbnails.value.containsKey(clip.id) && pendingThumbnailRequests.add(clip.id)) {
                wsClient.getThumbnail(clip.id)
            }
        }
    }
}
