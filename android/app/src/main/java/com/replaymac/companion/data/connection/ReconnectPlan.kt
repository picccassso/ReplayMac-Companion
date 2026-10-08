package com.replaymac.companion.data.connection

import com.replaymac.companion.data.model.DiscoveredMac
import com.replaymac.companion.data.model.MacEndpoint
import com.replaymac.companion.data.model.RememberedMac

/** Decides where to look for a remembered Mac, and how long to wait between rounds. */
object ReconnectPlan {
    private const val FIRST_BACKOFF_MS = 1_000L
    private const val MAX_BACKOFF_MS = 15_000L

    fun isSameMac(discovered: DiscoveredMac, remembered: RememberedMac): Boolean =
        remembered.isSameMacAs(discovered.serverId, discovered.name)

    /**
     * Addresses to try, best first: wherever discovery currently sees the Mac
     * on this network, then the way it was last reached (network or USB),
     * then the other one.
     */
    fun candidates(remembered: RememberedMac, discovered: List<DiscoveredMac>): List<MacEndpoint> {
        val seenNow = discovered
            .filter { isSameMac(it, remembered) }
            .map { MacEndpoint(it.host, it.port) }
        val usb = remembered.usbPort?.let { MacEndpoint(MacEndpoint.USB_HOST, it) }
        val previous = if (remembered.lastUsedUsb) {
            listOfNotNull(usb, remembered.networkEndpoint)
        } else {
            listOfNotNull(remembered.networkEndpoint, usb)
        }
        return (seenNow + previous).distinct()
    }

    /** 1s, 2s, 4s, 8s, then every 15s. */
    fun backoffMillis(round: Int): Long {
        val doubled = FIRST_BACKOFF_MS shl round.coerceIn(0, 4)
        return doubled.coerceAtMost(MAX_BACKOFF_MS)
    }
}
