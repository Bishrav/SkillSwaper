package com.example.skillswaper.model

data class SkillPost(
    val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val userAvatarUrl: String? = null,
    val skillName: String = "",
    val price: String = "",
    val duration: String = "",
    val caption: String = "",
    val imageUrl: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val commentsCount: Int = 0,
    val likesCount: Int = 0,
    val likedBy: List<String> = emptyList()
)

val sampleFeed = listOf(
    SkillPost(
        id = "1",
        userName = "Alex Chen",
        skillName = "UI/UX Design",
        price = "$50",
        duration = "2 hours",
        caption = "Mastering Figma and design systems. Ready to swap for some backend knowledge!",
        commentsCount = 12,
        likesCount = 45
    ),
    SkillPost(
        id = "2",
        userName = "Sarah Miller",
        skillName = "Photography",
        price = "$30",
        duration = "1 hour",
        caption = "Golden hour portraits and editing. Looking to learn some guitar.",
        commentsCount = 5,
        likesCount = 18
    )
)
