package com.kryptup.kryptup.data.wallet

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.web3j.crypto.Bip32ECKeyPair
import org.web3j.crypto.Credentials
import org.web3j.crypto.MnemonicUtils

class WalletManagerTest {

    @Test
    fun generateMnemonic_returnsValidTwelveWordPhrase() {
        val mnemonic = MnemonicUtils.generateMnemonic(ByteArray(16))
        val words = mnemonic.split(" ")
        assertEquals(12, words.size)
    }

    @Test
    fun deriveAddress_isDeterministic() {
        val mnemonic = "candy maple cake sugar honey pink sugar honey pink sugar honey pizza"
        val seed1 = MnemonicUtils.generateSeed(mnemonic, "")
        val masterKeyPair1 = Bip32ECKeyPair.generateKeyPair(seed1)
        
        val path = intArrayOf(
            44 or Bip32ECKeyPair.HARDENED_BIT,
            60 or Bip32ECKeyPair.HARDENED_BIT,
            0 or Bip32ECKeyPair.HARDENED_BIT,
            0,
            0
        )
        val derivedKeyPair1 = Bip32ECKeyPair.deriveKeyPair(masterKeyPair1, path)
        val credentials1 = Credentials.create(derivedKeyPair1)

        val seed2 = MnemonicUtils.generateSeed(mnemonic, "")
        val masterKeyPair2 = Bip32ECKeyPair.generateKeyPair(seed2)
        val derivedKeyPair2 = Bip32ECKeyPair.deriveKeyPair(masterKeyPair2, path)
        val credentials2 = Credentials.create(derivedKeyPair2)

        assertNotNull(credentials1.address)
        assertTrue(credentials1.address.startsWith("0x"))
        assertEquals(credentials1.address, credentials2.address)
    }
}
