package com.atmiya.innovation.ui.auth

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.atmiya.innovation.R
import com.atmiya.innovation.ui.theme.AtmiyaPrimary
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SplashScreen(
    onSessionValid: () -> Unit,
    onSessionInvalid: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    
    // Video State
    var videoUri by remember { mutableStateOf<android.net.Uri?>(null) }
    // Error State for Debugging
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    // Initialize Player immediately
    val exoPlayer = remember {
        androidx.media3.exoplayer.ExoPlayer.Builder(context).build()
    }

    // Try to get local URI synchronously first
    val localUri = remember {
        val resId = context.resources.getIdentifier("splash_video", "raw", context.packageName)
        if (resId != 0) {
            android.net.Uri.parse("android.resource://${context.packageName}/$resId")
        } else {
            null
        }
    }

    // Load video
    LaunchedEffect(Unit) {
        if (localUri != null) {
            videoUri = localUri
            try {
                exoPlayer.setMediaItem(androidx.media3.common.MediaItem.fromUri(localUri))
                exoPlayer.prepare()
                exoPlayer.playWhenReady = true
            } catch (e: Exception) {
                errorMessage = "Player Init Error: ${e.message}"
            }
        } else {
            // Fallback to Firebase Storage
            try {
                val storage = com.google.firebase.storage.FirebaseStorage.getInstance()
                val gsReference = storage.getReferenceFromUrl("gs://atmiya-eacdf.firebasestorage.app/Netfund_Splash_Screen.mp4")
                gsReference.downloadUrl.addOnSuccessListener { uri ->
                    videoUri = uri
                    try {
                        exoPlayer.setMediaItem(androidx.media3.common.MediaItem.fromUri(uri))
                        exoPlayer.prepare()
                        exoPlayer.playWhenReady = true
                    } catch (e: Exception) {
                        errorMessage = "Player Init Error: ${e.message}"
                    }
                }.addOnFailureListener {
                    errorMessage = "Video Load Error: Please add 'splash_video.mp4' to 'app/src/main/res/raw/'"
                    android.util.Log.e("SplashScreen", "Failed to get video URL", it)
                }
            } catch (e: Exception) {
                errorMessage = "Storage Init Error: ${e.message}"
                android.util.Log.e("SplashScreen", "Error initializing storage", e)
            }
        }
    }

    // Dispose player
    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }

    // Animation State (Restored for Footer)
    var startAnimation by remember { mutableStateOf(false) }
    val alphaAnim = animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(durationMillis = 1500), label = "alpha"
    )

    // Navigation Logic with Fallback
    LaunchedEffect(Unit) {
        startAnimation = true
        // Fallback: If video fails or takes too long, navigate anyway after 8 seconds (increased)
        delay(8000)
        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            onSessionValid()
        } else {
            onSessionInvalid()
        }
    }
    
    // Listen for completion
    DisposableEffect(exoPlayer) {
        val listener = object : androidx.media3.common.Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == androidx.media3.common.Player.STATE_ENDED) {
                    val user = FirebaseAuth.getInstance().currentUser
                    if (user != null) {
                        onSessionValid()
                    } else {
                        onSessionInvalid()
                    }
                }
                // Handle error state?
                if (playbackState == androidx.media3.common.Player.STATE_IDLE && exoPlayer.playerError != null) {
                     errorMessage = "Playback Error: ${exoPlayer.playerError?.message}"
                }
            }
            
            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                 errorMessage = "Player Error: ${error.message}"
                 android.util.Log.e("SplashScreen", "Player Error: ${error.message}")
                 // Navigate on error
                 val user = FirebaseAuth.getInstance().currentUser
                 if (user != null) {
                     onSessionValid()
                 } else {
                     onSessionInvalid()
                 }
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black), // Black background for video
        contentAlignment = Alignment.Center
    ) {
        // Video Player
        androidx.compose.ui.viewinterop.AndroidView(
            factory = { ctx ->
                androidx.media3.ui.PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false
                    resizeMode = androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_ZOOM // Fill screen
                    setShutterBackgroundColor(android.graphics.Color.BLACK) // Set shutter to black
                }
            },
            modifier = Modifier.fillMaxSize()
        )
        
        // Debug Error Message
        if (errorMessage != null) {
            Text(
                text = errorMessage ?: "",
                color = Color.Red,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(16.dp)
                    .background(Color.Black.copy(alpha = 0.7f))
            )
        }

        // Powered By Section (Overlay)
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 48.dp)
                .alpha(alphaAnim.value), // Restored fade-in
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Powered by",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.9f), // Changed to White for visibility on video
                    modifier = Modifier.padding(end = 8.dp)
                )
                Image(
                    painter = painterResource(id = R.drawable.hl_group_logo),
                    contentDescription = "HL Group Logo",
                    modifier = Modifier.size(60.dp)
                )
            }
        }
    }
}
