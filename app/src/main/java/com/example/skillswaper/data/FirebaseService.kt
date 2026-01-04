package com.example.skillswaper.data

import com.example.skillswaper.model.SkillPost
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

object FirebaseService {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    
    // Auth
    fun getCurrentUserId(): String? = auth.currentUser?.uid
    fun isUserLoggedIn(): Boolean = auth.currentUser != null

    // Firestore Skill CRUD
    suspend fun postSkill(skill: SkillPost) {
        val userId = auth.currentUser?.uid ?: return
        val userEmail = auth.currentUser?.email ?: "Unknown"
        
        val newSkill = skill.copy(
            userId = userId,
            userName = userEmail.substringBefore("@")
        )
        
        db.collection("skills").add(newSkill).await()
    }

    fun getSkillsFeed(): Flow<List<SkillPost>> = callbackFlow {
        val subscription = db.collection("skills")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                
                val skills = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(SkillPost::class.java)?.copy(id = doc.id)
                } ?: emptyList()
                
                trySend(skills)
            }
        awaitClose { subscription.remove() }
    }

    // User Profile Stats
    suspend fun updateFollowerCount(userId: String, increment: Boolean) {
        // Logic to update follower count in user document
    }
}
