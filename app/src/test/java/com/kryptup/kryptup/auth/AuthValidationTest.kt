package com.kryptup.kryptup.auth

import org.junit.Assert.*
import org.junit.Test

class AuthValidationTest {

    private fun generateUpiId(name: String, uid: String): String {
        val sanitized = name.lowercase().replace("[^a-z0-9]".toRegex(), "").ifEmpty { "user" }
        val uidSuffix = uid.take(4).lowercase()
        return "$sanitized$uidSuffix@kryptup"
    }

    private fun isValidResetEmail(email: String): Boolean {
        val clean = email.trim()
        return clean.isNotEmpty() && clean.contains("@") && clean.contains(".")
    }

    @Test
    fun generateUpiId_withValidNameAndUid_createsCorrectHandle() {
        val upiId = generateUpiId("Alice Wonderland", "a1b2c3d4e5f6")
        assertEquals("alicewonderlanda1b2@kryptup", upiId)
    }

    @Test
    fun generateUpiId_withSpecialCharacters_sanitizesCorrectly() {
        val upiId = generateUpiId("John Doe #1!", "xyz987654")
        assertEquals("johndoe1xyz9@kryptup", upiId)
    }

    @Test
    fun generateUpiId_withEmptyName_fallsBackToUser() {
        val upiId = generateUpiId("!@#$%", "u123456")
        assertEquals("useru123@kryptup", upiId)
    }

    @Test
    fun isValidResetEmail_validEmail_returnsTrue() {
        assertTrue(isValidResetEmail("test.user@kryptup.io"))
        assertTrue(isValidResetEmail("  alice@gmail.com  "))
    }

    @Test
    fun isValidResetEmail_missingAtSymbol_returnsFalse() {
        assertFalse(isValidResetEmail("invalidemail.com"))
    }

    @Test
    fun isValidResetEmail_missingDomainDot_returnsFalse() {
        assertFalse(isValidResetEmail("invalid@domain"))
    }

    @Test
    fun isValidResetEmail_emptyString_returnsFalse() {
        assertFalse(isValidResetEmail("   "))
    }
}
