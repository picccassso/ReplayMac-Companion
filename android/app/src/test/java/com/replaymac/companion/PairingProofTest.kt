package com.replaymac.companion

import com.replaymac.companion.data.network.PairingProof
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PairingProofTest {

    /** 32 bytes of 0x01, the secret shared by the macOS, iOS and Android test vectors. */
    private val secret = "AQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQE="

    @Test
    fun matchesTheSharedTestVector() {
        // printf 'replaycap-companion-auth-v1\nS\nD\nN' | openssl dgst -sha256 -mac HMAC \
        //   -macopt hexkey:0101010101010101010101010101010101010101010101010101010101010101 -binary | base64
        assertEquals(
            "uWE2MkK60y47fmnf7H64CPSeu+v47Xrw4QeYLwmpKcs=",
            PairingProof.compute(secret, serverId = "S", deviceId = "D", nonce = "N")
        )
    }

    @Test
    fun proofIsBoundToTheMacThePhoneAndTheNonce() {
        val proof = PairingProof.compute(secret, "S", "D", "N")
        assertNotEquals(proof, PairingProof.compute(secret, "S2", "D", "N"))
        assertNotEquals(proof, PairingProof.compute(secret, "S", "D2", "N"))
        assertNotEquals(proof, PairingProof.compute(secret, "S", "D", "N2"))
    }

    @Test
    fun unreadableSecretGivesNoProof() {
        assertNull(PairingProof.compute("not base64!", "S", "D", "N"))
        assertNull(PairingProof.compute("", "S", "D", "N"))
    }
}
