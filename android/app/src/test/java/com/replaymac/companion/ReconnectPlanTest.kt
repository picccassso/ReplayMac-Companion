package com.replaymac.companion

import com.replaymac.companion.data.connection.ReconnectPlan
import com.replaymac.companion.data.model.DiscoveredMac
import com.replaymac.companion.data.model.MacEndpoint
import com.replaymac.companion.data.model.RememberedMac
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReconnectPlanTest {

    private val office = MacEndpoint("10.0.0.42", 7375)
    private val usb = MacEndpoint(MacEndpoint.USB_HOST, 7375)

    private val mac = RememberedMac(
        serverId = "mac-1",
        name = "Alex’s MacBook Pro",
        networkEndpoint = office,
        usbPort = 7375,
        lastUsedUsb = false
    )

    private fun discovered(host: String, serverId: String? = "mac-1", name: String = mac.name) =
        DiscoveredMac(id = "$host:7375", name = name, host = host, port = 7375, serverId = serverId)

    @Test
    fun macSeenOnTheCurrentNetworkIsTriedBeforeItsOldAddress() {
        val home = discovered("192.168.0.20")
        assertEquals(
            listOf(MacEndpoint("192.168.0.20", 7375), office, usb),
            ReconnectPlan.candidates(mac, listOf(home))
        )
    }

    @Test
    fun usbComesFirstWhenThatWasTheLastRoute() {
        assertEquals(listOf(usb, office), ReconnectPlan.candidates(mac.copy(lastUsedUsb = true), emptyList()))
    }

    @Test
    fun anotherMacOnTheNetworkIsNotACandidate() {
        val colleague = discovered("192.168.0.30", serverId = "mac-2", name = "Sam’s iMac")
        assertEquals(listOf(office, usb), ReconnectPlan.candidates(mac, listOf(colleague)))
    }

    @Test
    fun sameAddressIsOnlyTriedOnce() {
        assertEquals(listOf(office, usb), ReconnectPlan.candidates(mac, listOf(discovered(office.host))))
    }

    @Test
    fun matchesByServerIdEvenWhenTheMacIsRenamed() {
        assertTrue(ReconnectPlan.isSameMac(discovered("10.0.0.2", name = "Renamed Mac"), mac))
        assertFalse(ReconnectPlan.isSameMac(discovered("10.0.0.2", serverId = "mac-2"), mac))
    }

    @Test
    fun fallsBackToNameForMacsThatAdvertiseNoId() {
        assertTrue(ReconnectPlan.isSameMac(discovered("10.0.0.2", serverId = null), mac))
        assertFalse(ReconnectPlan.isSameMac(discovered("10.0.0.2", serverId = null, name = "Other"), mac))
    }

    @Test
    fun backoffDoublesThenCaps() {
        assertEquals(listOf(1_000L, 2_000L, 4_000L, 8_000L, 15_000L, 15_000L), (0..5).map(ReconnectPlan::backoffMillis))
    }
}
