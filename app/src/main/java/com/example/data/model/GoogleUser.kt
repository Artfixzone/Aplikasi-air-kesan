package com.example.data.model

data class GoogleUser(
    val displayName: String,
    val email: String,
    val photoUrl: String? = null,
    val lastSyncTimestamp: Long = System.currentTimeMillis()
)
