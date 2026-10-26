package com.kryptup.kryptup.voice

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class FinancialNlpEngineTest {

    private val engine = FinancialNlpEngine()

    @Test
    fun parseCommand_checkBalance_returnsCheckBalanceIntent() {
        val intent = engine.parseCommand("Check my balance")
        assertTrue(intent is VoiceIntent.CheckBalance)
    }

    @Test
    fun parseCommand_checkPrice_returnsCheckPriceIntent() {
        val intent = engine.parseCommand("What is the price of ETH?")
        assertTrue(intent is VoiceIntent.CheckPrice)
        assertEquals("ETH", (intent as VoiceIntent.CheckPrice).symbol)
    }

    @Test
    fun parseCommand_sendCrypto_returnsSendCryptoIntent() {
        val intent = engine.parseCommand("send 0.5 eth to Bob")
        assertTrue(intent is VoiceIntent.SendCrypto)
        val sendIntent = intent as VoiceIntent.SendCrypto
        assertEquals(BigDecimal("0.5"), sendIntent.amount)
        assertEquals("ETH", sendIntent.symbol)
        assertEquals("Bob", sendIntent.recipient)
    }

    @Test
    fun parseCommand_navigateScreen_returnsNavigateScreenIntent() {
        val intent = engine.parseCommand("open trading screen")
        assertTrue(intent is VoiceIntent.NavigateScreen)
        assertEquals("trading", (intent as VoiceIntent.NavigateScreen).screen)
    }

    @Test
    fun parseCommand_unknownText_returnsUnknownIntent() {
        val intent = engine.parseCommand("hello world")
        assertTrue(intent is VoiceIntent.Unknown)
    }
}
