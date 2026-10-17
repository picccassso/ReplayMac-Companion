package com.replaymac.companion.data.network

import android.os.Build
import android.util.Log
import com.replaymac.companion.data.model.CompanionClientMessage
import com.replaymac.companion.data.model.CompanionServerEvent
import com.replaymac.companion.data.model.CompanionVolumeTargets
import com.replaymac.companion.data.model.ConnectionState
import com.replaymac.companion.data.repository.PairingSecretStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
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
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.TimeUnit

class CompanionWebSocketClient(
    private val deviceId: String,
    private val secrets: PairingSecretStore,
    private val deviceName: String = "${Build.MANUFACTURER} ${Build.MODEL}",
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO),
    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .pingInterval(15, TimeUnit.SECONDS)
        // Short, so trying a remembered Mac's old address on a new network fails fast.
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS) // Indefinite for WebSocket
        .build()
) : CompanionLink {
    companion object {
        private const val TAG = "CompanionWSClient"
        const val DEFAULT_PORT = 7375
    }

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    // No replay: a late subscriber must never receive a stale snapshot.
    private val _events = MutableSharedFlow<CompanionServerEvent>(extraBufferCapacity = 64)
    override val events: SharedFlow<CompanionServerEvent> = _events.asSharedFlow()

    private var currentWebSocket: WebSocket? = null
    /** Bumped on every connect and disconnect; callbacks from an older generation are ignored. */
    @Volatile private var generation = 0
    private var pingJob: Job? = null
    private var currentHost: String? = null
    private var currentPort: Int = DEFAULT_PORT

    fun connect(host: String, port: Int = DEFAULT_PORT) {
        disconnect()

        currentHost = host
        currentPort = port
        _connectionState.value = ConnectionState.Connecting(host, port)

        val cleanHost = host.trim().let { if (it.contains("%")) it.substringBefore("%") else it }
        val formattedHost = if (cleanHost.contains(":") && !cleanHost.startsWith("[")) "[$cleanHost]" else cleanHost
        val url = "ws://$formattedHost:$port"
        Log.d(TAG, "Connecting to $url")

        val request = try {
            Request.Builder()
                .url(url)
                .build()
        } catch (e: Exception) {
            Log.e(TAG, "Invalid WebSocket URL: $url", e)
            _connectionState.value = ConnectionState.Failed("Invalid host address: $host", host, port)
            return
        }

        val socketGeneration = ++generation
        val session = CompanionSession(deviceId, host, port, secrets)
        currentWebSocket = okHttpClient.newWebSocket(request, object : WebSocketListener() {
            // OkHttp keeps calling back on a socket after it has been replaced or
            // cancelled. Those late callbacks must not touch the current
            // connection: a cancelled socket reports "Socket closed" and could
            // overwrite fresh state with an old snapshot.
            private fun isCurrent() = socketGeneration == generation

            /** True once the Mac has approved this socket, so a later failure is a lost connection. */
            private fun wasConnected(): Boolean {
                val state = _connectionState.value
                return state is ConnectionState.Connected && state.host == host && state.port == port
            }

            override fun onOpen(webSocket: WebSocket, response: Response) {
                if (!isCurrent()) return
                Log.d(TAG, "WebSocket opened with $host:$port")
                sendHello()
                startPingLoop()
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                if (!isCurrent()) return
                // Auth replies can carry the pairing secret, which must never reach logcat.
                if (!text.contains("pairingSecret")) Log.v(TAG, "Received message: $text")
                handleIncomingMessage(text, session)
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "WebSocket closing: $code / $reason")
                webSocket.close(1000, null)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                if (!isCurrent()) return
                Log.d(TAG, "WebSocket closed: $code / $reason")
                stopPingLoop()
                if (_connectionState.value !is ConnectionState.Failed) {
                    _connectionState.value = session.closedState(wasConnected())
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                if (!isCurrent()) return
                Log.e(TAG, "WebSocket failure: ${t.message}", t)
                stopPingLoop()
                if (_connectionState.value !is ConnectionState.Failed) {
                    _connectionState.value = session.failedState(t, wasConnected())
                }
            }
        })
    }

    fun disconnect() {
        stopPingLoop()
        // Retire the socket first so its closing callbacks are ignored.
        generation++
        val socket = currentWebSocket
        currentWebSocket = null
        try {
            socket?.close(1000, "User disconnected")
        } catch (_: Exception) {}
        socket?.cancel()
        _connectionState.value = ConnectionState.Disconnected
    }

    /**
     * Asks the Mac for a fresh snapshot. ReplayMac answers a hello from an
     * already-paired device with its current state, so this catches up on
     * anything missed while the phone was asleep or in the background.
     */
    override fun resync(): Boolean {
        if (_connectionState.value !is ConnectionState.Connected) return false
        return sendMessage(CompanionClientMessage.hello(deviceId = deviceId, deviceName = deviceName, clientVersion = "1.0.0"))
    }

    fun clearError() {
        if (_connectionState.value is ConnectionState.Failed) {
            _connectionState.value = ConnectionState.Disconnected
        }
    }

    fun sendHello() {
        val helloMsg = CompanionClientMessage.hello(
            deviceId = deviceId,
            deviceName = deviceName,
            clientVersion = "1.0.0"
        )
        sendMessage(helloMsg)
    }

    override fun sendAction(actionName: String): Boolean {
        return sendMessage(CompanionClientMessage.action(actionName))
    }

    override fun getThumbnail(clipId: String): Boolean {
        return sendMessage(CompanionClientMessage.getThumbnail(clipId))
    }

    override fun revealClip(clipId: String): Boolean {
        return sendMessage(CompanionClientMessage.revealClip(clipId))
    }

    override fun deleteClip(clipId: String): Boolean {
        return sendMessage(CompanionClientMessage.deleteClip(clipId))
    }

    override fun trimClip(clipId: String): Boolean {
        return sendMessage(CompanionClientMessage.trimClip(clipId))
    }

    override fun copyClip(clipId: String): Boolean {
        return sendMessage(CompanionClientMessage.copyClip(clipId))
    }

    override fun setVolume(target: String, volume: Double): Boolean {
        return sendMessage(CompanionClientMessage.setVolume(target, volume))
    }

    fun setMicrophoneVolume(volume: Double): Boolean {
        return setVolume(CompanionVolumeTargets.MICROPHONE, volume)
    }

    fun setSystemAudioVolume(volume: Double): Boolean {
        return setVolume(CompanionVolumeTargets.SYSTEM_AUDIO, volume)
    }

    override fun toggleAppMute(bundleId: String): Boolean {
        return sendMessage(CompanionClientMessage.toggleAppMute(bundleId))
    }

    override fun unmuteAllApps(): Boolean {
        return sendMessage(CompanionClientMessage.unmuteAllApps())
    }

    override fun selectDisplay(displayId: String): Boolean {
        return sendMessage(CompanionClientMessage.selectDisplay(displayId))
    }

    override fun applyProfile(profileId: String): Boolean {
        return sendMessage(CompanionClientMessage.applyProfile(profileId))
    }

    fun ping(): Boolean {
        return sendMessage(CompanionClientMessage.ping())
    }

    private fun sendMessage(msg: CompanionClientMessage): Boolean {
        val ws = currentWebSocket ?: return false
        val jsonString = CompanionProtocolParser.encodeClientMessage(msg)
        if (msg.proof == null) Log.v(TAG, "Sending message: $jsonString")
        return ws.send(jsonString)
    }

    private fun handleIncomingMessage(text: String, session: CompanionSession) {
        val event = CompanionProtocolParser.parse(text)
        if (event is CompanionServerEvent.Error) {
            Log.w(TAG, "Server reported error: ${event.message} (${event.code})")
        }

        val step = session.handle(event)
        step.reply?.let(::sendMessage)
        step.state?.let { _connectionState.value = it }

        val emitted = _events.tryEmit(event)
        if (!emitted) {
            scope.launch {
                _events.emit(event)
            }
        }
    }

    private fun startPingLoop() {
        stopPingLoop()
        pingJob = scope.launch {
            while (isActive) {
                delay(20_000)
                ping()
            }
        }
    }

    private fun stopPingLoop() {
        pingJob?.cancel()
        pingJob = null
    }
}
