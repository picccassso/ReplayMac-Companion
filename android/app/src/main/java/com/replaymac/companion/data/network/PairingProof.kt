package com.replaymac.companion.data.network

import java.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Proves this phone holds the pairing secret without ever sending it:
 * an HMAC-SHA256 over the Mac's one-time nonce, bound to both identities.
 */
object PairingProof {
    const val CONTEXT = "replaycap-companion-auth-v1"
    private const val ALGORITHM = "HmacSHA256"

    /**
     * [nonce] is the base64 string exactly as the Mac sent it.
     * Returns null when [pairingSecret] isn't valid base64.
     */
    fun compute(pairingSecret: String, serverId: String, deviceId: String, nonce: String): String? {
        val key = try {
            Base64.getDecoder().decode(pairingSecret)
        } catch (_: IllegalArgumentException) {
            return null
        }
        if (key.isEmpty()) return null

        val message = listOf(CONTEXT, serverId, deviceId, nonce).joinToString("\n")
        val mac = Mac.getInstance(ALGORITHM).apply { init(SecretKeySpec(key, ALGORITHM)) }
        return Base64.getEncoder().encodeToString(mac.doFinal(message.toByteArray(Charsets.UTF_8)))
    }
}
