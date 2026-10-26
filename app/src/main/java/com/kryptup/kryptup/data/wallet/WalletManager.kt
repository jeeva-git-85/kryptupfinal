package com.kryptup.kryptup.data.wallet

import android.content.Context
import android.content.SharedPreferences
import com.google.firebase.auth.FirebaseAuth
import com.kryptup.kryptup.security.CryptoManager
import dagger.hilt.android.qualifiers.ApplicationContext
import org.web3j.crypto.Bip32ECKeyPair
import org.web3j.crypto.Credentials
import org.web3j.crypto.MnemonicUtils
import java.io.File
import java.security.SecureRandom
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class WalletManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val cryptoManager: CryptoManager,
    @Named("encrypted") val sharedPreferences: SharedPreferences,
    val auth: FirebaseAuth
) {
    private val walletDir = File(context.filesDir, "wallets")

    init {
        if (!walletDir.exists()) {
            walletDir.mkdirs()
        }
    }

    private fun getMnemonicKey(): String {
        val uid = auth.currentUser?.uid ?: "default_user"
        return "encrypted_mnemonic_$uid"
    }

    fun saveMnemonic(mnemonic: String) {
        sharedPreferences.edit().putString(getMnemonicKey(), mnemonic).apply()
    }

    fun getMnemonic(): String? {
        return sharedPreferences.getString(getMnemonicKey(), null)
    }

    fun hasWallet(): Boolean {
        return getMnemonic() != null
    }

    fun clearWallet() {
        sharedPreferences.edit().remove(getMnemonicKey()).apply()
    }

    fun generateMnemonic(): String {
        val initialEntropy = ByteArray(16)
        SecureRandom().nextBytes(initialEntropy)
        return MnemonicUtils.generateMnemonic(initialEntropy)
    }

    fun createWallet(mnemonic: String, pass: String = ""): String {
        val seed = MnemonicUtils.generateSeed(mnemonic, pass)
        val masterKeyPair = Bip32ECKeyPair.generateKeyPair(seed)
        
        // BIP-44 Ethereum derivation path: m/44'/60'/0'/0/0
        val path = intArrayOf(
            44 or Bip32ECKeyPair.HARDENED_BIT,
            60 or Bip32ECKeyPair.HARDENED_BIT,
            0 or Bip32ECKeyPair.HARDENED_BIT,
            0,
            0
        )
        val derivedKeyPair = Bip32ECKeyPair.deriveKeyPair(masterKeyPair, path)
        val credentials = Credentials.create(derivedKeyPair)
        
        return credentials.address
    }

    fun importWallet(mnemonic: String, pass: String = ""): String {
        return createWallet(mnemonic, pass)
    }

    fun getCredentialsFromMnemonic(mnemonic: String, pass: String = ""): Credentials {
        val seed = MnemonicUtils.generateSeed(mnemonic, pass)
        val masterKeyPair = Bip32ECKeyPair.generateKeyPair(seed)
        val path = intArrayOf(
            44 or Bip32ECKeyPair.HARDENED_BIT,
            60 or Bip32ECKeyPair.HARDENED_BIT,
            0 or Bip32ECKeyPair.HARDENED_BIT,
            0,
            0
        )
        val derivedKeyPair = Bip32ECKeyPair.deriveKeyPair(masterKeyPair, path)
        return Credentials.create(derivedKeyPair)
    }
}
