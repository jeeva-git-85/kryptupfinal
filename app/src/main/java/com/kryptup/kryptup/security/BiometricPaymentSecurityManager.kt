package com.kryptup.kryptup.security

import android.content.Context
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyPermanentlyInvalidatedException
import android.security.keystore.KeyProperties
import androidx.annotation.RequiresApi
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

enum class BiometricStatus {
    SUCCESS,
    NO_HARDWARE,
    HARDWARE_UNAVAILABLE,
    NONE_ENROLLED,
    UNSUPPORTED
}

data class EncryptedPayload(
    val ciphertext: ByteArray,
    val iv: ByteArray
)

@Singleton
class BiometricPaymentSecurityManager @Inject constructor(
    private val context: Context
) {
    companion object {
        private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
        private const val KEY_ALIAS = "kryptup_payment_biometric_key"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
    }

    private val keyStore: KeyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply {
        load(null)
    }

    /**
     * Checks if device supports hardware-backed BIOMETRIC_STRONG validation.
     */
    fun canAuthenticateStrong(): BiometricStatus {
        val biometricManager = BiometricManager.from(context)
        return when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)) {
            BiometricManager.BIOMETRIC_SUCCESS -> BiometricStatus.SUCCESS
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> BiometricStatus.NO_HARDWARE
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> BiometricStatus.HARDWARE_UNAVAILABLE
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> BiometricStatus.NONE_ENROLLED
            else -> BiometricStatus.UNSUPPORTED
        }
    }

    /**
     * Generates or retrieves the AES-256 secret key in Android Keystore with strict
     * hardware biometric binding (.setUserAuthenticationRequired(true)) and
     * enrollment invalidation (.setInvalidatedByBiometricEnrollment(true)).
     */
    @RequiresApi(Build.VERSION_CODES.M)
    fun getOrCreateSecretKey(): SecretKey {
        if (!keyStore.containsAlias(KEY_ALIAS)) {
            createSecretKey()
        }
        val entry = keyStore.getEntry(KEY_ALIAS, null) as KeyStore.SecretKeyEntry
        return entry.secretKey
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun createSecretKey() {
        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            KEYSTORE_PROVIDER
        )

        val builder = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            // Strict Biometric Binding: Key can ONLY be unlocked via biometric authentication
            .setUserAuthenticationRequired(true)
            // Enrollment Invalidation: If new face/fingerprint is enrolled in system settings, key is invalidated
            .setInvalidatedByBiometricEnrollment(true)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            builder.setUserAuthenticationParameters(
                0, // 0 = Authentication required for EVERY key usage
                KeyProperties.AUTH_BIOMETRIC_STRONG
            )
        } else {
            @Suppress("DEPRECATION")
            builder.setUserAuthenticationValidityDurationSeconds(-1)
        }

        keyGenerator.init(builder.build())
        keyGenerator.generateKey()
    }

    /**
     * Prepares an unauthenticated Cipher.
     * If user enrolled a new biometric, throws KeyPermanentlyInvalidatedException.
     */
    @Throws(KeyPermanentlyInvalidatedException::class)
    fun getInitializedCipher(): Cipher {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        val secretKey = getOrCreateSecretKey()
        try {
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        } catch (e: KeyPermanentlyInvalidatedException) {
            // Delete old invalidated key so caller can regenerate
            keyStore.deleteEntry(KEY_ALIAS)
            throw e
        }
        return cipher
    }

    /**
     * Helper to wrap initialized Cipher into BiometricPrompt.CryptoObject
     */
    fun createCryptoObject(): BiometricPrompt.CryptoObject? {
        return try {
            val cipher = getInitializedCipher()
            BiometricPrompt.CryptoObject(cipher)
        } catch (e: KeyPermanentlyInvalidatedException) {
            // Re-generate fresh key after invalidation
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                createSecretKey()
                val cipher = getInitializedCipher()
                BiometricPrompt.CryptoObject(cipher)
            } else null
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Encrypts the transaction payload using the unlocked Cipher post biometric authentication success.
     */
    fun encryptTransactionPayload(cipher: Cipher, payload: ByteArray): EncryptedPayload {
        val ciphertext = cipher.doFinal(payload)
        return EncryptedPayload(ciphertext = ciphertext, iv = cipher.iv)
    }

    /**
     * Purges and regenerates Keystore key (e.g. after invalidation error).
     */
    fun resetKeyStoreKey() {
        try {
            if (keyStore.containsAlias(KEY_ALIAS)) {
                keyStore.deleteEntry(KEY_ALIAS)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                createSecretKey()
            }
        } catch (_: Exception) {}
    }
}
