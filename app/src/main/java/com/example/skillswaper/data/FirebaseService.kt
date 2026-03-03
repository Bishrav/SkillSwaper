package com.example.skillswaper.data

import android.util.Log
import com.example.skillswaper.model.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

object FirebaseService {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private const val TAG = "FirebaseService"
    
    // Auth
    fun getCurrentUserId(): String? = auth.currentUser?.uid
    fun isUserLoggedIn(): Boolean = auth.currentUser != null
    fun getCurrentUserStats(): Flow<User?> = getUserStats(getCurrentUserId() ?: "")

    // User Data
    suspend fun saveUserProfile(user: User) {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid).set(user).await()
    }

    suspend fun resetPassword(email: String) {
        auth.sendPasswordResetEmail(email).await()
    }

    fun getUserStats(userId: String): Flow<User?> = callbackFlow {
        if (userId.isEmpty()) {
            trySend(null)
            close()
            return@callbackFlow
        }
        val subscription = db.collection("users").document(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error fetching user stats", error)
                    close(error)
                    return@addSnapshotListener
                }
                try {
                    val user = snapshot?.toObject(User::class.java)
                    Log.d(TAG, "User stats update for $userId: $user")
                    Log.d(TAG, "Raw followingList for $userId: ${snapshot?.get("followingList")}")
                    trySend(user)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to parse User object", e)
                    trySend(null)
                }
            }
        awaitClose { subscription.remove() }
    }

    // Firestore Skill CRUD
    suspend fun postSkill(skill: SkillPost) {
        val userId = auth.currentUser?.uid ?: return
        val userEmail = auth.currentUser?.email ?: "Unknown"
        
        val newSkill = skill.copy(
            userId = userId,
            userName = userEmail.substringBefore("@")
        )
        
        try {
            db.runBatch { batch ->
                val skillRef = db.collection("skills").document()
                batch.set(skillRef, newSkill)
                
                val userRef = db.collection("users").document(userId)
                // Use set with merge if we're not sure the document exists, 
                // but incrementing requires the document to exist or it might fail in transaction.
                // In batch, incrementing a non-existent field works, but the doc must exist.
                batch.set(userRef, mapOf("postedSkillsCount" to FieldValue.increment(1)), com.google.firebase.firestore.SetOptions.merge())
            }.await()
        } catch (e: Exception) {
            Log.e(TAG, "Error posting skill", e)
            throw e
        }
    }

    fun getSkillsFeed(): Flow<List<SkillPost>> = callbackFlow {
        val subscription = db.collection("skills")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error fetching skills feed", error)
                    trySend(emptyList()) // Send empty instead of closing with error
                    return@addSnapshotListener
                }
                
                val skills = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        doc.toObject(SkillPost::class.java)?.copy(id = doc.id)
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to parse SkillPost", e)
                        null
                    }
                } ?: emptyList()
                
                trySend(skills)
            }
        awaitClose { subscription.remove() }
    }

    fun getUserPosts(userId: String): Flow<List<SkillPost>> = callbackFlow {
        if (userId.isEmpty()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        val subscription = db.collection("skills")
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error fetching user posts", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val skills = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        doc.toObject(SkillPost::class.java)?.copy(id = doc.id)
                    } catch (e: Exception) {
                        null
                    }
                } ?: emptyList()
                trySend(skills.sortedByDescending { it.timestamp })
            }
        awaitClose { subscription.remove() }
    }

    fun getLikedPosts(userId: String): Flow<List<SkillPost>> = callbackFlow {
        if (userId.isEmpty()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        val subscription = db.collection("skills")
            .whereArrayContains("likedBy", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error fetching liked posts", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val skills = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        doc.toObject(SkillPost::class.java)?.copy(id = doc.id)
                    } catch (e: Exception) {
                        null
                    }
                } ?: emptyList()
                trySend(skills.sortedByDescending { it.timestamp })
            }
        awaitClose { subscription.remove() }
    }

    // Interactions: Likes
    suspend fun toggleLike(postId: String) {
        val userId = auth.currentUser?.uid ?: return
        if (postId.isEmpty()) return
        val docRef = db.collection("skills").document(postId)
        
        db.runTransaction { transaction ->
            val snapshot = transaction.get(docRef)
            val likedByRaw = snapshot.get("likedBy")
            val likedBy = if (likedByRaw is List<*>) {
                likedByRaw.filterIsInstance<String>()
            } else {
                emptyList<String>()
            }
            
            if (likedBy.contains(userId)) {
                transaction.update(docRef, "likedBy", FieldValue.arrayRemove(userId))
                transaction.update(docRef, "likesCount", FieldValue.increment(-1))
            } else {
                transaction.update(docRef, "likedBy", FieldValue.arrayUnion(userId))
                transaction.update(docRef, "likesCount", FieldValue.increment(1))
            }
        }.await()
    }

    // Interactions: Comments
    suspend fun addComment(postId: String, text: String) {
        val userId = auth.currentUser?.uid ?: return
        if (postId.isEmpty()) return
        val userEmail = auth.currentUser?.email ?: "Unknown"
        
        val comment = Comment(
            postId = postId,
            userId = userId,
            userName = userEmail.substringBefore("@"),
            text = text
        )
        
        db.runBatch { batch ->
            batch.set(db.collection("skills").document(postId).collection("comments").document(), comment)
            batch.update(db.collection("skills").document(postId), "commentsCount", FieldValue.increment(1))
        }.await()
    }

    fun getComments(postId: String): Flow<List<Comment>> = callbackFlow {
        if (postId.isEmpty()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        val subscription = db.collection("skills").document(postId).collection("comments")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error fetching comments", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val comments = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        doc.toObject(Comment::class.java)?.copy(id = doc.id)
                    } catch (e: Exception) {
                        null
                    }
                } ?: emptyList()
                trySend(comments)
            }
        awaitClose { subscription.remove() }
    }

    // Inquiries
    suspend fun sendInquiry(inquiry: Inquiry) {
        val fromUserId = auth.currentUser?.uid ?: return
        val userEmail = auth.currentUser?.email ?: "Unknown"
        
        val newInquiry = inquiry.copy(
            fromUserId = fromUserId,
            fromUserName = userEmail.substringBefore("@")
        )
        
        db.collection("inquiries").add(newInquiry).await()
    }

    fun getInquiries(): Flow<List<Inquiry>> = callbackFlow {
        val userId = auth.currentUser?.uid ?: ""
        if (userId.isEmpty()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        val subscription = db.collection("inquiries")
            .whereEqualTo("toUserId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error fetching inquiries", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val inquiries = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        doc.toObject(Inquiry::class.java)?.copy(id = doc.id)
                    } catch (e: Exception) {
                        null
                    }
                } ?: emptyList()
                trySend(inquiries.sortedByDescending { it.timestamp })
            }
        awaitClose { subscription.remove() }
    }

    // Social: Follow
    suspend fun toggleFollow(targetUserId: String) {
        val currentUserId = auth.currentUser?.uid ?: return
        if (targetUserId.isEmpty() || currentUserId == targetUserId) return
        
        val currentUserRef = db.collection("users").document(currentUserId)
        val targetUserRef = db.collection("users").document(targetUserId)
        
        try {
            Log.d(TAG, "Attempting to toggle follow for $targetUserId by $currentUserId")
            db.runTransaction { transaction ->
                // READ SECTION - All gets must happen first
                val currentSnap = transaction.get(currentUserRef)
                val targetSnap = transaction.get(targetUserRef)
                
                // Manual parsing of following list
                val followingRaw = currentSnap.get("followingList")
                val following = if (followingRaw is List<*>) {
                    followingRaw.filterIsInstance<String>()
                } else {
                    emptyList<String>()
                }
                
                // WRITE SECTION - Updates and sets
                
                // 1. Ensure Target Profile exists
                if (!targetSnap.exists()) {
                    Log.d(TAG, "Target user $targetUserId profile does not exist. Initializing basic profile.")
                    val dummyUser = User(
                        uid = targetUserId,
                        username = "User",
                        email = "..."
                    )
                    transaction.set(targetUserRef, dummyUser)
                }

                // 2. Handle Follow/Unfollow logic
                if (!currentSnap.exists()) {
                    Log.d(TAG, "Current user $currentUserId profile does not exist. Initializing basic profile.")
                    val userEmail = auth.currentUser?.email ?: "Unknown"
                    val newUser = User(
                        uid = currentUserId,
                        username = userEmail.substringBefore("@"),
                        email = userEmail,
                        followingList = listOf(targetUserId),
                        followingCount = 1
                    )
                    transaction.set(currentUserRef, newUser)
                    
                    transaction.update(targetUserRef, "followerList", FieldValue.arrayUnion(currentUserId))
                    transaction.update(targetUserRef, "followersCount", FieldValue.increment(1))
                } else {
                    if (following.contains(targetUserId)) {
                        Log.d(TAG, "Unfollowing user $targetUserId")
                        transaction.update(currentUserRef, "followingList", FieldValue.arrayRemove(targetUserId))
                        transaction.update(currentUserRef, "followingCount", FieldValue.increment(-1))
                        transaction.update(targetUserRef, "followerList", FieldValue.arrayRemove(currentUserId))
                        transaction.update(targetUserRef, "followersCount", FieldValue.increment(-1))
                    } else {
                        Log.d(TAG, "Following user $targetUserId")
                        transaction.update(currentUserRef, "followingList", FieldValue.arrayUnion(targetUserId))
                        transaction.update(currentUserRef, "followingCount", FieldValue.increment(1))
                        transaction.update(targetUserRef, "followerList", FieldValue.arrayUnion(currentUserId))
                        transaction.update(targetUserRef, "followersCount", FieldValue.increment(1))
                    }
                }
            }.await()
            Log.d(TAG, "Toggle follow transaction completed successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error toggling follow: ${e.message}", e)
            throw e // Rethrow to let UI handle feedback
        }
    }
}
