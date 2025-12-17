package com.atmiya.innovation.ui.video

import android.net.Uri
import android.widget.VideoView
import android.widget.MediaController
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebChromeClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.atmiya.innovation.data.MentorVideo
import com.atmiya.innovation.ui.components.SoftScaffold
import com.atmiya.innovation.ui.theme.AtmiyaPrimary
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import compose.icons.TablerIcons
import compose.icons.tablericons.X

/**
 * Extract YouTube video ID from various YouTube URL formats
 */
fun extractYouTubeVideoId(url: String): String? {
    val patterns = listOf(
        "(?:youtube\\.com/watch\\?v=|youtu\\.be/|youtube\\.com/embed/)([a-zA-Z0-9_-]{11})".toRegex(),
        "youtube\\.com/shorts/([a-zA-Z0-9_-]{11})".toRegex()
    )
    for (pattern in patterns) {
        val match = pattern.find(url)
        if (match != null) {
            return match.groupValues[1]
        }
    }
    return null
}

/**
 * Check if URL is a YouTube video
 */
fun isYouTubeUrl(url: String): Boolean {
    return url.contains("youtube.com") || url.contains("youtu.be")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MentorVideoDetailScreen(
    videoId: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var video by remember { mutableStateOf<MentorVideo?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var videoLoading by remember { mutableStateOf(true) }

    LaunchedEffect(videoId) {
        try {
            val snapshot = FirebaseFirestore.getInstance()
                .collection("mentorVideos").document(videoId).get().await()
            video = snapshot.toObject(MentorVideo::class.java)
        } catch (e: Exception) {
            android.util.Log.e("MentorVideoDetail", "Error fetching video: ${e.message}")
        } finally {
            isLoading = false
        }
    }

    // Show loading while fetching video data
    if (isLoading) {
        Box(
            modifier = Modifier.fillMaxSize().background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(color = Color.White)
                Spacer(modifier = Modifier.height(12.dp))
                Text("Loading video...", color = Color.White)
            }
        }
        return
    }

    // Show error if video not found
    if (video == null) {
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
            Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "This video is no longer available.",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "It may have been deleted by the mentor.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                    Spacer(Modifier.height(24.dp))
                    Button(onClick = onBack) {
                        Text("Go Back")
                    }
                }
            }
        }
        return
    }

    // Fullscreen video player - Dialog that covers entire screen
    val v = video!!
    val isYouTube = isYouTubeUrl(v.videoUrl)
    val youTubeVideoId = if (isYouTube) extractYouTubeVideoId(v.videoUrl) else null

    Dialog(
        onDismissRequest = { onBack() },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            if (isYouTube && youTubeVideoId != null) {
                // YouTube Video - Load original URL in WebView
                AndroidView(
                    factory = { ctx ->
                        WebView(ctx).apply {
                            webViewClient = object : WebViewClient() {
                                override fun onPageFinished(view: WebView?, url: String?) {
                                    videoLoading = false
                                }
                            }
                            webChromeClient = WebChromeClient()
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            settings.mediaPlaybackRequiresUserGesture = false
                            settings.loadWithOverviewMode = true
                            settings.useWideViewPort = true
                            loadUrl(v.videoUrl)
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                // Regular Video - Use VideoView
                AndroidView(
                    factory = { ctx ->
                        VideoView(ctx).apply {
                            setVideoURI(Uri.parse(v.videoUrl))
                            val mediaController = MediaController(ctx)
                            mediaController.setAnchorView(this)
                            setMediaController(mediaController)
                            setOnPreparedListener {
                                videoLoading = false
                                start()
                            }
                            setOnErrorListener { _, _, _ ->
                                videoLoading = false
                                false
                            }
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Loading indicator - RENDERED LAST so it shows ON TOP
            if (videoLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = Color.White, strokeWidth = 4.dp)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Loading video...", color = Color.White, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }

            // Close button
            IconButton(
                onClick = { onBack() },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .size(48.dp)
                    .background(
                        Color.Black.copy(alpha = 0.7f),
                        shape = androidx.compose.foundation.shape.CircleShape
                    )
            ) {
                Icon(
                    TablerIcons.X,
                    contentDescription = "Close",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            // Video title overlay at bottom
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.6f))
                    .padding(16.dp)
            ) {
                Column {
                    Text(
                        v.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "by ${v.mentorName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}
