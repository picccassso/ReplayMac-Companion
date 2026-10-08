package com.replaymac.companion

import com.replaymac.companion.data.connection.AutoReconnector
import com.replaymac.companion.data.model.ConnectionState
import com.replaymac.companion.data.model.DiscoveredMac
import com.replaymac.companion.data.model.MacEndpoint
import com.replaymac.companion.data.model.RememberedMac
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AutoReconnectorTest {

    private enum class Reply { Approves, Refuses, Silent, Denies }

    private val office = MacEndpoint("192.168.8.239", 7375)
    private val home = MacEndpoint("192.168.68.74", 7375)
    private val usb = MacEndpoint(MacEndpoint.USB_HOST, 7375)
    private val mac = RememberedMac("mac-1", "Alex’s MacBook Pro", office, usbPort = 7375, lastUsedUsb = false)

    /** Plays the socket: each address answers a connect the way [replies] says. */
    private inner class FakeNetwork(private val scope: TestScope) {
        val state = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
        val discovered = MutableStateFlow<List<DiscoveredMac>>(emptyList())
        val replies = mutableMapOf<MacEndpoint, Reply>()
        val attempts = mutableListOf<MacEndpoint>()
        var denied = false

        val reconnector = AutoReconnector(
            scope = scope.backgroundScope,
            connectionState = state,
            discoveredMacs = discovered,
            connect = ::connect,
            onPairingDenied = { denied = true }
        )

        private fun connect(endpoint: MacEndpoint) {
            attempts += endpoint
            state.value = ConnectionState.Connecting(endpoint.host, endpoint.port)
            state.value = when (replies[endpoint] ?: Reply.Refuses) {
                Reply.Approves -> ConnectionState.Connected(mac.name, "1.7.3", endpoint.host, endpoint.port, "mac-1")
                Reply.Refuses -> ConnectionState.Failed("refused", endpoint.host, endpoint.port)
                Reply.Silent -> return
                Reply.Denies -> ConnectionState.Failed("denied", endpoint.host, endpoint.port, pairingDenied = true)
            }
        }

        fun macAppearsAt(endpoint: MacEndpoint) {
            discovered.value = listOf(DiscoveredMac("${endpoint.host}:${endpoint.port}", mac.name, endpoint.host, endpoint.port, "mac-1"))
        }
    }

    @Test
    fun connectsToTheMacWhereDiscoverySeesItNow() = runTest {
        val net = FakeNetwork(this)
        net.replies[home] = Reply.Approves
        net.macAppearsAt(home)

        net.reconnector.start(mac)
        runCurrent()

        assertEquals(listOf(home), net.attempts)
        assertNull("Idle once connected", net.reconnector.target.value)
    }

    @Test
    fun fallsBackToUsbWhenTheOldNetworkAddressIsGone() = runTest {
        val net = FakeNetwork(this)
        net.replies[usb] = Reply.Approves

        net.reconnector.start(mac)
        runCurrent()

        assertEquals(listOf(office, usb), net.attempts)
        assertTrue(net.state.value is ConnectionState.Connected)
    }

    @Test
    fun abandonsAHangingAddressAsSoonAsTheMacIsFoundElsewhere() = runTest {
        val net = FakeNetwork(this)
        net.replies[office] = Reply.Silent
        net.replies[home] = Reply.Approves

        net.reconnector.start(mac)
        runCurrent()
        assertEquals(listOf(office), net.attempts)

        advanceTimeBy(1_000) // well inside the attempt timeout
        net.macAppearsAt(home)
        runCurrent()

        assertEquals(listOf(office, home), net.attempts)
        assertTrue(net.state.value is ConnectionState.Connected)
    }

    @Test
    fun keepsLookingAfterEveryAddressFailsAndWakesWhenTheMacAppears() = runTest {
        val net = FakeNetwork(this)
        net.replies[home] = Reply.Approves

        net.reconnector.start(mac)
        runCurrent()
        assertEquals(listOf(office, usb), net.attempts)
        assertEquals(mac, net.reconnector.target.value)

        net.macAppearsAt(home) // during the backoff wait
        runCurrent()

        assertEquals(listOf(office, usb, home), net.attempts)
        assertNull(net.reconnector.target.value)
    }

    @Test
    fun retriesKnownAddressesAfterTheBackoff() = runTest {
        val net = FakeNetwork(this)

        net.reconnector.start(mac)
        runCurrent()
        net.replies[office] = Reply.Approves // e.g. the Mac woke up
        advanceTimeBy(1_001)
        runCurrent()

        assertEquals(listOf(office, usb, office), net.attempts)
        assertTrue(net.state.value is ConnectionState.Connected)
    }

    @Test
    fun stopsAndForgetsWhenTheMacDeniesThePhone() = runTest {
        val net = FakeNetwork(this)
        net.replies[office] = Reply.Denies

        net.reconnector.start(mac)
        runCurrent()
        advanceTimeBy(60_000)

        assertEquals(listOf(office), net.attempts)
        assertTrue(net.denied)
        assertNull(net.reconnector.target.value)
    }

    @Test
    fun stopEndsTheSearch() = runTest {
        val net = FakeNetwork(this)

        net.reconnector.start(mac)
        runCurrent()
        net.reconnector.stop()
        advanceTimeBy(60_000)

        assertEquals(listOf(office, usb), net.attempts)
        assertNull(net.reconnector.target.value)
    }
}
