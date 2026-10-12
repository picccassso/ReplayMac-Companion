package com.replaymac.companion

import com.replaymac.companion.data.model.CompanionAuthChallenge
import com.replaymac.companion.data.model.CompanionAuthResponse
import com.replaymac.companion.data.model.CompanionServerEvent
import com.replaymac.companion.data.model.ConnectionState
import com.replaymac.companion.data.network.CompanionSession
import com.replaymac.companion.data.network.PairingProof
import com.replaymac.companion.data.repository.PairingSecretStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.net.ConnectException

class CompanionSessionTest {

    private val deviceId = "phone-1"
    private val serverId = "mac-1"
    private val secret = "AQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQE="

    private lateinit var secrets: PairingSecretStore
    private lateinit var session: CompanionSession

    @Before
    fun setUp() {
        secrets = PairingSecretStore(FakeSharedPreferences())
        session = CompanionSession(deviceId, "192.168.1.5", 7375, secrets)
    }

    private fun challenge(nonce: String = "bm9uY2U=") =
        CompanionServerEvent.AuthChallenge(CompanionAuthChallenge(nonce = nonce, serverId = serverId, serverName = "Studio Mac"))

    private fun auth(status: String, serverId: String? = this.serverId, pairingCode: String? = null, pairingSecret: String? = null, reason: String? = null) =
        CompanionServerEvent.Auth(
            CompanionAuthResponse(
                status = status,
                serverName = "Studio Mac",
                serverId = serverId,
                pairingCode = pairingCode,
                pairingSecret = pairingSecret,
                reason = reason
            )
        )

    // MARK: - Challenge

    @Test
    fun challengeIsAnsweredWithProofFromStoredSecret() {
        secrets.save(serverId, secret)

        val step = session.handle(challenge("bm9uY2U="))

        assertEquals("auth_proof", step.reply?.type)
        assertEquals(PairingProof.compute(secret, serverId, deviceId, "bm9uY2U="), step.reply?.proof)
        assertNull("A challenge doesn't change what the screen shows", step.state)
    }

    @Test
    fun challengeWithoutStoredSecretSendsProofless() {
        secrets.save("another-mac", secret)

        val step = session.handle(challenge())

        assertEquals("auth_proof", step.reply?.type)
        assertNull(step.reply?.proof)
    }

    // MARK: - Approval

    @Test
    fun approvalStoresSecretBeforeShowingConnected() {
        val step = session.handle(auth("approved", pairingSecret = secret))

        assertEquals(secret, secrets.secret(serverId))
        assertTrue(step.state is ConnectionState.Connected)
    }

    @Test
    fun approvalWithoutSecretKeepsTheStoredOne() {
        secrets.save(serverId, secret)

        session.handle(auth("approved"))

        assertEquals(secret, secrets.secret(serverId))
    }

    @Test
    fun approvalWithoutServerIdStoresSecretUnderTheChallengedMac() {
        session.handle(challenge())
        session.handle(auth("approved", serverId = null, pairingSecret = secret))

        assertEquals(secret, secrets.secret(serverId))
    }

    @Test
    fun pendingShowsPairingCodeWhenSent() {
        val withCode = session.handle(auth("pending", pairingCode = "4821")).state as ConnectionState.PendingApproval
        assertEquals("4821", withCode.pairingCode)
        assertEquals("Studio Mac", withCode.serverName)

        val withoutCode = session.handle(auth("pending")).state as ConnectionState.PendingApproval
        assertNull(withoutCode.pairingCode)
    }

    // MARK: - Denial

    @Test
    fun everyDenialForgetsThatMacsSecretOnly() {
        for (reason in listOf("user_denied", "revoked", "invalid_proof", null)) {
            secrets.save(serverId, secret)
            secrets.save("another-mac", secret)

            val state = session.handle(auth("denied", reason = reason)).state as ConnectionState.Failed

            assertTrue("$reason must count as pairing denied", state.pairingDenied)
            assertNull("$reason must delete the secret", secrets.secret(serverId))
            assertEquals(secret, secrets.secret("another-mac"))
        }
    }

    // MARK: - Server errors

    @Test
    fun serverErrorIsShownWhenTheMacClosesTheSocket() {
        session.handle(CompanionServerEvent.Error("Too many phones are connected.", "too_many_connections"))

        val state = session.closedState(wasConnected = false) as ConnectionState.Failed

        assertEquals("Too many phones are connected.", state.error)
        assertFalse("A refusal isn't a denial, so the Mac stays remembered", state.pairingDenied)
    }

    @Test
    fun serverErrorIsShownWhenTheSocketFails() {
        session.handle(CompanionServerEvent.Error("Try again in a minute.", "cooldown"))

        val state = session.failedState(ConnectException("refused"), wasConnected = false)

        assertEquals("Try again in a minute.", state.error)
        assertFalse(state.pairingDenied)
    }

    @Test
    fun errorsThatKeepTheSocketOpenDontReplaceTheCloseReason() {
        session.handle(CompanionServerEvent.Error("Not paired yet.", "not_authorized"))
        session.handle(CompanionServerEvent.Error("Clip not found"))

        assertEquals(ConnectionState.Disconnected, session.closedState(wasConnected = false))
        assertEquals(
            "ReplayMac closed the connection.",
            (session.closedState(wasConnected = true) as ConnectionState.Failed).error
        )
    }
}
