package com.example.tradeoff.features.auth

data class AuthResponse(
    val success: Boolean,
    val message: String,
    val data: UserProfile? = null
)
