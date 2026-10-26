package com.kryptup.kryptup.data.repository

import com.kryptup.kryptup.data.model.TransactionRecord
import com.kryptup.kryptup.data.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Test

class TransactionCalculationTest {

    private fun calculateTotalSpent(records: List<TransactionRecord>): Double {
        return records.filter { it.isDebit }.sumOf { it.amountValue }
    }

    @Test
    fun calculateTotalSpent_sumsOnlyDebitTransactions() {
        val transactions = listOf(
            TransactionRecord(
                id = "tx1",
                type = TransactionType.UPI_SEND,
                amountValue = 500.0,
                isDebit = true
            ),
            TransactionRecord(
                id = "tx2",
                type = TransactionType.BANK_DEPOSIT,
                amountValue = 10000.0,
                isDebit = false
            ),
            TransactionRecord(
                id = "tx3",
                type = TransactionType.SPOT_BUY,
                amountValue = 1500.0,
                isDebit = true
            ),
            TransactionRecord(
                id = "tx4",
                type = TransactionType.UPI_RECEIVE,
                amountValue = 200.0,
                isDebit = false
            )
        )

        val totalSpent = calculateTotalSpent(transactions)
        assertEquals(2000.0, totalSpent, 0.001)
    }

    @Test
    fun calculateTotalSpent_withEmptyList_returnsZero() {
        val totalSpent = calculateTotalSpent(emptyList())
        assertEquals(0.0, totalSpent, 0.001)
    }

    @Test
    fun calculateTotalSpent_withAllCredits_returnsZero() {
        val creditTransactions = listOf(
            TransactionRecord(
                id = "tx1",
                type = TransactionType.BANK_DEPOSIT,
                amountValue = 5000.0,
                isDebit = false
            ),
            TransactionRecord(
                id = "tx2",
                type = TransactionType.REWARD_CLAIM,
                amountValue = 100.0,
                isDebit = false
            )
        )
        val totalSpent = calculateTotalSpent(creditTransactions)
        assertEquals(0.0, totalSpent, 0.001)
    }
}
