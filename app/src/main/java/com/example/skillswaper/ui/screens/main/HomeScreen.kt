package com.example.skillswaper.ui.screens.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.skillswaper.data.FirebaseService
import com.example.skillswaper.model.SkillPost

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen() {
    val skills by FirebaseService.getSkillsFeed().collectAsState(initial = emptyList())

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("SkillSwaper", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { /* Search */ }) {
                        Icon(Icons.Default.Search, contentDescription = "Search")
                    }
                }
            )
        }
    ) { innerPadding ->
        if (skills.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No skills posted yet. Be the first!", color = MaterialTheme.colorScheme.secondary)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
            ) {
                items(skills) { post ->
                    SkillPostItem(post = post)
                    HorizontalDivider(color = Color.DarkGray.copy(alpha = 0.5f))
                }
            }
        }
    }
}

@Composable
fun SkillPostItem(post: SkillPost) {
    var isFollowing by remember { mutableStateOf(post.isFollowing) }

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
            Button(
                onClick = { isFollowing = !isFollowing },
                colors = if (isFollowing) ButtonDefaults.filledTonalButtonColors() else ButtonDefaults.buttonColors(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                modifier = Modifier.height(32.dp)
            ) {
                Text(if (isFollowing) "Following" else "Follow", style = MaterialTheme.typography.labelMedium)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        
        Text(text = post.caption, style = MaterialTheme.typography.bodyMedium)
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Post Details Card
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
                Button(onClick = { /* Inquiry */ }, modifier = Modifier.height(36.dp)) {
                    Text("Inquiry", style = MaterialTheme.typography.labelMedium)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { /* Like */ }) {
                Icon(Icons.Outlined.FavoriteBorder, contentDescription = "Like")
            }
            IconButton(onClick = { /* Comment */ }) {
                Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Comment") // Temporary replacement for ChatBubble
            }
            Text(text = "${post.commentsCount} comments", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
        }
    }
}
