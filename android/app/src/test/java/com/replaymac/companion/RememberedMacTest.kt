package com.replaymac.companion

import com.replaymac.companion.data.model.MacEndpoint
import com.replaymac.companion.data.model.RememberedMac
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RememberedMacTest {

    private val office = MacEndpoint("10.0.0.42", 7375)
    private val home = MacEndpoint("192.168.0.20", 7375)
    private val usb = MacEndpoint(MacEndpoint.USB_HOST, 7375)

    private val paired = RememberedMac.afterConnecting(null, "mac-1", "Alex’s MacBook Pro", office)

    @Test
    fun firstConnectionRemembersTheMacAndItsAddress() {
        assertEquals("mac-1", paired.serverId)
        assertEquals(office, paired.networkEndpoint)
        assertNull(paired.usbPort)
        assertFalse(paired.lastUsedUsb)
    }

    @Test
    fun connectingOverUsbKeepsTheNetworkAddress() {
        val viaUsb = RememberedMac.afterConnecting(paired, "mac-1", "Alex’s MacBook Pro", usb)
        assertEquals(office, viaUsb.networkEndpoint)
        assertEquals(7375, viaUsb.usbPort)
        assertTrue(viaUsb.lastUsedUsb)
    }

    @Test
    fun newNetworkAddressReplacesTheOldOneAndKeepsUsb() {
        val viaUsb = RememberedMac.afterConnecting(paired, "mac-1", "Alex’s MacBook Pro", usb)
        val atHome = RememberedMac.afterConnecting(viaUsb, "mac-1", "Alex’s MacBook Pro", home)
        assertEquals(home, atHome.networkEndpoint)
        assertEquals(7375, atHome.usbPort)
        assertFalse(atHome.lastUsedUsb)
    }

    @Test
    fun pairingWithADifferentMacForgetsTheOldRoutes() {
        val viaUsb = RememberedMac.afterConnecting(paired, "mac-1", "Alex’s MacBook Pro", usb)
        val other = RememberedMac.afterConnecting(viaUsb, "mac-2", "Sam’s iMac", home)
        assertEquals("mac-2", other.serverId)
        assertEquals(home, other.networkEndpoint)
        assertNull(other.usbPort)
    }

    @Test
    fun learnsTheServerIdOnceTheMacStartsSendingOne() {
        val beforeUpdate = RememberedMac.afterConnecting(null, null, "Alex’s MacBook Pro", office)
        val afterUpdate = RememberedMac.afterConnecting(beforeUpdate, "mac-1", "Alex’s MacBook Pro", office)
        assertEquals("mac-1", afterUpdate.serverId)
    }
}
