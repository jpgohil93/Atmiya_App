package com.atmiya.innovation.ui.dashboard.startup

import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.platform.LocalLifecycleOwner
import coil.compose.AsyncImage
import com.atmiya.innovation.data.FeaturedVideo
import com.atmiya.innovation.ui.theme.AtmiyaAccent
import com.atmiya.innovation.ui.theme.AtmiyaPrimary
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.HorizontalPagerIndicator
import com.google.accompanist.pager.rememberPagerState

@OptIn(ExperimentalPagerApi::class)
@Composable
fun HeroVideoSlider(
    videosState: StartupDashboardViewModel.UiState<List<FeaturedVideo>>,
    isVisible: Boolean = true,
    onVideoClick: (String) -> Unit // Kept for compatibility but unused for inline
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp)
    ) {
        when (videosState) {
            is StartupDashboardViewModel.UiState.Loading -> {
                VideoSliderShimmer()
            }
            is StartupDashboardViewModel.UiState.Success -> {
                val videos = videosState.data
                if (videos.isEmpty()) {
                    EmptyVideoState()
                } else {
                    val pagerState = rememberPagerState()
                    
                    // We need to track which video is currently playing to stop others
                    var playingVideoId by remember { mutableStateOf<String?>(null) }

                    // Auto-pause video when user scrolls to a different page
                    LaunchedEffect(pagerState.currentPage) {
                        // If we scroll away from the currently playing video, pause it
                        if (playingVideoId != null) {
                            val currentVideo = videos.getOrNull(pagerState.currentPage)
                            if (currentVideo?.id != playingVideoId) {
                                playingVideoId = null // This will trigger the video to stop
                            }
                        }
                    }

                    // Auto-pause when the entire slider becomes invisible (tab switch or scroll away)
                    LaunchedEffect(isVisible) {
                        if (!isVisible) {
                            playingVideoId = null
                        }
                    }

                    HorizontalPager(
                        count = videos.size,
                        state = pagerState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp), // Increased height slightly for player
                        contentPadding = PaddingValues(horizontal = 20.dp)
                    ) { page ->
                        val video = videos[page]
                        val isPlaying = playingVideoId == video.id
                        
                        VideoCard(
                            video = video,
                            isPlaying = isPlaying,
                            onPlayClick = { 
                                playingVideoId = video.id 
                            },
                            onPause = {
                                if (playingVideoId == video.id) {
                                    playingVideoId = null
                                }
                            }
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Page Indicators
                    // Page Indicators (Numbered)
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .background(
                                color = Color.Black.copy(alpha = 0.6f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "${pagerState.currentPage + 1} / ${videos.size}",
                            color = Color.White,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            is StartupDashboardViewModel.UiState.Error -> {
                ErrorVideoState(message = videosState.message)
            }
        }
    }
}

@Composable
fun VideoCard(
    video: FeaturedVideo,
    isPlaying: Boolean,
    onPlayClick: () -> Unit,
    onPause: () -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (isPlaying) {
                // Native Video Player (ExoPlayer) with gs:// URL support
                val context = LocalContext.current
                val lifecycleOwner = LocalLifecycleOwner.current
                var isVideoLoading by remember { mutableStateOf(true) }
                var videoError by remember { mutableStateOf<String?>(null) }
                var downloadUrl by remember { mutableStateOf<String?>(null) }
                
                // Convert gs:// to https:// download URL
                LaunchedEffect(video.videoUrl) {
                    try {
                        if (video.videoUrl.startsWith("gs://")) {
                            // Extract the storage reference from gs:// URL
                            val storage = com.google.firebase.storage.FirebaseStorage.getInstance()
                            val storageRef = storage.getReferenceFromUrl(video.videoUrl)
                            
                            // Get download URL
                            storageRef.downloadUrl.addOnSuccessListener { uri ->
                                downloadUrl = uri.toString()
                                android.util.Log.d("HeroVideoSlider", "Converted gs:// to: $downloadUrl")
                            }.addOnFailureListener { e ->
                                android.util.Log.e("HeroVideoSlider", "Failed to get download URL", e)
                                videoError = "Failed to load video: ${e.message}"
                                isVideoLoading = false
                            }
                        } else {
                            // Already an https:// URL
                            downloadUrl = video.videoUrl
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("HeroVideoSlider", "URL conversion error", e)
                        videoError = "Invalid video URL"
                        isVideoLoading = false
                    }
                }
                
                val exoPlayer = remember(downloadUrl) {
                    if (downloadUrl != null) {
                        androidx.media3.exoplayer.ExoPlayer.Builder(context).build().apply {
                            val mediaItem = androidx.media3.common.MediaItem.Builder()
                                .setUri(downloadUrl)
                                .setMimeType(androidx.media3.common.MimeTypes.VIDEO_MP4)
                                .build()
                            setMediaItem(mediaItem)
                            prepare()
                            playWhenReady = true
                        }
                    } else {
                        null
                    }
                }
                
                // Observe lifecycle to pause/resume video
                DisposableEffect(lifecycleOwner) {
                    val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
                        when (event) {
                            androidx.lifecycle.Lifecycle.Event.ON_PAUSE -> {
                                exoPlayer?.pause()
                                android.util.Log.d("HeroVideoSlider", "Video paused (lifecycle)")
                            }
                            androidx.lifecycle.Lifecycle.Event.ON_RESUME -> {
                                // Don't auto-resume, let user control
                            }
                            androidx.lifecycle.Lifecycle.Event.ON_DESTROY -> {
                                exoPlayer?.release()
                            }
                            else -> {}
                        }
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose {
                        lifecycleOwner.lifecycle.removeObserver(observer)
                    }
                }
                
                DisposableEffect(exoPlayer) {
                    val listener = object : androidx.media3.common.Player.Listener {
                        override fun onPlaybackStateChanged(playbackState: Int) {
                            android.util.Log.d("HeroVideoSlider", "Playback State: $playbackState")
                            when (playbackState) {
                                androidx.media3.common.Player.STATE_BUFFERING -> {
                                    isVideoLoading = true
                                }
                                androidx.media3.common.Player.STATE_READY -> {
                                    isVideoLoading = false
                                    videoError = null
                                }
                                androidx.media3.common.Player.STATE_ENDED -> {
                                    isVideoLoading = false
                                }
                            }
                        }

                        override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                            android.util.Log.e("HeroVideoSlider", "Player Error: ${error.message}", error)
                            isVideoLoading = false
                            videoError = "Playback error: ${error.message ?: "Unknown error"}"
                        }
                    }
                    exoPlayer?.addListener(listener)
                    onDispose {
                        exoPlayer?.removeListener(listener)
                        exoPlayer?.release()
                    }
                }

                // Observe when isPlaying changes from true to false
                LaunchedEffect(isPlaying) {
                    if (!isPlaying) {
                        exoPlayer?.pause()
                        onPause()
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable {
                            // Toggle play/pause when clicking anywhere on the video
                            exoPlayer?.let {
                                if (it.isPlaying) {
                                    it.pause()
                                } else {
                                    it.play()
                                }
                            }
                        }
                ) {
                    if (videoError == null && exoPlayer != null) {
                        AndroidView(
                            factory = { ctx ->
                                androidx.media3.ui.PlayerView(ctx).apply {
                                    player = exoPlayer
                                    useController = true
                                    controllerShowTimeoutMs = 3000
                                    setShowNextButton(false)
                                    setShowPreviousButton(false)
                                    // Enable fullscreen button - the button is controlled by the layout
                                    // Setting the listener enables the button
                                    setControllerOnFullScreenModeChangedListener { isFullScreen ->
                                        android.util.Log.d("HeroVideoSlider", "Fullscreen: $isFullScreen")
                                        // The PlayerView handles the actual fullscreen toggle
                                        // Just log it here for debugging
                                    }
                                    resizeMode = androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                                    layoutParams = ViewGroup.LayoutParams(
                                        ViewGroup.LayoutParams.MATCH_PARENT,
                                        ViewGroup.LayoutParams.MATCH_PARENT
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    } else if (videoError != null) {
                        // Error State
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = androidx.compose.material.icons.Icons.Default.Error,
                                    contentDescription = "Error",
                                    tint = Color.Red,
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = videoError!!,
                                    color = Color.White,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(16.dp),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }

                    // Loading indicator - show when buffering or initially loading
                    if (isVideoLoading && videoError == null) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.5f)),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(48.dp),
                                color = AtmiyaPrimary,
                                strokeWidth = 4.dp
                            )
                        }
                    }
                }
            } else {
                // Thumbnail and Overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable { onPlayClick() }
                ) {
                    // Thumbnail
                    AsyncImage(
                        model = video.thumbnailUrl ?: "https://via.placeholder.com/400x220/6366F1/FFFFFF?text=${video.title}",
                        contentDescription = video.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    
                    // Gradient Overlay
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color.Black.copy(alpha = 0.7f)
                                    )
                                )
                            )
                    )
                    
                    // Content
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Category Badge
                        if (video.category.isNotEmpty()) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = AtmiyaAccent.copy(alpha = 0.9f)
                            ) {
                                Text(
                                    text = video.category.uppercase(),
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        } else {
                            Spacer(modifier = Modifier.height(1.dp))
                        }
                        
                        // Title and Watch Button
                        Column {
                            Text(
                                text = video.title,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            // Watch Button
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .background(
                                        color = Color.White.copy(alpha = 0.3f),
                                        shape = RoundedCornerShape(20.dp)
                                    )
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Play",
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Play Video",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun VideoSliderShimmer() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(240.dp)
            .padding(horizontal = 28.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.LightGray.copy(alpha = 0.3f))
    )
}

@Composable
fun EmptyVideoState() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(240.dp)
            .padding(horizontal = 28.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "No featured videos available",
            style = MaterialTheme.typography.bodyLarge,
            color = Color.Gray
        )
    }
}

@Composable
fun ErrorVideoState(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(240.dp)
            .padding(horizontal = 28.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Unable to load videos",
            style = MaterialTheme.typography.bodyLarge,
            color = Color.Gray
        )
    }
}
