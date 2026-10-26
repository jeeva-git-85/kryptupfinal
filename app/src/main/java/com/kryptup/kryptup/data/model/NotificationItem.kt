package com.kryptup.kryptup.data.model

data class NotificationItem(
    val id: String = "",
    val senderName: String = "",
    val senderUid: String = "",
    val messageText: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false
)
