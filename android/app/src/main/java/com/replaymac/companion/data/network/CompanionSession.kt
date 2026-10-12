package com.replaymac.companion.data.network

import com.replaymac.companion.data.model.CompanionAuthChallenge
import com.replaymac.companion.data.model.CompanionAuthResponse
import com.replaymac.companion.data.model.CompanionClientMessage
import com.replaymac.companion.data.model.CompanionErrorCodes
import com.replaymac.companion.data.model.CompanionServerEvent
import com.replaymac.companion.data.model.ConnectionState
import com.replaymac.companion.data.repository.PairingSecretStore

/**
 * The protocol side of one socket, kept apart from OkHttp so it can be tested:
 * answers auth challenges, keeps pairing secrets in step with the Mac, and
 * decides what a close or failure means for the connection state.
 */
class CompanionSession(
    private val deviceId: String,
    private val host: String,
    private val port: Int,
    private val secrets: PairingSecretStore
) {
    /** What one message from the Mac leads to: a new state to show, a reply to send, or both. */
    data class Step(val state: ConnectionState? = null, val reply: CompanionClientMessage? = null)

    /** The serverId from the Mac's challenge, for an auth_response that leaves it out. */
    private var challengedServerId: String? = null

    /** Why the Mac turned this socket away. It closes the socket right after, so this is the failure to show. */
    private var lastServerError: String? = null

    fun handle(event: CompanionServerEvent): Step = when (event) {
        is CompanionServerEvent.AuthChallenge -> Step(reply = answer(event.challenge))
        is CompanionServerEvent.Auth -> Step(state = onAuthResponse(event.auth))
        is CompanionServerEvent.Error -> {
            if (event.code != null && event.code != CompanionErrorCodes.NOT_AUTHORIZED) {
                lastServerError = event.message
            }
            Step()
        }
        else -> Step()
    }

    /** The state once the socket has closed. */
    fun closedState(wasConnected: Boolean): ConnectionState {
        val serverError = lastServerError
        return when {
            serverError != null -> ConnectionState.Failed(serverError, host, port, wasConnected)
            // The Mac closing an approved socket (quit, server switched off)
            // is a lost connection, which the repository reconnects from.
            wasConnected -> ConnectionState.Failed("ReplayCap closed the connection.", host, port, wasConnected = true)
            else -> ConnectionState.Disconnected
        }
    }

    /** The state once the socket has failed. The Mac's own explanation wins over the socket error. */
    fun failedState(error: Throwable, wasConnected: Boolean): ConnectionState.Failed =
        ConnectionState.Failed(
            error = lastServerError ?: ConnectionErrors.describe(error, wasConnected),
            host = host,
            port = port,
            wasConnected = wasConnected
        )

    /** Without a stored secret the proof is left out, and the Mac asks the user to approve again. */
    private fun answer(challenge: CompanionAuthChallenge): CompanionClientMessage {
        challengedServerId = challenge.serverId
        val proof = secrets.secret(challenge.serverId)?.let { secret ->
            PairingProof.compute(secret, challenge.serverId, deviceId, challenge.nonce)
        }
        return CompanionClientMessage.authProof(proof)
    }

    private fun onAuthResponse(auth: CompanionAuthResponse): ConnectionState? {
        val serverId = auth.serverId ?: challengedServerId
        return when (auth.status.lowercase()) {
            "approved" -> {
                // Stored before showing Connected: the Mac sends a secret only once.
                if (serverId != null && auth.pairingSecret != null) {
                    secrets.save(serverId, auth.pairingSecret)
                }
                ConnectionState.Connected(
                    serverName = auth.serverName,
                    serverVersion = auth.serverVersion,
                    host = host,
                    port = port,
                    serverId = auth.serverId,
                    deviceName = auth.deviceName
                )
            }
            "pending" -> ConnectionState.PendingApproval(auth.serverName, host, port, auth.pairingCode)
            "denied" -> {
                serverId?.let(secrets::remove)
                ConnectionState.Failed(deniedMessage(auth), host, port, pairingDenied = true)
            }
            else -> null
        }
    }

    private fun deniedMessage(auth: CompanionAuthResponse): String = when (auth.reason) {
        "revoked" -> "“${auth.serverName}” removed this phone. Connect again to pair."
        "invalid_proof" -> "“${auth.serverName}” couldn't verify this phone. Connect again to pair."
        else -> "Pairing denied by ${auth.serverName}"
    }
}
