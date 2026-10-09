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
    /** Finding a remembered Mac again, on whichever network or cable it is reachable. */
    data class Reconnecting(val serverName: String) : ConnectionState
    data class Connected(
        val serverName: String,
        val serverVersion: String,
        val host: String,
        val port: Int,
        val serverId: String? = null,
        val deviceName: String? = null
    ) : ConnectionState
    /** [wasConnected] separates a dropped connection from one that never opened. */
    data class Failed(
        val error: String,
        val host: String? = null,
        val port: Int? = null,
        val wasConnected: Boolean = false,
        /** The Mac refused or revoked this phone, so it must not be retried automatically. */
        val pairingDenied: Boolean = false
    ) : ConnectionState
}

data class DiscoveredMac(
    val id: String,
    val name: String,
    val host: String,
    val port: Int,
    /** ReplayMac's stable identity from its Bonjour TXT record, when it advertises one. */
    val serverId: String? = null,
    val lastSeenTimestamp: Long = System.currentTimeMillis()
)
