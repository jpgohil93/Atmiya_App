package com.atmiya.innovation.ui.video

import android.content.Intent
import android.net.Uri
import android.widget.Toast
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.atmiya.innovation.data.MentorVideo
import com.atmiya.innovation.repository.FirestoreRepository
import com.atmiya.innovation.ui.theme.AtmiyaPrimary
import com.atmiya.innovation.ui.theme.AtmiyaSecondary
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.Timestamp
import kotlinx.coroutines.launch
import java.util.UUID

@Composable
fun MentorVideoScreen(role: String) {
    val repository = remember { FirestoreRepository() }
    val auth = FirebaseAuth.getInstance()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // State for videos
    var videos by remember { mutableStateOf(listOf<MentorVideo>()) }
    var isLoading by remember { mutableStateOf(true) }

    // Fetch videos
    LaunchedEffect(Unit) {
        try {
            videos = repository.getMentorVideos()
        } catch (e: Exception) {
            Toast.makeText(context, "Error fetching videos", Toast.LENGTH_SHORT).show()
        } finally {
            isLoading = false
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
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = AtmiyaPrimary)
            }
        } else {
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
        }

        if (showUploadDialog) {
            UploadVideoDialog(
                onDismiss = { showUploadDialog = false },
                onUpload = { newVideo ->
                    scope.launch {
                        val user = auth.currentUser
                        if (user != null) {
                            val userProfile = repository.getUser(user.uid)
                            val video = newVideo.copy(
                                id = UUID.randomUUID().toString(),
                                mentorId = user.uid,
                                mentorName = userProfile?.name ?: "Unknown Mentor",
                                createdAt = Timestamp.now()
                            )
                            repository.addMentorVideo(video)
                            // Refresh list
                            videos = repository.getMentorVideos()
                            Toast.makeText(context, "Video Added", Toast.LENGTH_SHORT).show()
                        }
                    }
                    showUploadDialog = false
                }
            )
        }
    }
}

@Composable
fun VideoCard(video: MentorVideo) {
    val context = LocalContext.current
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .clickable {
                if (video.videoUrl.isNotBlank()) {
                    try {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(video.videoUrl))
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        Toast.makeText(context, "Could not open video", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(context, "Invalid Video URL", Toast.LENGTH_SHORT).show()
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
    var duration by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Upload Mentor Video") },
        text = {
            Column {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Video Title") })
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = url, onValueChange = { url = it }, label = { Text("Video URL / Link") })
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = duration, onValueChange = { duration = it }, label = { Text("Duration (e.g. 10:00)") })
            }
        },
        confirmButton = {
            Button(onClick = {
                onUpload(MentorVideo(
                    id = "", // Set by caller
                    mentorId = "", // Set by caller
                    mentorName = "", // Set by caller
                    title = title,
                    description = "",
                    videoUrl = url,
                    thumbnailUrl = "",
                    duration = duration.ifBlank { "00:00" },
                    viewsCount = 0,
                    createdAt = null
                ))
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
