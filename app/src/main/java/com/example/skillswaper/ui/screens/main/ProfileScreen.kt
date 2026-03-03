package com.example.skillswaper.ui.screens.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.skillswaper.data.FirebaseService
import com.example.skillswaper.model.User
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    userId: String? = null, 
    onSignOut: (() -> Unit)? = null,
    onInquiryNavigate: (String, String, String) -> Unit
) {
    val currentUserId = FirebaseService.getCurrentUserId() ?: ""
    val targetUserId = userId ?: currentUserId
    val isOwnProfile = targetUserId == currentUserId
    
    val userStats by FirebaseService.getUserStats(targetUserId).collectAsState(initial = null)
    var selectedTab by remember { mutableStateOf(0) }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(if (isOwnProfile) "My Profile" else userStats?.username ?: "Profile") },
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
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { 
                            scope.launch { 
                                try {
                                    FirebaseService.toggleFollow(targetUserId) 
                                } catch (e: Exception) {
                                    android.util.Log.e("ProfileScreen", "Failed to toggle follow", e)
                                }
                            }
                        },
                        colors = if (isFollowing) ButtonDefaults.filledTonalButtonColors() else ButtonDefaults.buttonColors()
                    ) {
                        Text(if (isFollowing) "Following" else "Follow")
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Stats Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatItem("Followers", "${userStats?.followersCount ?: 0}")
                    StatItem("Following", "${userStats?.followingCount ?: 0}")
                    StatItem("Skills", "${userStats?.postedSkillsCount ?: 0}")
                    StatItem("Earnings", "$${userStats?.earnings ?: 0.0}")
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
                    items(posts) { post ->
                        SkillPostItem(
                            post = post, 
                            isFollowing = currentFollowingList.contains(post.userId),
                            onInquiryClick = { onInquiryNavigate(post.id, post.skillName, post.userId) }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                    }
                }
            }
        }
    }
}

@Composable
fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
    }
}


