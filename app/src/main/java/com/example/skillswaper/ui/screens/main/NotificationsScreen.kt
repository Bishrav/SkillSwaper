package com.example.skillswaper.ui.screens.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.skillswaper.data.FirebaseService
import com.example.skillswaper.model.Inquiry

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(onViewProfile: (String) -> Unit) {
    val inquiries by FirebaseService.getInquiries().collectAsState(initial = emptyList())

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(title = { Text("Alerts") })
        }
    ) { innerPadding ->
        if (inquiries.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No inquiries yet.", color = MaterialTheme.colorScheme.secondary)
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(innerPadding).fillMaxSize()
            ) {
                items(inquiries) { inquiry ->
                    ListItem(
                        headlineContent = { 
                            Text("${inquiry.fromUserName} sent an inquiry for ${inquiry.skillName}", fontWeight = FontWeight.Bold) 
                        },
                        supportingContent = {
                            Column {
                                Text("Contact: ${inquiry.contactDetails}")
                                if (inquiry.message.isNotEmpty()) {
                                    Text("Message: ${inquiry.message}", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        },
                        leadingContent = {
                            Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer))
                        },
                        trailingContent = {
                            IconButton(onClick = { onViewProfile(inquiry.fromUserId) }) {
                                Icon(Icons.Default.Person, contentDescription = "View Profile")
                            }
                        }
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}
