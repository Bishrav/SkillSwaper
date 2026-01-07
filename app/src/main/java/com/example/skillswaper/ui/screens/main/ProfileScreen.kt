package com.example.skillswaper.ui.screens.main

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Logout
import com.google.firebase.auth.FirebaseAuth

@Composable
fun ProfileScreen(onSignOut: () -> Unit) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Profile") },
                actions = {
                    IconButton(onClick = {
                        FirebaseAuth.getInstance().signOut()
                        onSignOut()
                    }) {
                        Icon(Icons.Default.Logout, contentDescription = "Sign Out")
                    }
                    IconButton(onClick = { /* Settings */ }) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Profile Header
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text("Your Name", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("@username", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary)
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Stats Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem("Followers", "1.2k")
                StatItem("Following", "450")
                StatItem("Skills", "5")
                StatItem("Earnings", "$800")
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Personal Skills Section
            Text("Your Posted Skills", modifier = Modifier.align(Alignment.Start), style = MaterialTheme.typography.titleMedium)
            // TODO: Add Grid/List of personal skill posts
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

private fun Modifier.background(color: androidx.compose.ui.graphics.Color) = this.then(
    androidx.compose.ui.draw.drawBehind { drawRect(color) }
)
