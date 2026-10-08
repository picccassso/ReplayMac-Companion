package com.replaymac.companion.data.network

import android.os.Build
import android.util.Log
import com.replaymac.companion.data.model.CompanionClientMessage
import com.replaymac.companion.data.model.CompanionServerEvent
import com.replaymac.companion.data.model.ConnectionState
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
    private val deviceName: String = "${Build.MANUFACTURER} ${Build.MODEL}",
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO),
    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .pingInterval(15, TimeUnit.SECONDS)
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS) // Indefinite for WebSocket
        .build()
) {
    companion object {
        private const val TAG = "CompanionWSClient"
        const val DEFAULT_PORT = 7375
    }

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _events = MutableSharedFlow<CompanionServerEvent>(replay = 1, extraBufferCapacity = 64)
    val events: SharedFlow<CompanionServerEvent> = _events.asSharedFlow()

    private var currentWebSocket: WebSocket? = null
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

        currentWebSocket = okHttpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d(TAG, "WebSocket opened with $host:$port")
                sendHello()
                startPingLoop()
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                Log.v(TAG, "Received message: $text")
                handleIncomingMessage(text, host, port)
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "WebSocket closing: $code / $reason")
                webSocket.close(1000, null)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "WebSocket closed: $code / $reason")
                stopPingLoop()
                if (_connectionState.value !is ConnectionState.Failed) {
                    _connectionState.value = ConnectionState.Disconnected
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e(TAG, "WebSocket failure: ${t.message}", t)
                stopPingLoop()
                if (_connectionState.value !is ConnectionState.Failed) {
                    _connectionState.value = ConnectionState.Failed(t.message ?: "Connection failed", host, port)
                }
            }
        })
    }

    fun disconnect() {
        stopPingLoop()
        try {
            currentWebSocket?.close(1000, "User disconnected")
        } catch (_: Exception) {}
        currentWebSocket?.cancel()
        currentWebSocket = null
        _connectionState.value = ConnectionState.Disconnected
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

    fun sendAction(actionName: String): Boolean {
        return sendMessage(CompanionClientMessage.action(actionName))
    }

    fun getThumbnail(clipId: String): Boolean {
        return sendMessage(CompanionClientMessage.getThumbnail(clipId))
    }

    fun revealClip(clipId: String): Boolean {
        return sendMessage(CompanionClientMessage.revealClip(clipId))
    }

    fun deleteClip(clipId: String): Boolean {
        return sendMessage(CompanionClientMessage.deleteClip(clipId))
    }

    fun ping(): Boolean {
        return sendMessage(CompanionClientMessage.ping())
    }

    private fun sendMessage(msg: CompanionClientMessage): Boolean {
        val ws = currentWebSocket ?: return false
        val jsonString = CompanionProtocolParser.encodeClientMessage(msg)
        Log.v(TAG, "Sending message: $jsonString")
        return ws.send(jsonString)
    }

    private fun handleIncomingMessage(text: String, host: String, port: Int) {
        val event = CompanionProtocolParser.parse(text)

        when (event) {
            is CompanionServerEvent.Auth -> {
                when (event.auth.status.lowercase()) {
                    "approved" -> {
                        _connectionState.value = ConnectionState.Connected(
                            serverName = event.auth.serverName,
                            serverVersion = event.auth.serverVersion,
                            host = host,
                            port = port
                        )
                    }
                    "pending" -> {
                        _connectionState.value = ConnectionState.PendingApproval(
                            serverName = event.auth.serverName,
                            host = host,
                            port = port
                        )
                    }
                    "denied" -> {
                        _connectionState.value = ConnectionState.Failed(
                            error = "Pairing denied by ${event.auth.serverName}",
                            host = host,
                            port = port
                        )
                    }
                }
            }
            is CompanionServerEvent.Error -> {
                Log.w(TAG, "Server reported error: ${event.message}")
            }
            else -> {
                // Snapshot, state changed, telemetry, thumbnail, pong, etc.
            }
        }

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
