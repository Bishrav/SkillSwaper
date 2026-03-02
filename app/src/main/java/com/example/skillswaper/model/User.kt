package com.example.skillswaper.model

data class User(
    val uid: String = "",
    val username: String = "",
    val email: String = "",
    val followersCount: Int = 0,
    val followingCount: Int = 0,
    val earnings: Double = 0.0,
    val postedSkillsCount: Int = 0,
    val bio: String = "",
    val avatarUrl: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val followerList: List<String>? = emptyList(),
    val followingList: List<String>? = emptyList()
)
