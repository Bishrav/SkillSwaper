package com.example.skillswaper.model

data class User(
    val uid: String = "",
    val username: String = "",
    val email: String = "",
    val followers: Int = 0,
    val following: Int = 0,
    val earnings: Double = 0.0,
    val bio: String = "",
    val avatarUrl: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
