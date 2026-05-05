package com.example.tradeoff.features.chat

data class SendMessageRequest(
    val senderEmail: String,
    val receiverEmail: String,
    val content: String,
    val channel: String? = null,
    val transactionId: Long? = null
)
