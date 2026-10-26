package com.kryptup.kryptup.data.model

data class ChatMessage(
    val id: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val text: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val isReceipt: Boolean = false,
    val receiptAmount: String? = null
)
