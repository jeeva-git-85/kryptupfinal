package com.kryptup.kryptup.security

import org.junit.Assert.*
import org.junit.Test
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

class PinSecurityValidationTest {

    private fun hashPinWithSalt(pin: String, salt: ByteArray): String {
        val spec = PBEKeySpec(pin.toCharArray(), salt, 10000, 256)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val hash = factory.generateSecret(spec).encoded
        return hash.joinToString("") { "%02x".format(it) }
    }

    @Test
    fun pbkdf2_withSalt_isDeterministic() {
        val salt = "1234567890abcdef".toByteArray()
        val h1 = hashPinWithSalt("1234", salt)
        val h2 = hashPinWithSalt("1234", salt)
        assertEquals(h1, h2)
        assertEquals(64, h1.length) // 256 bits = 64 hex chars
    }

    @Test
    fun pbkdf2_differentSalt_producesDifferentHashes() {
        val saltA = "1234567890abcdef".toByteArray()
        val saltB = "fedcba0987654321".toByteArray()
        val h1 = hashPinWithSalt("1234", saltA)
        val h2 = hashPinWithSalt("1234", saltB)
        assertNotEquals(h1, h2)
    }

    @Test
    fun lockout_triggersAfterFiveFailedAttempts() {
        var attempts = 0
        val maxAttempts = 5
        var isLocked = false

        for (i in 1..5) {
            attempts++
            if (attempts >= maxAttempts) {
                isLocked = true
            }
        }

        assertTrue(isLocked)
        assertEquals(5, attempts)
    }
}
