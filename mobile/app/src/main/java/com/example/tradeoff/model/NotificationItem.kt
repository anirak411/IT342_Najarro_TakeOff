package com.example.tradeoff.model

data class NotificationItem(
    val id: String,
    val title: String,
    val body: String,
    val createdAt: String,
    val timeLabel: String,
    val contactEmail: String,
    val contactName: String,
    val channel: String? = null,
    val transactionId: Long? = null
)
