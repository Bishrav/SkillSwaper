package com.example.skillswaper.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.skillswaper.model.Skill
import com.example.skillswaper.model.sampleSkills

@Composable
fun SkillListScreen(onSkillClick: (Skill) -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Available Skills") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(sampleSkills) { skill ->
                SkillItem(skill = skill, onClick = { onSkillClick(skill) })
            }
        }
    }
}

@Composable
fun SkillItem(skill: Skill, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        onClick = onClick
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = skill.name, style = MaterialTheme.typography.titleMedium)
            Text(text = skill.category, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = skill.description, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
