package com.example.tradeoff.features.user

data class UserSummary(
    val id: Long = 0,
    val displayName: String = "",
    val fullName: String = "",
    val email: String = "",
    val profilePicUrl: String? = null,
    val coverPicUrl: String? = null,
    val role: String = "USER"
)
