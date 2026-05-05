package com.example.tradeoff.features.chat

data class ChatMessage(
    val id: Long = 0,
    val senderEmail: String = "",
    val receiverEmail: String = "",
    val content: String = "",
    val createdAt: String? = null
)
