package com.example.tradeoff.features.auth
data class AuthRequest(
    val fullName: String,
    val displayName: String,
    val email: String,
    val password: String
)
