package com.atmiya.innovation.ui.dashboard.startup

import android.net.Uri
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.atmiya.innovation.data.FeaturedVideo
import com.google.accompanist.pager.HorizontalPagerIndicator
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import com.atmiya.innovation.ui.theme.AtmiyaPrimary
import compose.icons.TablerIcons
import compose.icons.tablericons.PlayerPlay
import compose.icons.tablericons.Maximize
import compose.icons.tablericons.X
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import coil.request.ImageRequest
import coil.compose.AsyncImage

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HeroVideoSlider(
    videosState: Any? = null, 
    isVisible: Boolean = true,
    onVideoClick: (String) -> Unit = {}
) {
    if (!isVisible) return

    val videos = when (videosState) {
        is com.atmiya.innovation.ui.dashboard.startup.StartupDashboardViewModel.UiState.Success<*> -> {
            (videosState.data as? List<*>)?.filterIsInstance<FeaturedVideo>() ?: emptyList()
        }
        is List<*> -> {
            videosState.filterIsInstance<FeaturedVideo>()
        }
        else -> emptyList()
    }

    if (videos.isEmpty()) return

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
    ) {
        val pagerState = rememberPagerState(pageCount = { videos.size })

        // Banner Video Container
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp), // Height for standard 16:9 feel
            contentPadding = PaddingValues(horizontal = 16.dp), // Peek next item
            pageSpacing = 16.dp
        ) { page ->
            VideoBannerItem(video = videos[page])
        }
        
        // Indicators
        if (videos.size > 1) {
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                repeat(videos.size) { iteration ->
                    val color = if (pagerState.currentPage == iteration) AtmiyaPrimary else Color.LightGray.copy(alpha = 0.5f)
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 3.dp)
                            .clip(CircleShape)
                            .background(color)
                            .size(if (pagerState.currentPage == iteration) 8.dp else 6.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun VideoBannerItem(video: FeaturedVideo) {
    var isPlaying by remember { mutableStateOf(false) }
    var showFullScreen by remember { mutableStateOf(false) }
    val context = LocalContext.current

    var resolvedThumbnailModel by remember { mutableStateOf<Any?>(null) }

    LaunchedEffect(video) {
        val thumb = video.thumbnailUrl
        val vid = video.videoUrl
        
        // Prioritize thumbnail, fallback to video (for frame extraction)
        val sourceToUse = if (!thumb.isNullOrEmpty()) thumb else vid
        
        if (sourceToUse.startsWith("gs://")) {
             try {
                 resolvedThumbnailModel = FirebaseStorage.getInstance().getReferenceFromUrl(sourceToUse).downloadUrl.await()
             } catch (e: Exception) {
                 e.printStackTrace()
                 resolvedThumbnailModel = sourceToUse
             }
        } else {
             resolvedThumbnailModel = sourceToUse
        }
    }

    if (showFullScreen) {
        BannerFullScreenPlayer(video = video, onDismiss = { showFullScreen = false })
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.Black)
            .clickable { isPlaying = !isPlaying }
    ) {
        if (isPlaying) {
            BannerVideoPlayer(videoUrl = video.videoUrl)
        } else {
            // Thumbnail
            coil.compose.AsyncImage(
                model = coil.request.ImageRequest.Builder(context)
                    .data(resolvedThumbnailModel)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            
            // Gradient Overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        androidx.compose.ui.graphics.Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f)),
                            startY = 100f
                        )
                    )
            )

            // Play Button (Center)
            Box(
                modifier = Modifier.align(Alignment.Center),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = compose.icons.TablerIcons.PlayerPlay,
                    contentDescription = "Play",
                    tint = Color.White,
                    modifier = Modifier
                        .size(48.dp)
                        .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                        .padding(8.dp)
                )
            }

            // Text Info (Bottom Left)
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp)
            ) {
                if (video.category.isNotEmpty()) {
                    Text(
                        text = video.category.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = AtmiyaPrimary,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                        modifier = Modifier
                            .background(Color.White.copy(alpha = 0.9f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                }
                Text(
                    text = video.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                    maxLines = 2,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }
        }
        
        // Actions (Top Right)
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp)
        ) {
            // Fullscreen Button
            IconButton(
                onClick = { showFullScreen = true },
                modifier = Modifier
                     .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                     .size(32.dp)
            ) {
                Icon(
                    imageVector = compose.icons.TablerIcons.Maximize,
                    contentDescription = "Fullscreen",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun BannerFullScreenPlayer(video: FeaturedVideo, onDismiss: () -> Unit) {
    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true
        )
    ) {
         Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
            // Re-use BannerVideoPlayer but ensure it handles fullscreen layout
            BannerVideoPlayer(videoUrl = video.videoUrl)
            
            // Close Button
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
            ) {
                Icon(
                     imageVector = compose.icons.TablerIcons.X,
                     contentDescription = "Close",
                     tint = Color.White
                )
            }
        }
    }
}

@Composable
fun BannerVideoPlayer(videoUrl: String) {
    if (videoUrl.contains("youtube.com") || videoUrl.contains("youtu.be")) {
        // Extract Video ID
        val videoId = extractYouTubeId(videoUrl)
        if (videoId != null) {
            YouTubeVideoPlayer(videoId = videoId)
        } else {
             Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Invalid YouTube URL", color = Color.White)
            }
        }
    } else {
        // Treat as MP4 / Native
        NativeBannerVideoPlayer(videoUrl = videoUrl)
    }
}

@Composable
fun NativeBannerVideoPlayer(videoUrl: String) {
    val context = LocalContext.current
    var playableUrl by remember { mutableStateOf<String?>(null) }
    
    // Resolve GS URL
    LaunchedEffect(videoUrl) {
        if (videoUrl.startsWith("gs://")) {
            try {
                val storageRef = FirebaseStorage.getInstance().getReferenceFromUrl(videoUrl)
                playableUrl = storageRef.downloadUrl.await().toString()
            } catch (e: Exception) {
                android.util.Log.e("HeroVideoSlider", "Error resolving gs url", e)
            }
        } else {
            playableUrl = videoUrl
        }
    }

    if (playableUrl != null) {
        val exoPlayer = remember {
            ExoPlayer.Builder(context).build().apply {
                setMediaItem(MediaItem.fromUri(Uri.parse(playableUrl)))
                prepare()
                playWhenReady = true // Auto-play since user explicitly clicked Play
            }
        }

        DisposableEffect(Unit) {
            onDispose {
                exoPlayer.release()
            }
        }

        AndroidView(
            factory = {
                PlayerView(it).apply {
                    player = exoPlayer
                    useController = true
                }
            },
            modifier = Modifier.fillMaxSize()
        )
    } else {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Color.White)
        }
    }
}


@Composable
fun YouTubeVideoPlayer(videoId: String) {
    val htmlData = """
        <!DOCTYPE html>
        <html>
          <head>
            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
            <style>
                body, html { margin: 0; padding: 0; background-color: black; width: 100%; height: 100%; overflow: hidden; }
                #player { width: 100%; height: 100%; }
            </style>
          </head>
          <body>
            <div id="player"></div>
            <script>
              var tag = document.createElement('script');
              tag.src = "https://www.youtube.com/iframe_api";
              var firstScriptTag = document.getElementsByTagName('script')[0];
              firstScriptTag.parentNode.insertBefore(tag, firstScriptTag);

              var player;
              function onYouTubeIframeAPIReady() {
                player = new YT.Player('player', {
                  height: '100%',
                  width: '100%',
                  videoId: '$videoId',
                  playerVars: {
                    'playsinline': 1,
                    'autoplay': 1, 
                    'controls': 1,
                    'rel': 0,
                    'fs': 1,
                    'modestbranding': 1
                  },
                  events: {
                    'onReady': onPlayerReady
                  }
                });
              }

              function onPlayerReady(event) {
                event.target.playVideo();
              }
            </script>
          </body>
        </html>
    """.trimIndent()
    
    AndroidView(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        factory = { context ->
            WebView(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.loadWithOverviewMode = true
                settings.useWideViewPort = true
                settings.mediaPlaybackRequiresUserGesture = false
                
                webChromeClient = WebChromeClient()
                webViewClient = WebViewClient()
                
                loadDataWithBaseURL("https://www.youtube.com", htmlData, "text/html", "UTF-8", null)
            }
        },
        update = { webView ->
             // webView.loadUrl(embedUrl) 
        }
    )
}

fun extractYouTubeId(url: String): String? {
    // Basic extraction logic
    return try {
        if (url.contains("youtu.be/")) {
            url.split("youtu.be/")[1].split("?")[0]
        } else if (url.contains("v=")) {
            url.split("v=")[1].split("&")[0]
        } else {
            null
        }
    } catch (e: Exception) {
        null
    }
}
