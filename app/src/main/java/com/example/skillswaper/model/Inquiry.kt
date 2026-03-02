package com.example.skillswaper.model

data class Inquiry(
    val id: String = "",
    val fromUserId: String = "",
    val fromUserName: String = "",
    val toUserId: String = "",
    val skillId: String = "",
    val skillName: String = "",
    val name: String = "",
    val contactDetails: String = "",
    val message: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
