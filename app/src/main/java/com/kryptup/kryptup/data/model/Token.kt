package com.kryptup.kryptup.data.model

import java.math.BigDecimal

data class Token(
    val symbol: String,
    val name: String,
    val address: String?, // null for native ETH
    val balance: BigDecimal = BigDecimal.ZERO,
    val decimals: Int = 18
)
