package com.kryptup.kryptup.trading

import com.kryptup.kryptup.data.model.TransactionType
import org.junit.Assert.*
import org.junit.Test
import java.util.Locale

class TradingOrderCalculationTest {

    data class OrderResult(
        val type: TransactionType,
        val totalUsd: Double,
        val isDebit: Boolean,
        val amountDisplay: String
    )

    private fun calculateOrder(type: String, symbol: String, amountStr: String, price: Double): OrderResult? {
        val amount = amountStr.toDoubleOrNull() ?: return null
        if (!amount.isFinite() || amount <= 0.0 || !price.isFinite() || price <= 0.0) return null
        val totalUsd = amount * price
        val isBuy = type.equals("Buy", ignoreCase = true)
        val transactionType = if (isBuy) TransactionType.SPOT_BUY else TransactionType.SPOT_SELL
        val display = if (isBuy) {
            String.format(Locale.US, "-$%.2f USD", totalUsd)
        } else {
            String.format(Locale.US, "+$%.2f USD", totalUsd)
        }

        return OrderResult(
            type = transactionType,
            totalUsd = totalUsd,
            isDebit = isBuy,
            amountDisplay = display
        )
    }

    @Test
    fun calculateOrder_buyEth_computesDebitAndSpotBuy() {
        val result = calculateOrder(
            type = "Buy",
            symbol = "ETH",
            amountStr = "1.5",
            price = 3000.0
        )

        assertNotNull(result)
        assertEquals(TransactionType.SPOT_BUY, result!!.type)
        assertEquals(4500.0, result.totalUsd, 0.001)
        assertTrue(result.isDebit)
        assertEquals("-$4500.00 USD", result.amountDisplay)
    }

    @Test
    fun calculateOrder_sellBtc_computesCreditAndSpotSell() {
        val result = calculateOrder(
            type = "Sell",
            symbol = "BTC",
            amountStr = "0.1",
            price = 60000.0
        )

        assertNotNull(result)
        assertEquals(TransactionType.SPOT_SELL, result!!.type)
        assertEquals(6000.0, result.totalUsd, 0.001)
        assertFalse(result.isDebit)
        assertEquals("+$6000.00 USD", result.amountDisplay)
    }

    @Test
    fun calculateOrder_invalidAmount_returnsNull() {
        val result = calculateOrder(
            type = "Buy",
            symbol = "SOL",
            amountStr = "abc",
            price = 150.0
        )

        assertNull(result)
    }

    @Test
    fun calculateOrder_negativeAmount_returnsNull() {
        val result = calculateOrder(
            type = "Buy",
            symbol = "SOL",
            amountStr = "-2.5",
            price = 150.0
        )

        assertNull(result)
    }
}
