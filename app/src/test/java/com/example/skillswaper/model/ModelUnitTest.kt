package com.example.skillswaper.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ModelUnitTest {
    @Test
    fun user_defaultValues() {
        val user = User(uid = "123", username = "testuser", email = "test@example.com")
        assertEquals("123", user.uid)
        assertEquals("testuser", user.username)
        assertEquals("test@example.com", user.email)
        assertEquals(0, user.followersCount)
        assertEquals(0, user.followingCount)
        assertEquals(0.0, user.earnings, 0.0)
        assertEquals("", user.bio)
        assertNull(user.avatarUrl)
        assertEquals(emptyList<String>(), user.followerList)
        assertEquals(emptyList<String>(), user.followingList)
    }

    @Test
    fun skillPost_defaultValues() {
        val post = SkillPost(id = "p1", userId = "u1", skillName = "Coding")
        assertEquals("p1", post.id)
        assertEquals("u1", post.userId)
        assertEquals("Coding", post.skillName)
        assertEquals(0, post.commentsCount)
        assertEquals(0, post.likesCount)
        assertEquals(emptyList<String>(), post.likedBy)
        assertNull(post.imageUrl)
    }
}
