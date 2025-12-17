package com.atmiya.innovation.ui.video

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import compose.icons.TablerIcons
import compose.icons.tablericons.Plus
import compose.icons.tablericons.PlayerPlay
import compose.icons.tablericons.ArrowLeft
import compose.icons.tablericons.Search
import compose.icons.tablericons.Trash
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.asImageBitmap
import com.atmiya.innovation.data.MentorVideo
import com.atmiya.innovation.repository.FirestoreRepository
import com.atmiya.innovation.repository.StorageRepository
import com.atmiya.innovation.ui.theme.AtmiyaPrimary
import com.atmiya.innovation.ui.theme.AtmiyaSecondary
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.Timestamp
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MentorVideoScreen(
    role: String, 
    mentorId: String? = null, 
    onBack: (() -> Unit)? = null,
    onVideoClick: ((String) -> Unit)? = null
) {
    val repository = remember { FirestoreRepository() }
    val storageRepository = remember { StorageRepository() }
    val auth = FirebaseAuth.getInstance()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    
    // Upload state
    var isUploading by remember { mutableStateOf(false) }
    var uploadProgress by remember { mutableStateOf(0) }
    var uploadVideoTitle by remember { mutableStateOf("") }

    // State for videos
    var videos by remember { mutableStateOf(listOf<MentorVideo>()) }
    var isLoading by remember { mutableStateOf(true) }
    var mentorName by remember { mutableStateOf<String?>(null) }

    // Fetch videos and mentor name
    LaunchedEffect(mentorId) {
        try {
            // Fetch mentor name if mentorId is provided
            if (!mentorId.isNullOrBlank()) {
                val mentor = repository.getMentor(mentorId)
                mentorName = mentor?.name
            }
            videos = repository.getMentorVideos(mentorId)
        } catch (e: Exception) {
            Toast.makeText(context, "Error fetching videos", Toast.LENGTH_SHORT).show()
        } finally {
            isLoading = false
        }
    }

    var showUploadDialog by remember { mutableStateOf(false) }
    
    // Search state
    var searchQuery by remember { mutableStateOf("") }
    
    // Delete confirmation dialog state
    var showDeleteDialog by remember { mutableStateOf(false) }
    var videoToDelete by remember { mutableStateOf<MentorVideo?>(null) }
    
    // Get current user ID for determining if user can delete
    val currentUserId = auth.currentUser?.uid
    
    // Filter videos based on search query
    val filteredVideos = remember(videos, searchQuery) {
        if (searchQuery.isBlank()) {
            videos
        } else {
            videos.filter { video ->
                video.title.contains(searchQuery, ignoreCase = true) ||
                video.mentorName.contains(searchQuery, ignoreCase = true) ||
                video.description.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    // Determine screen title
    val screenTitle = if (mentorName != null) {
        "Videos by $mentorName"
    } else {
        "Mentor Video Library"
    }
    
    // Delete confirmation dialog
    if (showDeleteDialog && videoToDelete != null) {
        AlertDialog(
            onDismissRequest = { 
                showDeleteDialog = false
                videoToDelete = null
            },
            title = { Text("Delete Video") },
            text = { Text("Are you sure you want to delete \"${videoToDelete?.title}\"? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        // Capture video data before clearing state
                        val videoToDeleteCopy = videoToDelete
                        showDeleteDialog = false
                        videoToDelete = null
                        
                        if (videoToDeleteCopy != null) {
                            scope.launch {
                                try {
                                    val videoId = videoToDeleteCopy.id
                                    val videoMentorId = videoToDeleteCopy.mentorId
                                    val currentUser = auth.currentUser?.uid
                                    android.util.Log.d("MentorVideoScreen", "Delete: videoId=$videoId, videoMentorId=$videoMentorId, currentUser=$currentUser")
                                    repository.deleteMentorVideo(videoId)
                                    videos = repository.getMentorVideos(mentorId)
                                    Toast.makeText(context, "Video deleted successfully", Toast.LENGTH_SHORT).show()
                                } catch (e: Exception) {
                                    val errorMsg = e.message ?: e.cause?.message ?: e::class.simpleName ?: "Unknown error"
                                    android.util.Log.e("MentorVideoScreen", "Delete failed: $errorMsg", e)
                                    Toast.makeText(context, "Error: $errorMsg", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { 
                        showDeleteDialog = false
                        videoToDelete = null
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                // Always show TopAppBar with back button
                TopAppBar(
                    title = { 
                        Text(
                            screenTitle, 
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        ) 
                    },
                    navigationIcon = {
                        IconButton(onClick = { onBack?.invoke() }) {
                            Icon(
                                TablerIcons.ArrowLeft,
                                contentDescription = "Back",
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        titleContentColor = AtmiyaPrimary
                    )
                )
            },
            floatingActionButton = {
                if (role == "mentor" && !isUploading) {
                    FloatingActionButton(
                        onClick = { showUploadDialog = true },
                        containerColor = AtmiyaSecondary,
                        contentColor = Color.White
                    ) {
                        Icon(TablerIcons.Plus, contentDescription = "Upload Video", modifier = Modifier.size(24.dp))
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
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Search Bar
                    item {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search videos...") },
                            leadingIcon = {
                                Icon(
                                    TablerIcons.Search,
                                    contentDescription = "Search",
                                    tint = Color.Gray
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            singleLine = true,
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AtmiyaPrimary,
                                unfocusedBorderColor = Color.LightGray
                            )
                        )
                    }
                    
                    if (filteredVideos.isEmpty()) {
                        item {
                            Text(
                                if (searchQuery.isNotBlank()) "No videos found matching \"$searchQuery\"" else "No videos available yet.", 
                                color = Color.Gray,
                                modifier = Modifier.padding(vertical = 16.dp)
                            )
                        }
                    } else {
                        items(filteredVideos, key = { it.id }) { video ->
                            VideoCard(
                                video = video,
                                canDelete = role == "mentor" && video.mentorId == currentUserId,
                                onVideoClick = {
                                    if (onVideoClick != null) {
                                        onVideoClick(video.id)
                                    } else {
                                        // Fallback: open in browser if no navigation callback
                                        if (video.videoUrl.isNotBlank()) {
                                            try {
                                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(video.videoUrl))
                                                context.startActivity(intent)
                                            } catch (e: Exception) {
                                                Toast.makeText(context, "Could not open video", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                },
                                onDeleteClick = {
                                    videoToDelete = video
                                    showDeleteDialog = true
                                }
                            )
                        }
                    }
                    
                    // Add bottom padding to avoid FAB overlap with last item
                    item {
                        Spacer(modifier = Modifier.height(if (isUploading) 140.dp else 100.dp))
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
                    },
                    onUploadFromDevice = { uri, title, thumbnailUrl, duration ->
                        // Close dialog immediately - upload continues in background
                        showUploadDialog = false
                        uploadVideoTitle = title
                        isUploading = true
                        uploadProgress = 0
                        
                        scope.launch {
                            try {
                                val user = auth.currentUser
                                if (user != null) {
                                    val userProfile = repository.getUser(user.uid)
                                    
                                    // Upload video with progress tracking
                                    val videoUrl = storageRepository.uploadMentorVideoWithProgress(
                                        context = context,
                                        mentorId = user.uid,
                                        uri = uri
                                    ) { progress ->
                                        uploadProgress = progress
                                    }
                                    
                                    // Create video record
                                    val video = MentorVideo(
                                        id = UUID.randomUUID().toString(),
                                        mentorId = user.uid,
                                        mentorName = userProfile?.name ?: "Unknown Mentor",
                                        title = title,
                                        description = "",
                                        videoUrl = videoUrl,
                                        thumbnailUrl = thumbnailUrl.ifBlank { null },
                                        duration = duration,
                                        viewsCount = 0,
                                        createdAt = Timestamp.now()
                                    )
                                    repository.addMentorVideo(video)
                                    
                                    // Refresh list
                                    videos = repository.getMentorVideos()
                                    Toast.makeText(context, "Video uploaded successfully!", Toast.LENGTH_SHORT).show()
                                }
                            } catch (e: Exception) {
                                Toast.makeText(context, "Upload failed: ${e.message}", Toast.LENGTH_LONG).show()
                            } finally {
                                isUploading = false
                                uploadProgress = 0
                            }
                        }
                    }
                )
            }
        }
        
        // Floating Upload Progress Indicator
        if (isUploading) {
            Card(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Uploading Video...",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = uploadVideoTitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                        }
                        Text(
                            text = "$uploadProgress%",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = AtmiyaPrimary
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { uploadProgress / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp),
                        color = AtmiyaPrimary,
                        trackColor = Color(0xFFE0E0E0),
                    )
                }
            }
        }
    }
}

@Composable
fun VideoCard(
    video: MentorVideo, 
    canDelete: Boolean = false,
    onVideoClick: () -> Unit = {},
    onDeleteClick: () -> Unit = {}
) {
    val context = LocalContext.current
    
    // State for auto-generated thumbnail
    var autoThumbnail by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    
    // Check if YouTube video and extract thumbnail URL
    val youTubeThumbnailUrl = remember(video.videoUrl) {
        if (isYouTubeUrl(video.videoUrl)) {
            val videoId = extractYouTubeVideoId(video.videoUrl)
            if (videoId != null) "https://img.youtube.com/vi/$videoId/maxresdefault.jpg" else null
        } else null
    }
    
    // Try to generate thumbnail from video URL if no thumbnail is set and not YouTube
    LaunchedEffect(video.videoUrl) {
        if (video.thumbnailUrl.isNullOrBlank() && youTubeThumbnailUrl == null && video.videoUrl.isNotBlank()) {
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                try {
                    val retriever = android.media.MediaMetadataRetriever()
                    retriever.setDataSource(video.videoUrl, HashMap())
                    val bitmap = retriever.getFrameAtTime(1000000) // Get frame at 1 second
                    retriever.release()
                    autoThumbnail = bitmap
                } catch (e: Exception) {
                    android.util.Log.e("VideoCard", "Failed to generate thumbnail: ${e.message}")
                }
            }
        }
    }
    
    // Determine the thumbnail to display
    val displayThumbnailUrl = video.thumbnailUrl?.takeIf { it.isNotBlank() } ?: youTubeThumbnailUrl
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onVideoClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
    ) {
        Column {
            // Thumbnail Area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .background(Color.DarkGray),
                contentAlignment = Alignment.Center
            ) {
                // Priority: 1. thumbnailUrl or YouTube thumbnail, 2. auto-generated, 3. gray placeholder
                when {
                    displayThumbnailUrl != null -> {
                        coil.compose.AsyncImage(
                            model = displayThumbnailUrl,
                            contentDescription = video.title,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )
                    }
                    autoThumbnail != null -> {
                        androidx.compose.foundation.Image(
                            bitmap = autoThumbnail!!.asImageBitmap(),
                            contentDescription = video.title,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )
                    }
                    // else: gray background (already set on Box)
                }
                
                // Play Button Overlay
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(
                            Color.Black.copy(alpha = 0.6f),
                            shape = androidx.compose.foundation.shape.CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        TablerIcons.PlayerPlay,
                        contentDescription = "Play",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
                
                // Duration Badge
                if (video.duration.isNotBlank() && video.duration != "00:00") {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(8.dp)
                            .background(
                                Color.Black.copy(alpha = 0.8f),
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = video.duration,
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
            
            // Video Info Below (YouTube-like)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = video.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        maxLines = 2,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = video.mentorName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                }
                
                // Delete Button (only for mentor's own videos)
                if (canDelete) {
                    IconButton(
                        onClick = { onDeleteClick() },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            TablerIcons.Trash,
                            contentDescription = "Delete",
                            tint = Color.Red,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun UploadVideoDialog(
    onDismiss: () -> Unit, 
    onUpload: (MentorVideo) -> Unit,
    onUploadFromDevice: ((android.net.Uri, String, String, String) -> Unit)? = null
) {
    var title by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    var thumbnailUrl by remember { mutableStateOf("") }
    var duration by remember { mutableStateOf("") }
    
    // Upload mode: 0 = YouTube/Link, 1 = Device Upload
    var uploadMode by remember { mutableStateOf(0) }
    var selectedVideoUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var selectedVideoName by remember { mutableStateOf("") }
    
    val context = LocalContext.current
    
    // Video Picker Launcher
    val videoPickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        if (uri != null) {
            // Check file size (max 100MB)
            val fileSize = try {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val sizeIndex = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE)
                    if (cursor.moveToFirst() && sizeIndex != -1) {
                        cursor.getLong(sizeIndex)
                    } else 0L
                } ?: 0L
            } catch (e: Exception) { 0L }
            
            val maxSizeBytes = 100 * 1024 * 1024L // 100MB
            if (fileSize > maxSizeBytes) {
                Toast.makeText(context, "Video too large. Max size: 100MB", Toast.LENGTH_SHORT).show()
            } else {
                selectedVideoUri = uri
                // Get file name
                val fileName = try {
                    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                        val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                        if (cursor.moveToFirst() && nameIndex != -1) {
                            cursor.getString(nameIndex)
                        } else "Selected Video"
                    } ?: "Selected Video"
                } catch (e: Exception) { "Selected Video" }
                selectedVideoName = fileName
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Upload Mentor Video") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Upload Mode Tabs
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = uploadMode == 0,
                        onClick = { uploadMode = 0 },
                        label = { Text("YouTube/Link") },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = uploadMode == 1,
                        onClick = { uploadMode = 1 },
                        label = { Text("From Device") },
                        modifier = Modifier.weight(1f)
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = title, 
                    onValueChange = { title = it }, 
                    label = { Text("Video Title") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                if (uploadMode == 0) {
                    // YouTube/Link Mode
                    OutlinedTextField(
                        value = url, 
                        onValueChange = { url = it }, 
                        label = { Text("Video URL / Link") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = thumbnailUrl, 
                        onValueChange = { thumbnailUrl = it }, 
                        label = { Text("Thumbnail URL (Optional)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    // Device Upload Mode
                    OutlinedButton(
                        onClick = { videoPickerLauncher.launch("video/*") },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            TablerIcons.Plus,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (selectedVideoUri != null) "Change Video" else "Select Video")
                    }
                    
                    if (selectedVideoUri != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    TablerIcons.PlayerPlay,
                                    contentDescription = null,
                                    tint = AtmiyaPrimary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = selectedVideoName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Max file size: 100MB",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = duration, 
                    onValueChange = { duration = it }, 
                    label = { Text("Duration (e.g. 10:00)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            val isValid = title.isNotBlank() && (
                (uploadMode == 0 && url.isNotBlank()) || 
                (uploadMode == 1 && selectedVideoUri != null)
            )
            
            Button(
                onClick = {
                    if (uploadMode == 0) {
                        // YouTube/Link mode - same as before
                        onUpload(MentorVideo(
                            id = "", // Set by caller
                            mentorId = "", // Set by caller
                            mentorName = "", // Set by caller
                            title = title,
                            description = "",
                            videoUrl = url,
                            thumbnailUrl = thumbnailUrl.ifBlank { null },
                            duration = duration.ifBlank { "00:00" },
                            viewsCount = 0,
                            createdAt = null
                        ))
                    } else {
                        // Device upload mode - pass uri to callback
                        selectedVideoUri?.let { uri ->
                            onUploadFromDevice?.invoke(uri, title, thumbnailUrl, duration.ifBlank { "00:00" })
                        }
                    }
                },
                enabled = isValid
            ) {
                Text(if (uploadMode == 1) "Upload" else "Add Video")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
