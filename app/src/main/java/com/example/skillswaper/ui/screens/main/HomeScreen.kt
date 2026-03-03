package com.example.skillswaper.ui.screens.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import android.util.Log
import com.example.skillswaper.data.FirebaseService
import com.example.skillswaper.model.Comment
import com.example.skillswaper.model.SkillPost
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(onInquiryNavigate: (String, String, String) -> Unit) {
    val skills by FirebaseService.getSkillsFeed().collectAsState(initial = emptyList())
    val currentUserStats by FirebaseService.getCurrentUserStats().collectAsState(initial = null)
    val followingList = currentUserStats?.followingList ?: emptyList()
    
    LaunchedEffect(followingList) {
        Log.d("HomeScreen", "Current user following list updated: $followingList")
    }

    var searchQuery by remember { mutableStateOf("") }
    var isSearchVisible by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            if (isSearchVisible) {
                TopAppBar(
                    title = {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search skills or users...") },
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            singleLine = true,
                            trailingIcon = {
                                IconButton(onClick = { 
                                    isSearchVisible = false
                                    searchQuery = "" 
                                }) {
                                    Icon(Icons.Default.Close, contentDescription = "Close Search")
                                }
                            }
                        )
                    },
                    modifier = Modifier.statusBarsPadding()
                )
            } else {
                CenterAlignedTopAppBar(
                    title = { Text("SkillSwaper", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) },
                    actions = {
                        IconButton(onClick = { isSearchVisible = true }) {
                            Icon(Icons.Default.Search, contentDescription = "Search")
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        val filteredSkills = skills.filter { 
            it.skillName.contains(searchQuery, ignoreCase = true) || 
            it.userName.contains(searchQuery, ignoreCase = true) ||
            it.caption.contains(searchQuery, ignoreCase = true)
        }

        if (filteredSkills.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                Text(
                    if (searchQuery.isEmpty()) "No skills posted yet." else "No results found for '$searchQuery'", 
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
            ) {
                items(filteredSkills) { post ->
                    SkillPostItem(
                        post = post,
                        isFollowing = followingList.contains(post.userId),
                        isStatsLoaded = currentUserStats != null,
                        onInquiryClick = { onInquiryNavigate(post.id, post.skillName, post.userId) }
                    )
                    HorizontalDivider(color = Color.DarkGray.copy(alpha = 0.5f))
                }
            }
        }
    }
}

@Composable
fun SkillPostItem(
    post: SkillPost, 
    isFollowing: Boolean, 
    isStatsLoaded: Boolean,
    onInquiryClick: () -> Unit
) {
    val currentUserId = FirebaseService.getCurrentUserId() ?: ""
    val isLiked = post.likedBy?.contains(currentUserId) == true
    val scope = rememberCoroutineScope()
    var showComments by remember { mutableStateOf(false) }
    
    // Optimistic UI state - more stable initialization
    var isFollowingLocal by remember { mutableStateOf(isFollowing) }
    
    // Sync with backend ONLY when data is explicitly loaded and reliable
    LaunchedEffect(isFollowing, isStatsLoaded) {
        if (isStatsLoaded) {
            isFollowingLocal = isFollowing
        }
    }

    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = post.userName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(text = post.skillName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
            }
            // Follow button functionality
            if (post.userId != currentUserId) {
                Button(
                    onClick = { 
                        isFollowingLocal = !isFollowingLocal // Instant UI update
                        scope.launch { 
                            try {
                                FirebaseService.toggleFollow(post.userId) 
                            } catch (e: Exception) {
                                Log.e("HomeScreen", "Failed to toggle follow", e)
                                isFollowingLocal = isFollowing // Revert on failure
                            }
                        }
                    },
                    colors = if (isFollowingLocal) ButtonDefaults.filledTonalButtonColors() else ButtonDefaults.buttonColors(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text(if (isFollowingLocal) "Following" else "Follow", style = MaterialTheme.typography.labelMedium)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        
        Text(text = post.caption, style = MaterialTheme.typography.bodyMedium)
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("Price", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                    Text(post.price, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                }
                Column {
                    Text("Duration", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                    Text(post.duration, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                }
                Button(onClick = onInquiryClick, modifier = Modifier.height(36.dp)) {
                    Text("Inquiry", style = MaterialTheme.typography.labelMedium)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { scope.launch { FirebaseService.toggleLike(post.id) } }) {
                Icon(
                    if (isLiked) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = "Like",
                    tint = if (isLiked) Color.Red else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(text = "${post.likesCount}", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(end = 8.dp))
            
            IconButton(onClick = { showComments = true }) {
                Icon(Icons.Default.Comment, contentDescription = "Comment")
            }
            Text(text = "${post.commentsCount}", style = MaterialTheme.typography.bodySmall)
        }
    }

    if (showComments) {
        CommentDialog(postId = post.id, onDismiss = { showComments = false })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommentDialog(postId: String, onDismiss: () -> Unit) {
    val comments by FirebaseService.getComments(postId).collectAsState(initial = emptyList())
    var commentText by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.7f),
            shape = MaterialTheme.shapes.large
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Comments", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                
                Spacer(modifier = Modifier.height(16.dp))
                
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(comments) { comment ->
                        Column(modifier = Modifier.padding(vertical = 8.dp)) {
                            Text(comment.userName, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                            Text(comment.text, style = MaterialTheme.typography.bodyMedium)
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    }
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = commentText,
                        onValueChange = { commentText = it },
                        placeholder = { Text("Write a comment...") },
                        modifier = Modifier.weight(1f),
                        maxLines = 2
                    )
                    IconButton(onClick = {
                        if (commentText.isNotEmpty()) {
                            scope.launch {
                                FirebaseService.addComment(postId, commentText)
                                commentText = ""
                            }
                        }
                    }) {
                        Icon(Icons.Default.Send, contentDescription = "Send")
                    }
                }
            }
        }
    }
}
