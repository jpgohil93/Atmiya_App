package com.atmiya.innovation.ui.video

import android.content.Intent
import android.net.Uri
import android.widget.VideoView
import android.widget.MediaController
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.atmiya.innovation.data.MentorVideo
import com.atmiya.innovation.repository.FirestoreRepository
import com.atmiya.innovation.ui.components.SoftScaffold
import com.atmiya.innovation.ui.theme.AtmiyaPrimary
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MentorVideoDetailScreen(
    videoId: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var video by remember { mutableStateOf<MentorVideo?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(videoId) {
        try {
            val snapshot = FirebaseFirestore.getInstance()
                .collection("mentorVideos").document(videoId).get().await()
            video = snapshot.toObject(MentorVideo::class.java)
        } catch (e: Exception) {
            // Handle error
        } finally {
            isLoading = false
        }
    }

    SoftScaffold(
        topBar = {
            TopAppBar(
                title = { Text("Video Detail", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = AtmiyaPrimary
                )
            )
        }
    ) { innerPadding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = AtmiyaPrimary)
            }
        } else if (video == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Video not found.")
            }
        } else {
            val v = video!!
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp)
            ) {
                // Video Player
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .background(Color.Black)
                ) {
                    AndroidView(
                        factory = { ctx ->
                            VideoView(ctx).apply {
                                setVideoURI(Uri.parse(v.videoUrl))
                                val mediaController = MediaController(ctx)
                                mediaController.setAnchorView(this)
                                setMediaController(mediaController)
                                start()
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(v.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Text("by ${v.mentorName}", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                Spacer(modifier = Modifier.height(16.dp))
                Text(v.description, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
