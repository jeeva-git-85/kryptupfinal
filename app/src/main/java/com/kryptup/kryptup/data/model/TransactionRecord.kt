package com.kryptup.kryptup.data.model

enum class TransactionType {
    UPI_SEND,
    UPI_RECEIVE,
    CRYPTO_SEND,
    CRYPTO_RECEIVE,
    SPOT_BUY,
    SPOT_SELL,
    BANK_DEPOSIT,
    REWARD_CLAIM
}

data class TransactionRecord(
    val id: String = "",
    val userId: String = "",
    val type: TransactionType = TransactionType.UPI_SEND,
    val title: String = "",
    val description: String = "",
    val amountDisplay: String = "",
    val amountValue: Double = 0.0,
    val isDebit: Boolean = true,
    val timestamp: Long = System.currentTimeMillis(),
    val status: String = "CONFIRMED",
    val category: String = "Transfer", // "Transfer", "Trading", "Banking", "Rewards"
    val referenceId: String = ""
)
