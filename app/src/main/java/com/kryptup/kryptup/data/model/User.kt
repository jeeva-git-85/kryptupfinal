package com.kryptup.kryptup.data.model

data class User(
    val uid: String = "",
    val email: String = "",
    val displayName: String = "",
    val upiId: String = "",
    val fiatBalanceInr: Double = 0.00,
    val profileImageUrl: String = "",
    val points: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)
