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
    // ...
            Button(
                onClick = { onViewVideos(mentor.id) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = AtmiyaPrimary)
            ) {
                Text("View Videos")
            }
        }
    }
