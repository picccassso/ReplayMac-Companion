package com.replaymac.companion

import com.replaymac.companion.data.model.CompanionClipSummary
import com.replaymac.companion.data.model.CompanionStateSnapshot
import com.replaymac.companion.data.model.CompanionTelemetryTick
import com.replaymac.companion.data.model.ConnectionState
import com.replaymac.companion.data.model.DiscoveredMac
import com.replaymac.companion.data.model.MacEndpoint
import com.replaymac.companion.data.model.PreviouslyConnectedItem
import com.replaymac.companion.data.model.RememberedMac
import com.replaymac.companion.ui.components.clipKindTitle
import com.replaymac.companion.ui.components.formatDurationSeconds
import com.replaymac.companion.ui.components.formatFileSize
import com.replaymac.companion.ui.screens.formatConnectedStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CompanionConnectionLogicTest {

    @Test
    fun testTelemetryMergingIntoSnapshot() {
        var snapshot = CompanionStateSnapshot(
            isRecording = true,
            bufferDurationSeconds = 30,
            bufferedSeconds = 10.0,
            bufferMemoryBytes = 20000000,
            bufferMemoryFormatted = "20 MB",
            recordingElapsedSeconds = 10.0
        )

        val tick = CompanionTelemetryTick(
            recordingElapsedSeconds = 15.5,
            extendedBufferElapsedSeconds = 45.0,
            sessionElapsedSeconds = 0.0,
            bufferedSeconds = 15.5,
            bufferMemoryBytes = 35000000,
            bufferMemoryFormatted = "35 MB"
        )

        snapshot = snapshot.copy(
            recordingElapsedSeconds = tick.recordingElapsedSeconds,
            extendedBufferElapsedSeconds = tick.extendedBufferElapsedSeconds,
            sessionElapsedSeconds = tick.sessionElapsedSeconds,
            bufferedSeconds = tick.bufferedSeconds,
            bufferMemoryBytes = tick.bufferMemoryBytes,
            bufferMemoryFormatted = tick.bufferMemoryFormatted
        )

        assertEquals(15.5, snapshot.recordingElapsedSeconds, 0.01)
        assertEquals(35000000L, snapshot.bufferMemoryBytes)
        assertEquals("35 MB", snapshot.bufferMemoryFormatted)
        assertEquals(30, snapshot.bufferDurationSeconds) // unchanged
    }

    @Test
    fun testDurationFormatter() {
        // Same output as the desktop's MenuBarState.formattedDuration.
        assertEquals("00:00", formatDurationSeconds(0.0))
        assertEquals("00:05", formatDurationSeconds(5.2))
        assertEquals("00:59", formatDurationSeconds(59.9))
        assertEquals("01:00", formatDurationSeconds(60.0))
        assertEquals("05:30", formatDurationSeconds(330.0))
        assertEquals("01:00:00", formatDurationSeconds(3600.0))
        assertEquals("02:05:10", formatDurationSeconds(7510.0))
        assertEquals("00:00", formatDurationSeconds(-10.0))
    }

    @Test
    fun testFileSizeFormatter() {
        assertEquals("", formatFileSize(null))
        assertEquals("", formatFileSize(0L))
        assertEquals("450 KB", formatFileSize(450 * 1024L))
        assertEquals("5.0 MB", formatFileSize(5L * 1024 * 1024))
        assertEquals("1.5 GB", formatFileSize((1.5 * 1024 * 1024 * 1024).toLong()))
    }

    @Test
    fun testClipKindFormatting() {
        assertEquals("Replay", clipKindTitle("replay"))
        assertEquals("Session", clipKindTitle("session"))
        assertEquals("Extended Replay", clipKindTitle("extended"))
        assertEquals("Extended Replay", clipKindTitle("extended replay"))
        assertEquals("Custom", clipKindTitle("custom"))
    }

    @Test
    fun testConnectionStates() {
        val disconnected: ConnectionState = ConnectionState.Disconnected
        assertTrue(disconnected is ConnectionState.Disconnected)

        val connecting = ConnectionState.Connecting("192.168.1.5", 7375)
        assertEquals("192.168.1.5", connecting.host)
        assertEquals(7375, connecting.port)

        val pending = ConnectionState.PendingApproval("Studio Mac", "192.168.1.5", 7375)
        assertEquals("Studio Mac", pending.serverName)

        val connected = ConnectionState.Connected(
            serverName = "Studio Mac",
            serverVersion = "1.7.3",
            host = "192.168.1.5",
            port = 7375,
            serverId = "srv-123",
            deviceName = "Pixel 9 Pro"
        )
        assertEquals("Studio Mac", connected.serverName)
        assertEquals("1.7.3", connected.serverVersion)
        assertEquals("192.168.1.5", connected.host)
        assertEquals(7375, connected.port)
        assertEquals("srv-123", connected.serverId)
        assertEquals("Pixel 9 Pro", connected.deviceName)

        val defaultConnected = ConnectionState.Connected("Studio Mac", "1.7.3", "192.168.1.5", 7375)
        assertNull(defaultConnected.serverId)
        assertNull(defaultConnected.deviceName)

        val failed = ConnectionState.Failed("Connection refused", "127.0.0.1", 7375)
        assertEquals("Connection refused", failed.error)
    }

    @Test
    fun testPreviouslyConnectedOnlineOfflineResolution() {
        val savedMacOnline = RememberedMac(
            serverId = "srv-1",
            name = "Office Mac",
            networkEndpoint = MacEndpoint("192.168.1.10", 7375),
            usbPort = null,
            lastUsedUsb = false
        )
        val savedMacOffline = RememberedMac(
            serverId = "srv-2",
            name = "Home Mac",
            networkEndpoint = MacEndpoint("192.168.1.20", 7375),
            usbPort = null,
            lastUsedUsb = false
        )
        val savedMacByName = RememberedMac(
            serverId = null,
            name = "Legacy Studio",
            networkEndpoint = MacEndpoint("192.168.1.30", 7375),
            usbPort = null,
            lastUsedUsb = false
        )

        val discoveredMacs = listOf(
            DiscoveredMac(
                id = "disc-1",
                name = "Office Mac (Renamed)",
                host = "192.168.1.15",
                port = 7375,
                serverId = "srv-1"
            ),
            DiscoveredMac(
                id = "disc-3",
                name = "Legacy Studio",
                host = "192.168.1.35",
                port = 7375,
                serverId = null
            )
        )

        val savedList = listOf(savedMacOnline, savedMacOffline, savedMacByName)
        val items = savedList.map { mac ->
            val match = discoveredMacs.firstOrNull { mac.isSameMacAs(it) }
            val onlineEndpoint = match?.let { MacEndpoint(it.host, it.port) }
            PreviouslyConnectedItem(
                mac = mac,
                isOnline = match != null,
                onlineEndpoint = onlineEndpoint
            )
        }

        assertEquals(3, items.size)

        // Item 1: Online matched by serverId, endpoint reflects current discovered network address
        val item1 = items[0]
        assertEquals("srv-1", item1.id)
        assertTrue(item1.isOnline)
        assertNotNull(item1.onlineEndpoint)
        assertEquals("192.168.1.15", item1.onlineEndpoint?.host)

        // Item 2: Offline since not in discoveredMacs
        val item2 = items[1]
        assertEquals("srv-2", item2.id)
        assertFalse(item2.isOnline)
        assertNull(item2.onlineEndpoint)

        // Item 3: Online matched by name since both have null serverId
        val item3 = items[2]
        assertEquals("Legacy Studio", item3.id)
        assertTrue(item3.isOnline)
        assertNotNull(item3.onlineEndpoint)
        assertEquals("192.168.1.35", item3.onlineEndpoint?.host)
    }

    @Test
    fun testDiscoveredMacsFilteringExcludesPreviouslyConnected() {
        val savedMac = RememberedMac(
            serverId = "srv-1",
            name = "Office Mac",
            networkEndpoint = MacEndpoint("192.168.1.10", 7375),
            usbPort = null,
            lastUsedUsb = false
        )
        val savedList = listOf(savedMac)

        val discoveredOnline1 = DiscoveredMac(
            id = "disc-1",
            name = "Office Mac",
            host = "192.168.1.10",
            port = 7375,
            serverId = "srv-1"
        )
        val discoveredNew = DiscoveredMac(
            id = "disc-2",
            name = "New Discovered Mac",
            host = "192.168.1.99",
            port = 7375,
            serverId = "srv-new"
        )

        val allDiscovered = listOf(discoveredOnline1, discoveredNew)

        // Filter: exclude any discovered Mac that matches an entry in previouslyConnectedMacs
        val filtered = allDiscovered.filter { d -> savedList.none { it.isSameMacAs(d) } }

        assertEquals(1, filtered.size)
        assertEquals("New Discovered Mac", filtered[0].name)
        assertEquals("srv-new", filtered[0].serverId)

        // When saved Mac is removed (e.g. forgotten by user), the filtered list includes it again
        val emptySavedList = emptyList<RememberedMac>()
        val unfilteredAgain = allDiscovered.filter { d -> emptySavedList.none { it.isSameMacAs(d) } }
        assertEquals(2, unfilteredAgain.size)
    }

    @Test
    fun testConnectedStatusLabelFormatting() {
        assertEquals("Connected • Pixel 9 Pro", formatConnectedStatus("Pixel 9 Pro"))
        assertEquals("Connected • Alex's Phone", formatConnectedStatus("Alex's Phone"))
        assertEquals("Connected", formatConnectedStatus(null))
        assertEquals("Connected", formatConnectedStatus(""))
        assertEquals("Connected", formatConnectedStatus("   "))
    }
}
