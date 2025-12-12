@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
package com.atmiya.innovation.ui.dashboard

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import compose.icons.TablerIcons
import compose.icons.tablericons.*
import compose.icons.tablericons.ThumbUp
import compose.icons.tablericons.MessageCircle
import compose.icons.tablericons.Photo
import compose.icons.tablericons.PlayerPlay
import compose.icons.tablericons.Plus
import compose.icons.tablericons.DotsVertical
import compose.icons.tablericons.Trash
import compose.icons.tablericons.Send
import compose.icons.tablericons.X
import compose.icons.tablericons.FaceId
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.atmiya.innovation.data.Comment
import com.atmiya.innovation.data.WallPost
import com.atmiya.innovation.repository.FirestoreRepository
import com.atmiya.innovation.ui.components.*
import com.atmiya.innovation.ui.theme.AtmiyaPrimary
import com.atmiya.innovation.ui.theme.AtmiyaSecondary
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import java.util.UUID
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.media3.common.MediaItem

import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.TextButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Scaffold
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider

import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WallScreen(
    viewModel: WallViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    onNavigateToProfile: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToDashboard: () -> Unit, // Added callback
    onLogout: () -> Unit,
    onOpenDrawer: () -> Unit
) {
    val posts by viewModel.posts.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val filterType by viewModel.filterType.collectAsState()
    val selectedSector by viewModel.selectedSector.collectAsState()
    val error by viewModel.error.collectAsState()
    
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val scope = rememberCoroutineScope()

    LaunchedEffect(error) {
        error?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.clearError()
        }
    }
    
    val sectors = listOf("All", "Tech", "Fintech", "Healthcare", "EdTech", "AgriTech", "CleanTech")
    
    var showCreateDialog by remember { mutableStateOf(false) }
    
    // User Data State
    var userName by remember { mutableStateOf("") }
    var userPhotoUrl by remember { mutableStateOf("") }
    var userRole by remember { mutableStateOf("") }
    
    val repository = remember { FirestoreRepository() }

    // Fetch User Data
    LaunchedEffect(auth.currentUser?.uid) {
        val uid = auth.currentUser?.uid
        if (uid != null) {
            val user = repository.getUser(uid)
            if (user != null) {
                userName = user.name
                userPhotoUrl = user.profilePhotoUrl ?: "" // Fixed field name
                userRole = user.role.replaceFirstChar { if (it.isLowerCase()) it.titlecase(java.util.Locale.getDefault()) else it.toString() }
            } else {
                // Fallback to Auth if Firestore fails or user not found
                userName = auth.currentUser?.displayName ?: "User"
                userPhotoUrl = auth.currentUser?.photoUrl?.toString() ?: ""
            }
        }
    }

    val currentRoute = "Wall" // Since we are on WallScreen

    val swipeRefreshState = rememberSwipeRefreshState(isRefreshing = isLoading)

    Scaffold(

        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateDialog = true },
                containerColor = AtmiyaPrimary,
                contentColor = Color.White,
                shape = CircleShape
            ) {
                Icon(TablerIcons.Plus, contentDescription = "Create Post", tint = Color.White, modifier = Modifier.size(24.dp))
            }
        }
    ) { innerPadding ->

            SwipeRefresh(
                state = swipeRefreshState,
                onRefresh = { viewModel.refresh() },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                    // Content Area
                    // Posts List
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 100.dp) // No horizontal padding for full width
                    ) {
                        // "Post your Activity" Input Bar - kept but full width style
                        item {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp) // Keep some padding for the input area itself
                                    .height(56.dp)
                                    .clip(RoundedCornerShape(28.dp))
                                    .clickable { showCreateDialog = true },
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(28.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "What's on your mind?",
                                        color = Color.Gray,
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Icon(
                                        TablerIcons.Photo,
                                        contentDescription = null,
                                        tint = AtmiyaPrimary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                            Divider(color = Color.LightGray.copy(alpha = 0.3f), thickness = 8.dp)
                        }

                        if (posts.isEmpty() && !isLoading) {
                            item {
                                Text(
                                    "No posts found.",
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                    modifier = Modifier.padding(top = 32.dp).fillMaxWidth(),
                                    style = MaterialTheme.typography.bodyLarge,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        } else {
                            items(
                                items = posts,
                                key = { it.id }
                            ) { post ->
                                PostCard(
                                    post = post,
                                    currentUserId = auth.currentUser?.uid ?: "",
                                    onLike = { viewModel.toggleLike(post) },
                                    onVote = { optionId -> viewModel.voteOnPoll(post, optionId) },
                                    onConnect = {
                                        Toast.makeText(context, "Connect request sent to ${post.authorName}", Toast.LENGTH_SHORT).show()
                                    },
                                    onDelete = {
                                        viewModel.deletePost(post.id)
                                    }
                                )
                                HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f), thickness = 8.dp)
                                
                                // Pagination Trigger
                                if (post == posts.last()) {
                                    LaunchedEffect(Unit) {
                                        viewModel.loadMore()
                                    }
                                }
                            }
                        }
                    }
            }
            
            // Create Post Dialog
            // Create Post Dialog
            if (showCreateDialog) {
                CreatePostScreen(
                    onDismiss = { showCreateDialog = false },
                    onPost = { content, mediaItems, pollQuestion, pollOptions ->
                        viewModel.createPost(context, content, mediaItems, pollQuestion, pollOptions)
                        showCreateDialog = false
                    }
                )
            }
    }
}

// ConnectionsList removed from here as filters are gone

@Composable
fun CreatePostDialog(
    onDismiss: () -> Unit, 
    onPost: (String, List<Pair<Uri, Boolean>>, String?, List<String>) -> Unit
) {
    var text by remember { mutableStateOf("") }
    // List of Pair<Uri, isVideo>
    var selectedMediaItems by remember { mutableStateOf<List<Pair<Uri, Boolean>>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    
    // Poll State
    var isPollMode by remember { mutableStateOf(false) }
    var pollQuestion by remember { mutableStateOf("") }
    var pollOptions by remember { mutableStateOf(listOf("", "")) } 

    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val firestoreRepository = remember { FirestoreRepository() }
    
    // Limit to 5 images/videos per batch or total? User said "multiple". Let's say 10 max total for sanity.
    val maxItems = 10 

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(maxItems)) { uris ->
        if (uris.isNotEmpty()) {
            // Append new items
            val newItems = uris.map { it to false } // isVideo = false
            selectedMediaItems = selectedMediaItems + newItems
            isPollMode = false 
        }
    }
    
    val videoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(maxItems)) { uris ->
        if (uris.isNotEmpty()) {
            val newItems = uris.map { it to true } // isVideo = true
            selectedMediaItems = selectedMediaItems + newItems
            isPollMode = false 
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isPollMode) "Create Poll" else "Create Post", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                if (!isPollMode) {
                    SoftTextField(
                        value = text,
                        onValueChange = { text = it },
                        label = "What's on your mind?",
                        minLines = 3
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    if (selectedMediaItems.isNotEmpty()) {
                        // Horizontal scroll for multiple items
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth().height(150.dp)
                        ) {
                            items(selectedMediaItems.size) { index ->
                                val (uri, isVideo) = selectedMediaItems[index]
                                Box(modifier = Modifier.width(150.dp).fillMaxHeight().clip(RoundedCornerShape(8.dp))) {
                                    if (isVideo) {
                                        Box(modifier = Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
                                            Icon(TablerIcons.PlayerPlay, contentDescription = null, tint = Color.White, modifier = Modifier.size(32.dp))
                                            Text("Video", color = Color.White, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 40.dp))
                                        }
                                    } else {
                                        AsyncImage(
                                            model = uri,
                                            contentDescription = "Selected Image",
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    }
                                    IconButton(
                                        onClick = { 
                                            // Remove item
                                            selectedMediaItems = selectedMediaItems.toMutableList().apply { removeAt(index) }
                                        },
                                        modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).size(24.dp).background(Color.Black.copy(alpha = 0.5f), CircleShape)
                                    ) {
                                        Icon(TablerIcons.X, contentDescription = "Remove", tint = Color.White, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                } else {
                    // Modern Poll UI
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                    ) {
                        SoftTextField(
                            value = pollQuestion,
                            onValueChange = { pollQuestion = it },
                            label = "Ask a question...",
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        pollOptions.forEachIndexed { index, option ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                OutlinedTextField(
                                    value = option,
                                    onValueChange = { newText -> 
                                        pollOptions = pollOptions.toMutableList().apply { set(index, newText) }
                                    },
                                    placeholder = { Text("Option ${index + 1}") },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = AtmiyaPrimary,
                                        unfocusedBorderColor = Color.LightGray.copy(alpha = 0.5f)
                                    ),
                                    singleLine = true
                                )
                                if (pollOptions.size > 2) {
                                    IconButton(onClick = { 
                                        pollOptions = pollOptions.toMutableList().apply { removeAt(index) }
                                    }) {
                                        Icon(TablerIcons.X, contentDescription = "Remove Option", tint = Color.Gray, modifier = Modifier.size(20.dp))
                                    }
                                }
                            }
                        }
                        
                        if (pollOptions.size < 5) {
                            TextButton(onClick = { pollOptions = pollOptions + "" }) {
                                Icon(TablerIcons.Plus, contentDescription = null, tint = AtmiyaPrimary, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Add Option", color = AtmiyaPrimary)
                            }
                        }
                    }
                }

                if (!isPollMode) {
                    val permissionLauncher = rememberLauncherForActivityResult(
                        ActivityResultContracts.RequestMultiplePermissions()
                    ) { permissions ->
                        // Optional: Handle permission result if needed, but we'll retry picker on click
                    }

                    val isPhotoMode = !isPollMode && selectedMediaItems.isNotEmpty() && !selectedMediaItems[0].second
                    val isVideoMode = !isPollMode && selectedMediaItems.isNotEmpty() && selectedMediaItems[0].second

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly // Distribute evenly
                    ) {
                        // Photos Button
                        OutlinedButton(
                            onClick = { 
                                isPollMode = false
                                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                                    imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                } else {
                                    if (androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_EXTERNAL_STORAGE) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                                        imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                    } else {
                                        permissionLauncher.launch(arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE))
                                    }
                                }
                            },
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (!isPollMode && selectedMediaItems.isNotEmpty() && !selectedMediaItems[0].second) AtmiyaPrimary.copy(alpha = 0.1f) else Color.Transparent
                            )
                        ) {
                             Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(TablerIcons.Photo, contentDescription = null, tint = if (isPhotoMode) AtmiyaPrimary else Color.Gray, modifier = Modifier.size(24.dp))
                                Text("Photos", style = MaterialTheme.typography.bodySmall, color = if (isPhotoMode) AtmiyaPrimary else Color.Gray)
                            }
                        }

                        // Videos Button
                        OutlinedButton(
                            onClick = { 
                                isPollMode = false
                                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                                    videoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))
                                } else {
                                    if (androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_EXTERNAL_STORAGE) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                                        videoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))
                                    } else {
                                        permissionLauncher.launch(arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE))
                                    }
                                }
                            },
                             colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (!isPollMode && selectedMediaItems.isNotEmpty() && selectedMediaItems[0].second) AtmiyaPrimary.copy(alpha = 0.1f) else Color.Transparent
                            )
                        ) {
                             Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(TablerIcons.PlayerPlay, contentDescription = null, tint = if (isVideoMode) AtmiyaPrimary else Color.Gray, modifier = Modifier.size(24.dp))
                                Text("Videos", style = MaterialTheme.typography.bodySmall, color = if (isVideoMode) AtmiyaPrimary else Color.Gray)
                            }
                        }

                        // Poll Button
                        OutlinedButton(
                            onClick = { 
                                isPollMode = true 
                                selectedMediaItems = emptyList() 
                            },
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (isPollMode) AtmiyaPrimary.copy(alpha = 0.1f) else Color.Transparent,
                                contentColor = if (isPollMode) AtmiyaPrimary else Color.Gray
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (isPollMode) AtmiyaPrimary else Color.LightGray)
                        ) {
                             Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(androidx.compose.material.icons.Icons.Filled.List, contentDescription = null, tint = if (isPollMode) AtmiyaPrimary else Color.Gray, modifier = Modifier.size(24.dp))
                                Text("Poll", style = MaterialTheme.typography.bodySmall, color = if (isPollMode) AtmiyaPrimary else Color.Gray)
                            }
                        }
                    }
                    if (!isPollMode) {
                         Text("Max size: Image 5MB, Video 20MB", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), modifier = Modifier.padding(top = 8.dp, start = 4.dp))
                    }
            }
        }
    },
        confirmButton = {
            SoftButton(
                onClick = { 
                    if (isPollMode) {
                        if (pollQuestion.isBlank() || pollOptions.any { it.isBlank() } || pollOptions.size < 2) {
                            Toast.makeText(context, "Please fill all poll fields (min 2 options)", Toast.LENGTH_SHORT).show()
                            return@SoftButton
                        }
                        isLoading = true
                        onPost("", emptyList(), pollQuestion, pollOptions)
                    } else {
                        if (text.isBlank() && selectedMediaItems.isEmpty()) {
                            Toast.makeText(context, "Please add text or media", Toast.LENGTH_SHORT).show()
                            return@SoftButton
                        }
                        isLoading = true
                        onPost(text, selectedMediaItems, null, emptyList()) 
                    }
                },
                text = if (isLoading) "Posting..." else "Post",
                icon = TablerIcons.Send,
                isLoading = isLoading,
                modifier = Modifier.width(120.dp).height(40.dp)
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = AtmiyaPrimary)
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(24.dp)
    )
}

fun formatTimestampIST(timestamp: com.google.firebase.Timestamp?): String {
    if (timestamp == null) return ""
    val sdf = java.text.SimpleDateFormat("dd MMM, hh:mm a", java.util.Locale.getDefault())
    sdf.timeZone = java.util.TimeZone.getTimeZone("Asia/Kolkata")
    return sdf.format(timestamp.toDate())
}

@Composable
fun PostCard(
    post: WallPost,
    currentUserId: String,
    onLike: () -> Unit,
    onVote: (String) -> Unit,
    onConnect: () -> Unit,
    onDelete: () -> Unit // Added callback
) {
    val repository = remember { FirestoreRepository() }
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    
    val isLiked by produceState(initialValue = false, post.id, currentUserId) {
        if (currentUserId.isNotEmpty()) {
            val docRef = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("wallPosts").document(post.id)
                .collection("upvotes").document(currentUserId)
            val listener = docRef.addSnapshotListener { snapshot, _ ->
                value = snapshot?.exists() == true
            }
            awaitDispose { listener.remove() }
        }
    }

    var showComments by rememberSaveable { mutableStateOf(false) }

    // Full Width Card
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White,
        shadowElevation = 0.dp // Flat design
    ) {
        Column {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically, 
                modifier = Modifier.padding(12.dp)
            ) {
                AsyncImage(
                    model = post.authorPhotoUrl,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp).clip(CircleShape).background(Color.Gray),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = post.authorName.ifBlank { "Unknown User" }, 
                            fontWeight = FontWeight.Bold, 
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                         Text(
                            text = formatTimestampIST(post.createdAt),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                        Text(
                             text = " • ",
                             style = MaterialTheme.typography.bodySmall,
                             color = Color.Gray
                        )
                        RoleBadge(role = post.authorRole.ifBlank { "User" })
                    }
                   
                }
                
                // Connect Button - Top Right
                TextButton(
                    onClick = onConnect,
                    colors = ButtonDefaults.textButtonColors(contentColor = AtmiyaPrimary)
                ) {
                    Text("+ Connect", fontWeight = FontWeight.Bold)
                }

                // Delete Option (Three Dots)
                if (post.authorUserId == currentUserId) {
                    Box {
                        var expanded by remember { mutableStateOf(false) }
                        IconButton(onClick = { expanded = true }) {
                            Icon(TablerIcons.DotsVertical, contentDescription = "More", tint = Color.Gray, modifier = Modifier.size(20.dp))
                        }
                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Delete Post", color = Color.Red) },
                                onClick = {
                                    expanded = false
                                    onDelete()
                                },
                                leadingIcon = {
                                    Icon(TablerIcons.Trash, contentDescription = null, tint = Color.Red, modifier = Modifier.size(20.dp))
                                }
                            )
                        }
                    }
                }
            }
            
            // Content Text
            if (post.postType == "poll") {
                Column(modifier = Modifier.padding(horizontal = 12.dp)) {
                    Text(text = post.pollQuestion ?: "", style = MaterialTheme.typography.bodyLarge)
                    Spacer(modifier = Modifier.height(12.dp))
                    PollSection(post = post, currentUserId = currentUserId, onVote = onVote)
                }
            } else {
                if (post.content.isNotBlank()) {
                    Text(
                        text = getAnnotatedString(post.content), 
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 12.dp)
                    )
                }

                val displayAttachments = remember(post) {
                    if (post.attachments.isNotEmpty()) post.attachments
                    else if (!post.mediaUrl.isNullOrBlank()) {
                         // Import from data package if needed, used fully qualified name in dialog, so I'll construct it here or assume implicit imports
                         listOf(com.atmiya.innovation.data.PostAttachment(
                             id = UUID.randomUUID().toString(),
                             type = post.mediaType,
                             url = post.mediaUrl,
                             thumbnailUrl = post.thumbnailUrl
                         ))
                    } else emptyList()
                }

                var showFullScreenMedia by remember { mutableStateOf(false) }
                var initialMediaIndex by remember { mutableStateOf(0) }
                
                if (showFullScreenMedia) {
                    FullScreenMediaDialog(
                        mediaItems = displayAttachments,
                        initialPage = initialMediaIndex,
                        onDismiss = { showFullScreenMedia = false }
                    )
                }

                // Media - Full Width with correct Aspect Ratio
                if (post.attachments.isNotEmpty()) {
                    val pagerState = rememberPagerState(pageCount = { post.attachments.size })
                    
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f) // Square aspect ratio typically looks good, or 4:5
                            .clip(RoundedCornerShape(0.dp)) // Optional: rounded corners
                    ) {
                        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                            val attachment = post.attachments[page]
                            Box(modifier = Modifier.fillMaxSize().background(Color.Black).clickable { 
                                initialMediaIndex = page
                                showFullScreenMedia = true
                            }) { 
                                if (attachment.type == "video") {
                                    VideoPlayerWithPreview(videoUrl = attachment.url, thumbnailUrl = attachment.thumbnailUrl)
                                } else {
                                    AsyncImage(
                                        model = ImageRequest.Builder(context)
                                            .data(attachment.url)
                                            .crossfade(true)
                                            .build(),
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop 
                                    )
                                }
                            }
                        }
                        
                        // Instagram-style Dots Indicator
                        if (post.attachments.size > 1) {
                            Row(
                                modifier = Modifier
                                    .wrapContentHeight()
                                    .fillMaxWidth()
                                    .align(Alignment.BottomCenter)
                                    .padding(bottom = 16.dp), // More padding from bottom
                                horizontalArrangement = Arrangement.Center
                            ) {
                                repeat(post.attachments.size) { iteration ->
                                    val color = if (pagerState.currentPage == iteration) AtmiyaPrimary else Color.White.copy(alpha = 0.5f)
                                    Box(
                                        modifier = Modifier
                                            .padding(3.dp)
                                            .clip(CircleShape)
                                            .background(color)
                                            .size(6.dp) // Smaller dots
                                    )
                                }
                            }
                        }
                    }

                } else if (post.mediaType == "image" && !post.mediaUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(post.mediaUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 400.dp) // Limit height but allow full width
                            .clickable { 
                                initialMediaIndex = 0
                                showFullScreenMedia = true 
                            },
                        contentScale = ContentScale.Crop // Crop to fill width nicely like FB
                    )

                } else if (post.mediaType == "video" && !post.mediaUrl.isNullOrBlank()) {
                     Box(modifier = Modifier.fillMaxWidth().height(250.dp).clickable {
                           initialMediaIndex = 0
                           showFullScreenMedia = true
                     }) {
                        VideoPlayerWithPreview(videoUrl = post.mediaUrl, thumbnailUrl = post.thumbnailUrl)
                     }
                }
            }
            
            // Stats Row
            Row(
                 modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                 verticalAlignment = Alignment.CenterVertically,
                 horizontalArrangement = Arrangement.SpaceBetween
            ) {
                 if (post.likesCount > 0) {
                     Row(verticalAlignment = Alignment.CenterVertically) {
                         Icon(TablerIcons.ThumbUp, contentDescription = null, tint = AtmiyaPrimary, modifier = Modifier.size(20.dp))
                         Spacer(modifier = Modifier.width(4.dp))
                         Text(text = "${post.likesCount}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                     }
                 } else { Spacer(Modifier.width(1.dp)) }
                 
                 if (post.commentsCount > 0) {
                      Text(text = "${post.commentsCount} comments", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                 }
            }

            Divider(color = Color.LightGray.copy(alpha = 0.2f))

            // Actions Row - Full Width Evenly Spaced
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Like
                Box(
                    modifier = Modifier.weight(1f).clickable { onLike() }.padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            if (isLiked) TablerIcons.ThumbUp else TablerIcons.ThumbUp, // Tabler ThumbUp is outlined by default. If filled needed, check library, else use tint/variation. Assuming outlined for now or same icon.
                            // Tabler doesn't always have "Filled" variant in same object. If 'isLiked', maybe use a different icon or filled version if available. 
                            // For now using same icon, maybe change tint logic which is already handled by 'tint'. 
                            // BETTER: TablerIcons.ThumbUp works. Tint handles active state. 
                            contentDescription = "Like",
                            tint = if (isLiked) AtmiyaPrimary else Color.Gray,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Like",
                            color = if (isLiked) AtmiyaPrimary else Color.Gray,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                
                // Comment
                Box(
                    modifier = Modifier.weight(1f).clickable { showComments = !showComments }.padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                     Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            TablerIcons.MessageCircle, 
                            contentDescription = "Comment",
                            tint = Color.Gray,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "Comment", color = Color.Gray, fontWeight = FontWeight.Medium)
                        if (post.commentsCount > 0) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = "${post.commentsCount}", color = Color.Gray, fontWeight = FontWeight.Medium)
                        }
                    }
                }
                
                // Share
                 Box(
                    modifier = Modifier.weight(1f).clickable { 
                                val sendIntent = android.content.Intent().apply {
                                    action = android.content.Intent.ACTION_SEND
                                    putExtra(android.content.Intent.EXTRA_TEXT, "Check this out on Atmiya Innovation App!\nhttps://netfund.app/wall_post/${post.id}")
                                    type = "text/plain"
                                }
                                val shareIntent = android.content.Intent.createChooser(sendIntent, null)
                        context.startActivity(shareIntent)
                    }.padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                     Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            TablerIcons.Share, 
                            contentDescription = "Share",
                            tint = Color.Gray,
                            modifier = Modifier.size(20.dp)
                        )
                         Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "Share", color = Color.Gray, fontWeight = FontWeight.Medium)
                    }
                }
            }
            
            if (showComments) {
                Divider(color = Color.LightGray.copy(alpha = 0.2f))
                InlineCommentsSection(
                    postId = post.id,
                    repository = repository,
                    currentUserId = currentUserId
                )
            }
        }
    }

}

@Composable
fun FullScreenMediaDialog(
    mediaItems: List<com.atmiya.innovation.data.PostAttachment>,
    initialPage: Int,
    onDismiss: () -> Unit
) {
    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true
        )
    ) {
        val pagerState = rememberPagerState(initialPage = initialPage, pageCount = { mediaItems.size })
        
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                val item = mediaItems[page]
                if (item.type == "video") {
                    val context = LocalContext.current
                    val exoPlayer = remember {
                        ExoPlayer.Builder(context).build().apply {
                            setMediaItem(MediaItem.fromUri(item.url))
                            prepare()
                            playWhenReady = true // Auto-play in fullscreen
                        }
                    }
                    
                    DisposableEffect(Unit) {
                        onDispose { exoPlayer.release() }
                    }
                    
                    AndroidView(
                        factory = { ctx ->
                            PlayerView(ctx).apply {
                                player = exoPlayer
                                useController = true
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    AsyncImage(
                        model = item.url,
                        contentDescription = "Full Screen Image",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                }
            }
            
            // Close Button
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
            ) {
                Icon(TablerIcons.X, contentDescription = "Close", tint = Color.White, modifier = Modifier.size(24.dp))
            }
            
            // Dots Indicator (if multiple)
            if (mediaItems.size > 1) {
                 Row(
                    modifier = Modifier
                        .wrapContentHeight()
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 32.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    repeat(mediaItems.size) { iteration ->
                        val color = if (pagerState.currentPage == iteration) AtmiyaPrimary else Color.White.copy(alpha = 0.5f)
                        Box(
                            modifier = Modifier
                                .padding(4.dp)
                                .clip(CircleShape)
                                .background(color)
                                .size(8.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun VideoPlayer(videoUrl: String) {
    val context = LocalContext.current
    var isFullScreen by remember { mutableStateOf(false) }
    
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(videoUrl))
            prepare()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }

    if (isFullScreen) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { isFullScreen = false },
            properties = androidx.compose.ui.window.DialogProperties(
                usePlatformDefaultWidth = false,
                dismissOnBackPress = true
            )
        ) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            player = exoPlayer
                            useController = true
                            setShowNextButton(false)
                            setShowPreviousButton(false)
                            setControllerOnFullScreenModeChangedListener { 
                                isFullScreen = false 
                            }
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
                // Fallback close button if controller hides
                IconButton(
                    onClick = { isFullScreen = false },
                    modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)
                ) {
                    Icon(TablerIcons.X, contentDescription = "Close Full Screen", tint = Color.White, modifier = Modifier.size(24.dp))
                }
            }
        }
    }

    // Inline Player
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(250.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.Black)
    ) {
        if (!isFullScreen) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = exoPlayer
                        useController = true
                        setShowNextButton(false)
                        setShowPreviousButton(false)
                        setControllerOnFullScreenModeChangedListener { 
                             isFullScreen = true
                        }
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
            // Manual Overlay Button (optional if controller has it, but good for visibility)
            IconButton(
                onClick = { isFullScreen = true },
                modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)
            ) {
                 // Using ZoomOutMap as a proxy for Fullscreen if Fullscreen icon is missing/extended
                 // Or create a custom vector if needed. For now using an available default.
                 // If Icons.Default.Fullscreen is available, use it. If not, fallback.
                 // Checking commonly available: Icons.Default.Add is definitely there.
                 // Let's rely on controller mostly, but keep this as "Expand"
                 Icon(TablerIcons.InfoCircle, contentDescription = "Full Screen", tint = Color.White, modifier = Modifier.size(24.dp)) 
            }
        } else {
            // Placeholder when fullscreen is active
            Box(modifier = Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
                Text("Playing in Full Screen", color = Color.White)
            }
        }
    }
}

// Helper for Hashtags
fun getAnnotatedString(text: String): androidx.compose.ui.text.AnnotatedString {
    return androidx.compose.ui.text.buildAnnotatedString {
        val words = text.split("\\s+".toRegex())
        words.forEach { word ->
            if (word.startsWith("#")) {
                pushStyle(androidx.compose.ui.text.SpanStyle(
                    color = AtmiyaPrimary,
                    fontWeight = FontWeight.Bold
                ))
                append("$word ")
                pop()
            } else {
                append("$word ")
            }
        }
    }
}

// PollSection and InlineCommentsSection remain same, copying them below
@Composable
fun PollSection(
    post: WallPost,
    currentUserId: String,
    onVote: (String) -> Unit
) {
    val repository = remember { FirestoreRepository() }
    var votedOptionId by remember { mutableStateOf<String?>(null) }
    
    LaunchedEffect(post.id, currentUserId) {
        votedOptionId = repository.getPollVote(post.id, currentUserId)
    }
    
    val totalVotes = post.pollOptions.sumOf { it.voteCount }
    
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        post.pollOptions.forEach { option ->
            val isSelected = option.id == votedOptionId
            val percentage = if (totalVotes > 0) (option.voteCount.toFloat() / totalVotes) else 0f
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    .clickable(enabled = votedOptionId == null) {
                        if (votedOptionId == null) {
                            onVote(option.id)
                            votedOptionId = option.id 
                        }
                    }
            ) {
                if (votedOptionId != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(percentage)
                            .background(if (isSelected) AtmiyaPrimary.copy(alpha = 0.2f) else Color.Gray.copy(alpha = 0.1f))
                    )
                }
                
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = option.text,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) AtmiyaPrimary else MaterialTheme.colorScheme.onSurface
                    )
                    
                    if (votedOptionId != null) {
                        Text(
                            text = "${(percentage * 100).toInt()}%",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
        
        if (totalVotes > 0) {
            Text(
                text = "$totalVotes votes",
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray,
                modifier = Modifier.align(Alignment.End)
            )
        }
    }
}

@Composable
fun InlineCommentsSection(
    postId: String,
    repository: FirestoreRepository,
    currentUserId: String
) {
    val comments by repository.getComments(postId).collectAsState(initial = emptyList())
    var newCommentText by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var isPosting by remember { mutableStateOf(false) }
    val auth = FirebaseAuth.getInstance()
    val userPhoto = auth.currentUser?.photoUrl

    Column(modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 8.dp)) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Comments (${comments.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        // Comment Input
        // Fix: Add weight(1f) to text field to prevent overflow off screen
         Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp), // Adjusted padding to match the original input section's visual spacing
            verticalAlignment = Alignment.CenterVertically
        ) {
            // User Avatar (Small)
            if (userPhoto != null) {
                AsyncImage(
                    model = userPhoto,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp).clip(CircleShape).background(Color.LightGray),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier.size(40.dp).clip(CircleShape).background(Color.LightGray),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(TablerIcons.User, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                }
            }
            
            Spacer(modifier = Modifier.width(12.dp)) // Adjusted width to match original

            // Input Field
            Row(
                modifier = Modifier
                    .weight(1f) // CRITICAL FIX: Ensure it takes only available space
                    .clip(RoundedCornerShape(12.dp)) // Adjusted to match original shape
                    .background(Color(0xFFF5F5F5)) // Adjusted to match original color
                    .padding(horizontal = 16.dp, vertical = 12.dp), // Adjusted to match original padding
                verticalAlignment = Alignment.CenterVertically
            ) {
                androidx.compose.foundation.text.BasicTextField(
                    value = newCommentText,
                    onValueChange = { newCommentText = it },
                    modifier = Modifier.weight(1f), // Grow inside the container
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = Color.Black), // Adjusted to match original
                    decorationBox = { innerTextField ->
                        if (newCommentText.isEmpty()) {
                            Text("Write your comments here...", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                        }
                        innerTextField()
                    }
                )
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            // Send Button
            if (isPosting) {
                 CircularProgressIndicator(modifier = Modifier.size(32.dp), strokeWidth = 2.dp) // Adjusted size
            } else {
                 IconButton(
                    onClick = {
                        if (newCommentText.isNotBlank()) {
                            isPosting = true
                            val comment = Comment(
                                id = UUID.randomUUID().toString(),
                                authorUserId = currentUserId,
                                authorName = auth.currentUser?.displayName ?: "User",
                                authorPhotoUrl = auth.currentUser?.photoUrl?.toString(),
                                text = newCommentText,
                                createdAt = Timestamp.now()
                            )
                            scope.launch {
                                repository.addComment(postId, comment)
                                newCommentText = ""
                                isPosting = false
                            }
                        }
                    },
                    enabled = newCommentText.isNotBlank(),
                    modifier = Modifier
                        .size(32.dp)
                        .background(AtmiyaPrimary, CircleShape)
                ) {
                    Icon(TablerIcons.Send, contentDescription = "Send", tint = Color.White, modifier = Modifier.size(20.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Comments List
        // Show 5 newest comments (assuming getComments returns oldest first, we take last 5 and reverse)
        val displayComments = comments.takeLast(5).reversed()
        
        displayComments.forEach { comment ->
            Row(
                modifier = Modifier.padding(vertical = 12.dp),
                verticalAlignment = Alignment.Top
            ) {
                AsyncImage(
                    model = comment.authorPhotoUrl,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp).clip(CircleShape).background(Color.Gray),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            comment.authorName.ifBlank { "Unknown User" }, 
                            fontWeight = FontWeight.Bold, 
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            formatTimestampIST(comment.createdAt), 
                            style = MaterialTheme.typography.labelSmall, 
                            color = Color.Gray
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(comment.text, style = MaterialTheme.typography.bodyMedium)
                    
                    // Actions
                    Row(modifier = Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(TablerIcons.Heart, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.Gray)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Like", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        
                        Spacer(modifier = Modifier.width(16.dp))
                        
                        // Reply Icon (using generic if Reply not found, but usually it is)
                        // Using AutoMirrored.Filled.Reply if available, or just a text for safety if icon is missing
                        // Let's try standard Reply
                        Text("Reply", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    }
                }
            }
        }
        
        if (comments.size > 5) {
            Text(
                "View all ${comments.size} comments", 
                style = MaterialTheme.typography.labelSmall, 
                color = AtmiyaPrimary,
                modifier = Modifier.padding(vertical = 8.dp).clickable { }
            )
        }
    }
}

@Composable
fun VideoPlayerWithPreview(videoUrl: String, thumbnailUrl: String?) {
    var isPlaying by remember { mutableStateOf(false) }

    if (isPlaying) {
        VideoPlayer(videoUrl = videoUrl)
    } else {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.Black)
                .clickable { isPlaying = true },
            contentAlignment = Alignment.Center
        ) {
            if (!thumbnailUrl.isNullOrBlank()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(thumbnailUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            // Play Icon
            Icon(
                imageVector = TablerIcons.PlayerPlay,
                contentDescription = "Play",
                tint = Color.White,
                modifier = Modifier.size(64.dp)
            )
        }
    }
}
