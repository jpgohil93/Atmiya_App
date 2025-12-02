package com.atmiya.innovation.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.atmiya.innovation.data.Mentor
import com.atmiya.innovation.repository.FirestoreRepository
import com.atmiya.innovation.ui.theme.AtmiyaPrimary
import com.atmiya.innovation.ui.theme.AtmiyaSecondary
import com.atmiya.innovation.ui.components.SoftCard

@Composable
fun MentorsScreen(onViewProfile: (String) -> Unit) {
    val repository = remember { FirestoreRepository() }
    var mentors by remember { mutableStateOf<List<Mentor>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        try {
            mentors = repository.getAllMentors()
        } catch (e: Exception) {
            // Handle error
        } finally {
            isLoading = false
        }
    }

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = AtmiyaPrimary)
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(mentors) { mentor ->
                MentorCard(mentor, onViewProfile)
            }
        }
    }
}

@Composable
fun MentorCard(mentor: Mentor, onViewProfile: (String) -> Unit) {
    SoftCard(
        modifier = Modifier.fillMaxWidth(),
        elevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .clip(CircleShape)
                        .background(AtmiyaSecondary.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = mentor.name.take(1),
                        fontWeight = FontWeight.Bold,
                        color = AtmiyaPrimary,
                        style = MaterialTheme.typography.titleLarge
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(text = mentor.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(text = mentor.title, style = MaterialTheme.typography.bodyMedium, color = AtmiyaPrimary)
                    Text(text = mentor.organization, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = "Expertise: ${mentor.expertiseAreas.joinToString(", ")}", style = MaterialTheme.typography.bodySmall)
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { onViewProfile(mentor.uid) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = AtmiyaPrimary)
            ) {
                Text("View Profile")
            }
        }
    }
}

