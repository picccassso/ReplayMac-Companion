package com.replaymac.companion.data.model

/** A host and port the companion can open a socket to. */
data class MacEndpoint(val host: String, val port: Int) {
    val isUsb: Boolean get() = host == USB_HOST

    companion object {
        /** `adb reverse` exposes the Mac's server on the phone's loopback address. */
        const val USB_HOST = "127.0.0.1"
    }
}

/**
 * The Mac this phone last paired with, remembered by identity rather than by
 * address so it can be found again on any network or over USB.
 */
data class RememberedMac(
    /** ReplayMac's stable server id; null when paired with a build that doesn't send one. */
    val serverId: String?,
    val name: String,
    /** Last address the Mac was reached at over a network. */
    val networkEndpoint: MacEndpoint?,
    /** Port last used over USB, if this Mac was ever reached that way. */
    val usbPort: Int?,
    /** Whether the most recent connection went over USB rather than the network. */
    val lastUsedUsb: Boolean
) {
    companion object {
        /**
         * What to remember after connecting to [serverName] at [endpoint]. Keeps
         * the other route (network or USB) when it's the same Mac as [previous],
         * and starts afresh when the phone has paired with a different one.
         */
        fun afterConnecting(
            previous: RememberedMac?,
            serverId: String?,
            serverName: String,
            endpoint: MacEndpoint
        ): RememberedMac {
            val base = previous?.takeIf { it.isSameMacAs(serverId, serverName) }
                ?: RememberedMac(serverId, serverName, networkEndpoint = null, usbPort = null, lastUsedUsb = false)
            return base.copy(serverId = serverId ?: base.serverId, name = serverName).reachedAt(endpoint)
        }
    }

    /**
     * Matches by server id when both sides have one, otherwise by name (older
     * ReplayMac builds send no id).
     */
    fun isSameMacAs(otherServerId: String?, otherName: String): Boolean =
        if (serverId != null && otherServerId != null) serverId == otherServerId else name == otherName

    /** Remembers [endpoint] as the latest way this Mac was reached. */
    fun reachedAt(endpoint: MacEndpoint): RememberedMac = if (endpoint.isUsb) {
        copy(usbPort = endpoint.port, lastUsedUsb = true)
    } else {
        copy(networkEndpoint = endpoint, lastUsedUsb = false)
    }
}
