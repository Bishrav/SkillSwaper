package com.example.skillswaper.ui.screens.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.skillswaper.data.FirebaseService
import com.example.skillswaper.model.User
import com.google.firebase.auth.FirebaseAuth
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import com.example.skillswaper.model.SkillPost
import com.example.skillswaper.ui.screens.main.PaymentBottomSheet
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    userId: String? = null, 
    onBack: (() -> Unit)? = null,
    onSignOut: (() -> Unit)? = null,
    onInquiryNavigate: (String, String, String) -> Unit
) {
    val currentUserId = FirebaseService.getCurrentUserId() ?: ""
    val targetUserId = userId ?: currentUserId
    val isOwnProfile = targetUserId == currentUserId
    val context = LocalContext.current
    
    val userStats by FirebaseService.getUserStats(targetUserId).collectAsState(initial = null)
    var selectedTab by remember { mutableStateOf(0) }
    var selectedPostForPayment by remember { mutableStateOf<SkillPost?>(null) }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(if (isOwnProfile) "My Profile" else userStats?.username ?: "Profile") },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                        Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    }
                },
                actions = {
                    if (isOwnProfile) {
                        IconButton(onClick = {
                            FirebaseAuth.getInstance().signOut()
                            onSignOut?.invoke()
                        }) {
                            Icon(Icons.Default.ExitToApp, contentDescription = "Sign Out")
                        }
                        IconButton(onClick = { /* Settings */ }) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings")
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            // Profile Header
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(userStats?.username ?: "Loading...", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(userStats?.email ?: "...", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary)
                
                if (!isOwnProfile) {
                    val currentUserStats by FirebaseService.getCurrentUserStats().collectAsState(initial = null)
                    val isFollowing = currentUserStats?.followingList?.contains(targetUserId) == true
                    
                    // Optimistic UI state - persistent and protected
                    var isFollowingLocal by rememberSaveable(targetUserId) { mutableStateOf(isFollowing) }
                    var lastInteractionTime by rememberSaveable(targetUserId) { mutableLongStateOf(0L) }
                    
                    LaunchedEffect(isFollowing, currentUserStats != null) {
                        val currentTime = System.currentTimeMillis()
                        // Ignore backend updates for 10 seconds after a click to prevent premature reverts
                        if (currentUserStats != null && (currentTime - lastInteractionTime > 10000)) {
                            isFollowingLocal = isFollowing
                            android.util.Log.d("ProfileScreen", "Is following state updated from backend: $isFollowing for user: $targetUserId")
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { 
                            isFollowingLocal = !isFollowingLocal // Instant UI update
                            lastInteractionTime = System.currentTimeMillis()
                            scope.launch { 
                                try {
                                    FirebaseService.toggleFollow(targetUserId) 
                                } catch (e: Exception) {
                                    android.util.Log.e("ProfileScreen", "Failed to toggle follow", e)
                                    Toast.makeText(context, "Follow failed: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                                    isFollowingLocal = isFollowing // Revert on failure
                                }
                            }
                        },
                        colors = if (isFollowingLocal) ButtonDefaults.filledTonalButtonColors() else ButtonDefaults.buttonColors()
                    ) {
                        Text(if (isFollowingLocal) "Following" else "Follow")
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Stats Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatItem("Followers", userStats?.followersCount ?: 0)
                    StatItem("Following", userStats?.followingCount ?: 0)
                    StatItem("Skills", userStats?.postedSkillsCount ?: 0)
                    StatItem("Earnings", "$${String.format("%.2f", userStats?.earnings ?: 0.0)}")
                }
            }
            
            // Tabs for Posts
            TabRow(selectedTabIndex = selectedTab) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }) {
                    Text("Posts", modifier = Modifier.padding(16.dp))
                }
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }) {
                    Text("Liked", modifier = Modifier.padding(16.dp))
                }
            }
            
            // Posts List
            val postsFlow = if (selectedTab == 0) {
                FirebaseService.getUserPosts(targetUserId)
            } else {
                FirebaseService.getLikedPosts(targetUserId)
            }
            val posts by postsFlow.collectAsState(initial = emptyList())
            
            val currentUserStats by FirebaseService.getCurrentUserStats().collectAsState(initial = null)
            val currentFollowingList = currentUserStats?.followingList ?: emptyList()

            if (posts.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("No posts found.", color = MaterialTheme.colorScheme.secondary)
                }
            } else {
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(posts, key = { it.id }) { post ->
                        SkillPostItem(
                            post = post, 
                            isFollowing = currentFollowingList.contains(post.userId),
                            isStatsLoaded = currentUserStats != null,
                            onInquiryClick = { onInquiryNavigate(post.id, post.skillName, post.userId) },
                            onPayClick = { 
                                selectedPostForPayment = post
                            }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                    }
                }
            }
        }
        
        selectedPostForPayment?.let { post ->
            PaymentBottomSheet(
                post = post,
                onDismiss = { selectedPostForPayment = null },
                onPurchaseSuccess = { selectedPostForPayment = null }
            )
        }
    }
}

@Composable
fun StatItem(label: String, value: Any) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value.toString(),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.secondary
        )
    }
}
