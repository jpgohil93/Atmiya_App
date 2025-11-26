package com.atmiya.innovation.ui.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.atmiya.innovation.ui.theme.AtmiyaPrimary
import com.atmiya.innovation.ui.theme.AtmiyaAccent

data class Mentor(
    val id: String,
    val name: String,
    val expertise: List<String>,
    val experience: String,
    val organization: String
)

@Composable
fun MentorsScreen(onViewVideos: (String) -> Unit = {}) {
    // Dummy data for now, or fetch from Firestore
    val mentors = listOf(
        Mentor("1", "Dr. A. Patel", listOf("Tech", "AI"), "15 years", "TechCorp"),
        Mentor("2", "Ms. S. Shah", listOf("Finance", "Marketing"), "10 years", "FinGroup")
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(mentors) { mentor ->
            MentorCard(mentor, onViewVideos)
        }
    }
}

@Composable
fun MentorCard(mentor: Mentor, onViewVideos: (String) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = mentor.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(text = mentor.organization, style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "Expertise: ${mentor.expertise.joinToString(", ")}", style = MaterialTheme.typography.bodySmall)
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { onViewVideos(mentor.id) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = AtmiyaPrimary)
            ) {
                Text("View Videos")
            }
        }
    }
}
