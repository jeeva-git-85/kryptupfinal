package com.kryptup.kryptup.voice

import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal

class VoiceNlpComprehensiveTest {

    private val engine = FinancialNlpEngine()

    @Test
    fun parseCommand_sendWithPayKeyword_returnsSendCryptoIntent() {
        val intent = engine.parseCommand("pay 2.5 USDT to Charlie")
        assertTrue(intent is VoiceIntent.SendCrypto)
        val sendIntent = intent as VoiceIntent.SendCrypto
        assertEquals(BigDecimal("2.5"), sendIntent.amount)
        assertEquals("USDT", sendIntent.symbol)
        assertEquals("Charlie", sendIntent.recipient)
    }

    @Test
    fun parseCommand_sendWithTransferKeyword_returnsSendCryptoIntent() {
        val intent = engine.parseCommand("transfer 10 DAI to David")
        assertTrue(intent is VoiceIntent.SendCrypto)
        val sendIntent = intent as VoiceIntent.SendCrypto
        assertEquals(BigDecimal("10"), sendIntent.amount)
        assertEquals("DAI", sendIntent.symbol)
        assertEquals("David", sendIntent.recipient)
    }

    @Test
    fun parseCommand_sendWithSubstringsInRecipientNames_avoidsPriceCollision() {
        val intentCosta = engine.parseCommand("Send 5 ETH to Costa")
        assertTrue("Expected SendCrypto for Costa", intentCosta is VoiceIntent.SendCrypto)
        assertEquals("Costa", (intentCosta as VoiceIntent.SendCrypto).recipient)

        val intentSocrates = engine.parseCommand("Send 1 ETH to Socrates")
        assertTrue("Expected SendCrypto for Socrates", intentSocrates is VoiceIntent.SendCrypto)
        assertEquals("Socrates", (intentSocrates as VoiceIntent.SendCrypto).recipient)
    }

    @Test
    fun parseCommand_priceForBtc_returnsBtcPriceIntent() {
        val intent = engine.parseCommand("what is the Bitcoin rate today?")
        assertTrue(intent is VoiceIntent.CheckPrice)
        assertEquals("BTC", (intent as VoiceIntent.CheckPrice).symbol)
    }

    @Test
    fun parseCommand_priceForTether_returnsUsdtPriceIntent() {
        val intent = engine.parseCommand("show me tether price")
        assertTrue(intent is VoiceIntent.CheckPrice)
        assertEquals("USDT", (intent as VoiceIntent.CheckPrice).symbol)
    }

    @Test
    fun parseCommand_checkBalanceNaturalLanguage_returnsCheckBalanceIntent() {
        val intent1 = engine.parseCommand("hey how much do I have in my account?")
        assertTrue(intent1 is VoiceIntent.CheckBalance)

        val intent2 = engine.parseCommand("show my balance")
        assertTrue(intent2 is VoiceIntent.CheckBalance)
    }

    @Test
    fun parseCommand_navigationScreens_returnsProperScreenTargets() {
        val tests = mapOf(
            "go to vault" to "wallet",
            "show my bank linking" to "bank",
            "open the charts screen" to "trading",
            "open qr scanner" to "scanner",
            "show my messages" to "chat",
            "open my reward points" to "rewards"
        )

        for ((phrase, expectedTarget) in tests) {
            val intent = engine.parseCommand(phrase)
            assertTrue("Expected NavigateScreen for '$phrase'", intent is VoiceIntent.NavigateScreen)
            assertEquals(expectedTarget, (intent as VoiceIntent.NavigateScreen).screen)
        }
    }

    @Test
    fun parseCommand_unsupportedCommand_returnsUnknownIntent() {
        val intent = engine.parseCommand("play some jazz music")
        assertTrue(intent is VoiceIntent.Unknown)
        assertEquals("play some jazz music", (intent as VoiceIntent.Unknown).rawText)
    }
}
