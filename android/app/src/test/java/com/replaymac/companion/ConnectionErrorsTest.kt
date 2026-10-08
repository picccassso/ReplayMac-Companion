package com.replaymac.companion

import com.replaymac.companion.data.network.ConnectionErrors
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import java.io.EOFException
import java.net.ConnectException
import java.net.SocketTimeoutException

class ConnectionErrorsTest {

    @Test
    fun aDroppedConnectionNeverReadsAsAFailedConnect() {
        assertEquals(
            "The connection to your Mac was interrupted.",
            ConnectionErrors.describe(EOFException(), wasConnected = true)
        )
    }

    @Test
    fun messagelessExceptionsStillGetAReadableReason() {
        // OkHttp reports a socket closed without a WebSocket close frame as a bare EOFException.
        assertNotEquals("Connection failed", ConnectionErrors.describe(EOFException(), wasConnected = false))
        assertEquals(
            "Nothing is listening there. Check Companion Server is on in ReplayMac.",
            ConnectionErrors.describe(ConnectException("Connection refused"), wasConnected = false)
        )
        assertEquals(
            "Your Mac didn't respond. Check it's awake and on the same network.",
            ConnectionErrors.describe(SocketTimeoutException(), wasConnected = false)
        )
    }

    @Test
    fun unknownErrorsKeepTheirOwnMessage() {
        assertEquals("Boom", ConnectionErrors.describe(IllegalStateException("Boom"), wasConnected = false))
        assertEquals("Connection failed", ConnectionErrors.describe(IllegalStateException(), wasConnected = false))
    }
}
