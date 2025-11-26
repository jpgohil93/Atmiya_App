package com.atmiya.innovation.ui.video

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.atmiya.innovation.ui.theme.AtmiyaPrimary
import com.atmiya.innovation.ui.theme.AtmiyaSecondary

data class MentorVideo(
    val id: String,
    val title: String,
    val mentorName: String,
    val duration: String,
    val videoUrl: String // In real app, this would be a URL
)

@Composable
fun MentorVideoScreen(role: String) {
    val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
    val auth = com.google.firebase.auth.FirebaseAuth.getInstance()

    // State for videos
    var videos by remember { mutableStateOf(listOf<MentorVideo>()) }

    // Listen for real-time updates
    LaunchedEffect(Unit) {
        db.collection("mentor_videos")
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) return@addSnapshotListener
                
                if (snapshot != null) {
                    val newVideos = snapshot.documents.map { doc ->
                        MentorVideo(
                            id = doc.id,
                            title = doc.getString("title") ?: "",
                            mentorName = doc.getString("mentorName") ?: "Unknown Mentor",
                            duration = doc.getString("duration") ?: "00:00",
                            videoUrl = doc.getString("videoUrl") ?: ""
                        )
                    }
                    videos = newVideos
                }
            }
    }

    var showUploadDialog by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            if (role == "mentor") {
                FloatingActionButton(
                    onClick = { showUploadDialog = true },
                    containerColor = AtmiyaSecondary,
                    contentColor = Color.White
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Upload Video")
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = "Mentor Video Library",
                    style = MaterialTheme.typography.headlineMedium,
                    color = AtmiyaPrimary,
                    fontWeight = FontWeight.Bold
                )
            }
            if (videos.isEmpty()) {
                item {
                    Text("No videos available yet.", color = Color.Gray)
                }
            } else {
                items(videos) { video ->
                    VideoCard(video)
                }
            }
        }

        if (showUploadDialog) {
            UploadVideoDialog(
                onDismiss = { showUploadDialog = false },
                onUpload = { newVideo ->
                    val user = auth.currentUser
                    if (user != null) {
                        val videoData = hashMapOf(
                            "mentorId" to user.uid,
                            "mentorName" to "My Name", // TODO: Fetch from profile
                            "title" to newVideo.title,
                            "videoUrl" to newVideo.videoUrl,
                            "duration" to "10:00", // Placeholder duration
                            "timestamp" to com.google.firebase.Timestamp.now()
                        )
                        db.collection("mentor_videos").add(videoData)
                    }
                    showUploadDialog = false
                }
            )
        }
    }
}

@Composable
fun VideoCard(video: MentorVideo) {
    val context = androidx.compose.ui.platform.LocalContext.current
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .clickable {
                if (video.videoUrl.isNotBlank()) {
                    try {
                        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(video.videoUrl))
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        android.widget.Toast.makeText(context, "Could not open video", android.widget.Toast.LENGTH_SHORT).show()
                    }
                } else {
                    android.widget.Toast.makeText(context, "Invalid Video URL", android.widget.Toast.LENGTH_SHORT).show()
                }
            },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Black)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            // Thumbnail Placeholder (Gray Background)
            // TODO: Use Coil to load thumbnail if available
            Box(modifier = Modifier
                .fillMaxSize()
                .background(Color.DarkGray))
            
            // Play Button Overlay
            Icon(
                Icons.Filled.PlayArrow,
                contentDescription = "Play",
                tint = Color.White,
                modifier = Modifier.size(64.dp)
            )
            
            // Info Overlay at Bottom
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.6f))
                    .padding(12.dp)
            ) {
                Text(text = video.title, color = Color.White, fontWeight = FontWeight.Bold)
                Text(text = "${video.mentorName} • ${video.duration}", color = Color.LightGray, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
fun UploadVideoDialog(onDismiss: () -> Unit, onUpload: (MentorVideo) -> Unit) {
    var title by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Upload Mentor Video") },
        text = {
            Column {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Video Title") })
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = url, onValueChange = { url = it }, label = { Text("Video URL / Link") })
            }
        },
        confirmButton = {
            Button(onClick = {
                onUpload(MentorVideo("new", title, "Me", "00:00", url))
            }) {
                Text("Upload")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
