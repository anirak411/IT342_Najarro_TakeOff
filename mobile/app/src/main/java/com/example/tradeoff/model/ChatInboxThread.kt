package com.example.tradeoff.model

data class ChatInboxThread(
    val email: String,
    val displayName: String,
    val preview: String,
    val timeLabel: String,
    val lastMessageAt: String? = null,
    val channel: String? = null,
    val transactionId: Long? = null
)
