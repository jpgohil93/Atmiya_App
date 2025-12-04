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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material.icons.outlined.Comment
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.ui.text.withStyle
import androidx.compose.material3.*
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
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    // Netfund Logo
                    AsyncImage(
                        model = com.atmiya.innovation.R.drawable.netfund_logo,
                        contentDescription = "Netfund",
                        modifier = Modifier.height(60.dp), // Increased size
                        contentScale = ContentScale.Fit
                    )
                },
                navigationIcon = {
                    // Custom Stylish Burger Menu
                    IconButton(
                        onClick = onOpenDrawer,
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .size(40.dp)
                    ) {
                        Column(
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(modifier = Modifier.width(20.dp).height(2.dp).background(AtmiyaPrimary, RoundedCornerShape(1.dp)))
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(modifier = Modifier.width(14.dp).height(2.dp).background(AtmiyaPrimary, RoundedCornerShape(1.dp))) // Asymmetric look
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(modifier = Modifier.width(20.dp).height(2.dp).background(AtmiyaPrimary, RoundedCornerShape(1.dp)))
                        }
                    }
                },
                actions = {
                    // Larger Profile Photo
                    Box(
                        modifier = Modifier
                            .padding(end = 16.dp)
                            .size(44.dp) // Bigger size
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable { onNavigateToProfile() },
                        contentAlignment = Alignment.Center
                    ) {
                        if (userPhotoUrl.isNotBlank()) {
                            AsyncImage(
                                model = userPhotoUrl,
                                contentDescription = "Profile",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = "Profile",
                                tint = AtmiyaPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateDialog = true },
                containerColor = AtmiyaPrimary,
                contentColor = Color.White,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = "Create Post")
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
                Column(modifier = Modifier.fillMaxSize()) {
                    // Header & Filters & Input
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(bottom = 8.dp)
                    ) {
                        // "Post your Activity" Input Bar
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .height(56.dp)
                                .clip(RoundedCornerShape(16.dp)) // Less radius as requested
                                .clickable { showCreateDialog = true },
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Post your Activity",
                                    color = Color.Gray,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.weight(1f)
                                )
                                Icon(
                                    Icons.Default.AttachFile,
                                    contentDescription = null,
                                    tint = Color.Gray,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Icon(
                                    Icons.Default.Image,
                                    contentDescription = null,
                                    tint = Color.Gray,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Filter Tabs
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = 16.dp)
                        ) {
                            FilterChip(
                                selected = filterType == "all",
                                onClick = { viewModel.setFilter("all") },
                                label = { Text("News feed") } // Renamed to match reference vibe
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            FilterChip(
                                selected = filterType == "funding_call",
                                onClick = { viewModel.setFilter("funding_call") },
                                label = { Text("Funding Calls") }
                            )
                            // Add more placeholder filters if needed to match "Top Traders", "Following"
                            Spacer(modifier = Modifier.width(8.dp))
                            FilterChip(
                                selected = filterType == "connections",
                                onClick = { viewModel.setFilter("connections") },
                                label = { Text("Connections") }
                            )
                        }
                        
                        // Sector Chips (Visible only for Funding Calls)
                        if (filterType == "funding_call") {
                            Spacer(modifier = Modifier.height(8.dp))
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(sectors) { sector ->
                                    FilterChip(
                                        selected = selectedSector == sector,
                                        onClick = { viewModel.setSector(sector) },
                                        label = { Text(sector) }
                                    )
                                }
                            }
                        }
                    }

                    // Content Area
                    if (filterType == "connections") {
                        val connections by viewModel.connections.collectAsState()
                        ConnectionsList(connections = connections, onChatClick = { /* TODO: Navigate to Chat */ })
                    } else {
                        // Posts List
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            contentPadding = PaddingValues(top = 8.dp, bottom = 100.dp)
                        ) {
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
                                        }
                                    )
                                    
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
                }
            }
            
            // Create Post Dialog
            if (showCreateDialog) {
                CreatePostDialog(
                    onDismiss = { showCreateDialog = false },
                    onPost = { content, uri, isVideo, pollQuestion, pollOptions ->
                        viewModel.createPost(context, content, uri, isVideo, pollQuestion, pollOptions)
                        showCreateDialog = false
                        Toast.makeText(context, "Posting...", Toast.LENGTH_SHORT).show()
                    }
                )
            }
    }
}

@Composable
fun ConnectionsList(
    connections: List<com.atmiya.innovation.data.User>,
    onChatClick: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 100.dp)
    ) {
        items(connections) { user ->
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = Color.White,
                shadowElevation = 1.dp
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AsyncImage(
                        model = user.profilePhotoUrl,
                        contentDescription = null,
                        modifier = Modifier.size(50.dp).clip(CircleShape).background(Color.Gray),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = user.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        RoleBadge(role = user.role)
                    }
                    IconButton(
                        onClick = { onChatClick(user.uid) },
                        modifier = Modifier.background(AtmiyaPrimary.copy(alpha = 0.1f), CircleShape)
                    ) {
                        Icon(Icons.Default.Chat, contentDescription = "Chat", tint = AtmiyaPrimary)
                    }
                }
            }
        }
    }
}

// ... (CreatePostDialog and formatTimestampIST remain same, copying them below for completeness)

@Composable
fun CreatePostDialog(
    onDismiss: () -> Unit, 
    onPost: (String, Uri?, Boolean, String?, List<String>) -> Unit
) {
    var text by remember { mutableStateOf("") }
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var isVideo by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    
    // Poll State
    var isPollMode by remember { mutableStateOf(false) }
    var pollQuestion by remember { mutableStateOf("") }
    var pollOptions by remember { mutableStateOf(listOf("", "")) } 

    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val firestoreRepository = remember { FirestoreRepository() }
    // Removed isAdmin check as requested

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            selectedUri = uri
            isVideo = false
            isPollMode = false 
        }
    }
    
    val videoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            selectedUri = uri
            isVideo = true
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
                    
                    if (selectedUri != null) {
                        Box(modifier = Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(12.dp))) {
                            if (isVideo) {
                                Box(modifier = Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.PlayCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(48.dp))
                                    Text("Video Selected", color = Color.White, modifier = Modifier.padding(top = 64.dp))
                                }
                            } else {
                                AsyncImage(
                                    model = selectedUri,
                                    contentDescription = "Selected Image",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }
                            IconButton(
                                onClick = { selectedUri = null },
                                modifier = Modifier.align(Alignment.TopEnd).background(Color.Black.copy(alpha = 0.5f), CircleShape)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Remove", tint = Color.White)
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                } else {
                    OutlinedTextField(
                        value = pollQuestion,
                        onValueChange = { pollQuestion = it },
                        label = { Text("Ask a question...") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    pollOptions.forEachIndexed { index, option ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = option,
                                onValueChange = { newText -> 
                                    pollOptions = pollOptions.toMutableList().apply { set(index, newText) }
                                },
                                label = { Text("Option ${index + 1}") },
                                modifier = Modifier.weight(1f)
                            )
                            if (pollOptions.size > 2) {
                                IconButton(onClick = { 
                                    pollOptions = pollOptions.toMutableList().apply { removeAt(index) }
                                }) {
                                    Icon(Icons.Default.Close, contentDescription = "Remove Option")
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    
                    if (pollOptions.size < 5) {
                        TextButton(onClick = { pollOptions = pollOptions + "" }) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Text("Add Option")
                        }
                    }
                }

                if (!isPollMode) {
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        OutlinedButton(onClick = { imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) {
                            Icon(Icons.Default.Image, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Photo")
                        }
                        OutlinedButton(onClick = { videoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)) }) {
                            Icon(Icons.Default.VideoLibrary, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Video")
                        }
                    }
                    Text("Max size: Image 10MB, Video 50MB", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), modifier = Modifier.padding(top = 8.dp))
                }
                
                // Always show Poll option
                Spacer(modifier = Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { 
                    isPollMode = !isPollMode 
                    if (isPollMode) selectedUri = null 
                }) {
                    Checkbox(checked = isPollMode, onCheckedChange = { 
                        isPollMode = it 
                        if (it) selectedUri = null
                    })
                    Text("Create a Poll", fontWeight = FontWeight.Bold)
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
                        onPost("", null, false, pollQuestion, pollOptions)
                    } else {
                        if (text.isBlank() && selectedUri == null) {
                            Toast.makeText(context, "Please add text or media", Toast.LENGTH_SHORT).show()
                            return@SoftButton
                        }
                        isLoading = true
                        onPost(text, selectedUri, isVideo, null, emptyList()) 
                    }
                },
                text = if (isLoading) "Posting..." else "Post",
                icon = Icons.Default.Send,
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
    onConnect: () -> Unit
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

    // Card Design matching reference: White, 16dp radius, clean shadow
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp), // Less radius as requested
        color = Color.White,
        shadowElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(
                    model = post.authorPhotoUrl,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp).clip(CircleShape).background(Color.Gray),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = post.authorName.ifBlank { "Unknown User" }, 
                            fontWeight = FontWeight.Bold, 
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        RoleBadge(role = post.authorRole.ifBlank { "User" })
                    }
                    Text(
                        text = formatTimestampIST(post.createdAt),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
                
                Spacer(modifier = Modifier.weight(1f))
                
                // Connect Button (Small & Clean)
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = AtmiyaPrimary.copy(alpha = 0.1f),
                    modifier = Modifier.clickable { onConnect() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Outlined.PersonAdd, contentDescription = null, tint = AtmiyaPrimary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Connect", color = AtmiyaPrimary, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            
            // Content
            if (post.postType == "poll") {
                Text(text = post.pollQuestion ?: "", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(12.dp))
                PollSection(post = post, currentUserId = currentUserId, onVote = onVote)
            } else {
                if (post.content.isNotBlank()) {
                    Text(
                        text = getAnnotatedString(post.content), 
                        style = MaterialTheme.typography.bodyMedium,
                        lineHeight = 20.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Media
                if (post.mediaType == "image" && !post.mediaUrl.isNullOrBlank()) {
                    var showFullScreenImage by remember { mutableStateOf(false) }
                    
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(post.mediaUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { showFullScreenImage = true },
                        contentScale = ContentScale.FillWidth
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    if (showFullScreenImage) {
                        FullScreenImageDialog(imageUrl = post.mediaUrl, onDismiss = { showFullScreenImage = false })
                    }
                } else if (post.mediaType == "video" && !post.mediaUrl.isNullOrBlank()) {
                    VideoPlayerWithPreview(videoUrl = post.mediaUrl, thumbnailUrl = post.thumbnailUrl)
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Actions Row (Matching reference: Comments pill, Like icon)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // "Comments here" Pill
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.clickable { showComments = !showComments }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(
                            model = auth.currentUser?.photoUrl, // Current user avatar
                            contentDescription = null,
                            modifier = Modifier.size(20.dp).clip(CircleShape).background(Color.Gray),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (post.commentsCount > 0) "View ${post.commentsCount} comments" else "Comments here",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                }

                // Like & Share
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onLike) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                if (isLiked) Icons.Filled.ThumbUp else Icons.Outlined.ThumbUp, // Changed to ThumbUp
                                contentDescription = "Like",
                                tint = if (isLiked) AtmiyaPrimary else Color.Gray, // Primary color for like
                                modifier = Modifier.size(20.dp)
                            )
                            if (post.likesCount > 0) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "${post.likesCount}", 
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.Gray
                                )
                            }
                        }
                    }
                    IconButton(onClick = { 
                        val sendIntent: android.content.Intent = android.content.Intent().apply {
                            action = android.content.Intent.ACTION_SEND
                            putExtra(android.content.Intent.EXTRA_TEXT, "${post.content}\n\nCheck this out on Atmiya Innovation App!")
                            type = "text/plain"
                        }
                        val shareIntent = android.content.Intent.createChooser(sendIntent, null)
                        context.startActivity(shareIntent)
                    }) {
                        Icon(
                            Icons.Default.Share,
                            contentDescription = "Share",
                            tint = Color.Gray,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
            
            if (showComments) {
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
fun FullScreenImageDialog(imageUrl: String, onDismiss: () -> Unit) {
    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            AsyncImage(
                model = imageUrl,
                contentDescription = "Full Screen Image",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
            ) {
                Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
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
                    factory = {
                        PlayerView(context).apply {
                            player = exoPlayer
                            useController = true
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
                IconButton(
                    onClick = { isFullScreen = false },
                    modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close Full Screen", tint = Color.White)
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(250.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.Black)
    ) {
        AndroidView(
            factory = {
                PlayerView(context).apply {
                    player = exoPlayer
                    useController = true
                }
            },
            modifier = Modifier.fillMaxSize()
        )
        // Full Screen Button Overlay
        IconButton(
            onClick = { isFullScreen = true },
            modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)
        ) {
            Icon(Icons.Default.Fullscreen, contentDescription = "Full Screen", tint = Color.White)
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
    var newComment by remember { mutableStateOf("") }
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

        // Input Section (Top)
        Row(verticalAlignment = Alignment.Top) {
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
                    Icon(Icons.Default.Person, contentDescription = null, tint = Color.White)
                }
            }
            Spacer(modifier = Modifier.width(12.dp))

            // Custom Input Box
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(Color(0xFFF5F5F5), RoundedCornerShape(12.dp))
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                if (newComment.isEmpty()) {
                    Text(
                        "Write your comments here...",
                        color = Color.Gray,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                
                androidx.compose.foundation.text.BasicTextField(
                    value = newComment,
                    onValueChange = { newComment = it },
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = Color.Black),
                    modifier = Modifier.fillMaxWidth().padding(end = 40.dp)
                )

                if (newComment.isNotBlank()) {
                    IconButton(
                        onClick = {
                            if (newComment.isNotBlank()) {
                                isPosting = true
                                val comment = Comment(
                                    id = UUID.randomUUID().toString(),
                                    authorUserId = currentUserId,
                                    authorName = auth.currentUser?.displayName ?: "User",
                                    authorPhotoUrl = auth.currentUser?.photoUrl?.toString(),
                                    text = newComment,
                                    createdAt = Timestamp.now()
                                )
                                scope.launch {
                                    repository.addComment(postId, comment)
                                    newComment = ""
                                    isPosting = false
                                }
                            }
                        },
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .size(32.dp)
                            .background(AtmiyaPrimary, CircleShape)
                    ) {
                        Icon(
                            Icons.Default.Send, 
                            contentDescription = "Send", 
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
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
                        Icon(Icons.Default.ThumbUp, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.Gray)
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
                imageVector = Icons.Default.PlayCircle,
                contentDescription = "Play",
                tint = Color.White,
                modifier = Modifier.size(64.dp)
            )
        }
    }
}
