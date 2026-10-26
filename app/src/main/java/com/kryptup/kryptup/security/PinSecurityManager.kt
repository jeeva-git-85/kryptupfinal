package com.kryptup.kryptup.security

import android.content.SharedPreferences
import com.google.firebase.auth.FirebaseAuth
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class PinSecurityManager @Inject constructor(
    @Named("encrypted") private val prefs: SharedPreferences,
    private val auth: FirebaseAuth
) {
    companion object {
        private const val ITERATIONS = 120_000
        private const val KEY_LENGTH = 256
        private const val MAX_ATTEMPTS = 5
        private const val LOCKOUT_DURATION_MS = 5 * 60 * 1000L // 5 minutes
    }

    private fun getPinKey(): String {
        val uid = auth.currentUser?.uid ?: "default_user"
        return "user_tx_pin_hash_$uid"
    }

    private fun getSaltKey(): String {
        val uid = auth.currentUser?.uid ?: "default_user"
        return "user_tx_pin_salt_$uid"
    }

    private fun getBiometricKey(): String {
        val uid = auth.currentUser?.uid ?: "default_user"
        return "user_tx_biometric_enabled_$uid"
    }

    private fun getAttemptsKey(): String {
        val uid = auth.currentUser?.uid ?: "default_user"
        return "user_tx_pin_attempts_$uid"
    }

    private fun getLockoutKey(): String {
        val uid = auth.currentUser?.uid ?: "default_user"
        return "user_tx_pin_locked_until_$uid"
    }

    fun isPinSet(): Boolean {
        val hash = prefs.getString(getPinKey(), null)
        return !hash.isNullOrEmpty()
    }

    fun isLockedOut(): Boolean {
        val lockUntil = prefs.getLong(getLockoutKey(), 0L)
        return System.currentTimeMillis() < lockUntil
    }

    fun getRemainingLockoutSeconds(): Long {
        val lockUntil = prefs.getLong(getLockoutKey(), 0L)
        val remaining = (lockUntil - System.currentTimeMillis()) / 1000L
        return if (remaining > 0) remaining else 0L
    }

    fun setPin(pin: String) {
        val salt = ByteArray(16)
        SecureRandom().nextBytes(salt)
        val saltHex = salt.joinToString("") { "%02x".format(it) }

        val hashedPin = hashPinWithSalt(pin, salt)
        prefs.edit()
            .putString(getPinKey(), hashedPin)
            .putString(getSaltKey(), saltHex)
            .putInt(getAttemptsKey(), 0)
            .putLong(getLockoutKey(), 0L)
            .apply()
    }

    fun verifyPin(enteredPin: String): Boolean {
        if (isLockedOut()) return false

        val storedHash = prefs.getString(getPinKey(), null) ?: return false
        val storedSaltHex = prefs.getString(getSaltKey(), null)

        val isValid = if (!storedSaltHex.isNullOrEmpty()) {
            val salt = storedSaltHex.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
            hashPinWithSalt(enteredPin, salt) == storedHash
        } else {
            // Backward-compatible fallback: verify via legacy SHA-256, then auto-upgrade to PBKDF2
            val legacyMatch = legacySha256(enteredPin) == storedHash
            if (legacyMatch) migrateToSaltedHash(enteredPin)
            legacyMatch
        }

        if (isValid) {
            // Reset attempts counter on successful verification
            prefs.edit()
                .putInt(getAttemptsKey(), 0)
                .putLong(getLockoutKey(), 0L)
                .apply()
            return true
        } else {
            val attempts = prefs.getInt(getAttemptsKey(), 0) + 1
            val editor = prefs.edit().putInt(getAttemptsKey(), attempts)
            if (attempts >= MAX_ATTEMPTS) {
                editor.putLong(getLockoutKey(), System.currentTimeMillis() + LOCKOUT_DURATION_MS)
            }
            editor.apply()
            return false
        }
    }

    fun isBiometricEnabled(): Boolean {
        return prefs.getBoolean(getBiometricKey(), true)
    }

    fun setBiometricEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(getBiometricKey(), enabled).apply()
    }

    fun clearPin() {
        prefs.edit()
            .remove(getPinKey())
            .remove(getSaltKey())
            .remove(getBiometricKey())
            .remove(getAttemptsKey())
            .remove(getLockoutKey())
            .apply()
    }

    private fun hashPinWithSalt(pin: String, salt: ByteArray): String {
        val spec = PBEKeySpec(pin.toCharArray(), salt, ITERATIONS, KEY_LENGTH)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val hash = factory.generateSecret(spec).encoded
        return hash.joinToString("") { "%02x".format(it) }
    }

    /**
     * Legacy unsalted SHA-256 — kept only for one-time migration on next successful login.
     * Call [migrateToSaltedHash] after verifying with this path.
     */
    private fun legacySha256(pin: String): String {
        val bytes = java.security.MessageDigest.getInstance("SHA-256").digest(pin.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * Migrates a legacy SHA-256 PIN hash to PBKDF2. Call this after a successful legacy verification.
     */
    fun migrateToSaltedHash(pin: String) {
        setPin(pin)
    }
}
