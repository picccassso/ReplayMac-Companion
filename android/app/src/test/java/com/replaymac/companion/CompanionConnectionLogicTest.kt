package com.replaymac.companion

import com.replaymac.companion.data.model.CompanionClipSummary
import com.replaymac.companion.data.model.CompanionStateSnapshot
import com.replaymac.companion.data.model.CompanionTelemetryTick
import com.replaymac.companion.data.model.ConnectionState
import com.replaymac.companion.ui.components.clipKindTitle
import com.replaymac.companion.ui.components.formatDurationSeconds
import com.replaymac.companion.ui.components.formatFileSize
import org.junit.Assert.assertEquals
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

        val connected = ConnectionState.Connected("Studio Mac", "1.7.3", "192.168.1.5", 7375)
        assertEquals("1.7.3", connected.serverVersion)

        val failed = ConnectionState.Failed("Connection refused", "127.0.0.1", 7375)
        assertEquals("Connection refused", failed.error)
    }
}
