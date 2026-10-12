package com.replaymac.companion.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.util.Log
import com.replaymac.companion.data.connection.AutoReconnector
import com.replaymac.companion.data.model.CompanionActions
import com.replaymac.companion.data.model.CompanionServerEvent
import com.replaymac.companion.data.model.CompanionStateSnapshot
import com.replaymac.companion.data.model.ConnectionState
import com.replaymac.companion.data.model.DiscoveredMac
import com.replaymac.companion.data.model.MacEndpoint
import com.replaymac.companion.data.model.RememberedMac
import com.replaymac.companion.data.network.CompanionWebSocketClient
import com.replaymac.companion.data.network.NsdDiscoveryManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class CompanionRepository(
    context: Context,
    // Supervised, so one failing job can't silently stop the state collectors.
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
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

    private val pairingSecrets = PairingSecretStore(
        context.getSharedPreferences(PairingSecretStore.PREFS_NAME, Context.MODE_PRIVATE)
    )

    private val nsdManager = NsdDiscoveryManager(context, scope)
    private val wsClient = CompanionWebSocketClient(
        deviceId = deviceId,
        secrets = pairingSecrets,
        deviceName = formattedDeviceName,
        scope = scope
    )

    val discoveredMacs: StateFlow<List<DiscoveredMac>> = nsdManager.discoveredMacs
    val isDiscovering: StateFlow<Boolean> = nsdManager.isDiscovering
    private val rememberedMacStore = RememberedMacStore(prefs)

    private val _previouslyConnectedMacs = MutableStateFlow<List<RememberedMac>>(rememberedMacStore.loadAll())
    val previouslyConnectedMacs: StateFlow<List<RememberedMac>> = _previouslyConnectedMacs.asStateFlow()

    private val reconnector = AutoReconnector(
        scope = scope,
        connectionState = wsClient.connectionState,
        discoveredMacs = nsdManager.discoveredMacs,
        connect = ::openSocket,
        onPairingDenied = {
            rememberedMacStore.clear()
            _previouslyConnectedMacs.value = emptyList()
        }
    )

    /** Only look for the Mac while the app is on screen; it's cheap then and pointless otherwise. */
    @Volatile private var isForeground = false

    /** The socket's state, shown as [ConnectionState.Reconnecting] while the remembered Mac is being found. */
    val connectionState: StateFlow<ConnectionState> =
        combine(wsClient.connectionState, reconnector.target) { state, target ->
            val settled = state is ConnectionState.Connected || state is ConnectionState.PendingApproval
            if (target != null && !settled) ConnectionState.Reconnecting(target.name) else state
        }.stateIn(scope, SharingStarted.Eagerly, wsClient.connectionState.value)

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
        scope.launch {
            wsClient.connectionState.collect(::onSocketStateChanged)
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

    /** A connection the user chose; it replaces any automatic reconnect in progress. */
    fun connect(host: String, port: Int = CompanionWebSocketClient.DEFAULT_PORT) {
        reconnector.stop()
        openSocket(MacEndpoint(host, port))
    }

    fun disconnect() {
        reconnector.stop()
        wsClient.disconnect()
        _snapshot.value = CompanionStateSnapshot()
        _thumbnails.value = emptyMap()
        pendingThumbnailRequests.clear()
    }

    fun clearError() {
        wsClient.clearError()
    }

    /**
     * The app came on screen: re-read the Mac's state if connected (it may have
     * changed while away), otherwise go and find the remembered Mac.
     */
    fun onAppForegrounded() {
        isForeground = true
        _previouslyConnectedMacs.value = rememberedMacStore.loadAll()
        when (wsClient.connectionState.value) {
            is ConnectionState.Connected -> wsClient.resync()
            is ConnectionState.Disconnected, is ConnectionState.Failed -> reconnectToRememberedMac()
            else -> {}
        }
    }

    /** Stops looking for the Mac while off screen. An open connection is kept. */
    fun onAppBackgrounded() {
        isForeground = false
        if (reconnector.target.value != null) {
            reconnector.stop()
            wsClient.disconnect()
        }
    }

    fun forgetMac(mac: RememberedMac) {
        rememberedMacStore.remove(mac)
        mac.serverId?.let(pairingSecrets::remove)
        _previouslyConnectedMacs.value = rememberedMacStore.loadAll()
    }

    private fun reconnectToRememberedMac() {
        val mac = rememberedMacStore.load() ?: return
        if (isForeground) reconnector.start(mac)
    }

    private fun onSocketStateChanged(state: ConnectionState) {
        when {
            state is ConnectionState.Connected -> rememberConnectedMac(state)
            state is ConnectionState.Failed && state.pairingDenied -> {
                rememberedMacStore.clear()
                _previouslyConnectedMacs.value = emptyList()
            }
            state is ConnectionState.Failed && state.wasConnected -> reconnectToRememberedMac()
        }
    }

    private fun rememberConnectedMac(state: ConnectionState.Connected) {
        rememberedMacStore.save(
            RememberedMac.afterConnecting(
                previous = rememberedMacStore.load(),
                serverId = state.serverId,
                serverName = state.serverName,
                endpoint = MacEndpoint(state.host, state.port)
            )
        )
        _previouslyConnectedMacs.value = rememberedMacStore.loadAll()
    }

    private fun openSocket(endpoint: MacEndpoint) {
        // Prefill the manual form with the last network address, never the USB loopback.
        if (!endpoint.isUsb) {
            prefs.edit()
                .putString(KEY_LAST_HOST, endpoint.host)
                .putInt(KEY_LAST_PORT, endpoint.port)
                .apply()
        }
        _snapshot.value = CompanionStateSnapshot()
        wsClient.connect(endpoint.host, endpoint.port)
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
