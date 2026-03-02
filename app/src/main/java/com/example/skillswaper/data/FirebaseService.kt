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
                    trySend(snapshot?.toObject(User::class.java))
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
        
        db.runBatch { batch ->
            val skillRef = db.collection("skills").document()
            batch.set(skillRef, newSkill)
            
            val userRef = db.collection("users").document(userId)
            batch.update(userRef, "postedSkillsCount", FieldValue.increment(1))
        }.await()
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
            val likedBy = snapshot.get("likedBy") as? List<String> ?: emptyList()
            
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
        
        db.runTransaction { transaction ->
            val currentSnap = transaction.get(currentUserRef)
            val following = currentSnap.get("followingList") as? List<String> ?: emptyList()
            
            if (following.contains(targetUserId)) {
                // Unfollow
                transaction.update(currentUserRef, "followingList", FieldValue.arrayRemove(targetUserId))
                transaction.update(currentUserRef, "followingCount", FieldValue.increment(-1))
                transaction.update(targetUserRef, "followerList", FieldValue.arrayRemove(currentUserId))
                transaction.update(targetUserRef, "followersCount", FieldValue.increment(-1))
            } else {
                // Follow
                transaction.update(currentUserRef, "followingList", FieldValue.arrayUnion(targetUserId))
                transaction.update(currentUserRef, "followingCount", FieldValue.increment(1))
                transaction.update(targetUserRef, "followerList", FieldValue.arrayUnion(currentUserId))
                transaction.update(targetUserRef, "followersCount", FieldValue.increment(1))
            }
        }.await()
    }
}
