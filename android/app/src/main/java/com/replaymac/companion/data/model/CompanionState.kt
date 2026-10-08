package com.replaymac.companion.data.model

sealed interface CompanionServerEvent {
    data class Auth(val auth: CompanionAuthResponse) : CompanionServerEvent
    data class Snapshot(val snapshot: CompanionStateSnapshot) : CompanionServerEvent
    data class StateChanged(val snapshot: CompanionStateSnapshot) : CompanionServerEvent
    data class Telemetry(val tick: CompanionTelemetryTick) : CompanionServerEvent
    data class Thumbnail(val clipId: String, val base64: String?) : CompanionServerEvent
    data object Pong : CompanionServerEvent
    data class Error(val message: String) : CompanionServerEvent
    data class Unknown(val rawType: String, val rawJson: String) : CompanionServerEvent
}

sealed interface ConnectionState {
    data object Disconnected : ConnectionState
    data class Connecting(val host: String, val port: Int) : ConnectionState
    data class PendingApproval(val serverName: String, val host: String, val port: Int) : ConnectionState
    data class Connected(
        val serverName: String,
        val serverVersion: String,
        val host: String,
        val port: Int
    ) : ConnectionState
    data class Failed(val error: String, val host: String? = null, val port: Int? = null) : ConnectionState
}

data class DiscoveredMac(
    val id: String,
    val name: String,
    val host: String,
    val port: Int,
    val lastSeenTimestamp: Long = System.currentTimeMillis()
)
