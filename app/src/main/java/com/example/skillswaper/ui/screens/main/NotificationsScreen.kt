package com.example.skillswaper.ui.screens.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

data class NotificationItem(
    val id: String,
    val userName: String,
    val skillName: String,
    val type: String = "Inquiry"
)

val sampleNotifications = listOf(
    NotificationItem("1", "John Doe", "UI/UX Design"),
    NotificationItem("2", "Emily Blunt", "Photography"),
    NotificationItem("3", "David Goggins", "Guitar Lessons")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen() {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(title = { Text("Notifications") })
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.padding(innerPadding).fillMaxSize()
        ) {
            items(sampleNotifications) { notification ->
                ListItem(
                    headlineContent = { 
                        Text("${notification.userName} sent an inquiry for ${notification.skillName}") 
                    },
                    leadingContent = {
                        Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer))
                    },
                    trailingContent = {
                        IconButton(onClick = { /* View Inquiry */ }) {
                            Icon(Icons.Default.Info, contentDescription = "Details")
                        }
                    }
                )
                HorizontalDivider()
            }
        }
    }
}
