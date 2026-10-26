package com.kryptup.kryptup.data.model

import java.math.BigDecimal

data class WalletTransaction(
    val hash: String,
    val from: String,
    val to: String,
    val value: BigDecimal,
    val symbol: String,
    val timestamp: Long,
    val isIncoming: Boolean,
    val status: TransactionStatus = TransactionStatus.CONFIRMED
)

enum class TransactionStatus {
    PENDING, CONFIRMED, FAILED
}
