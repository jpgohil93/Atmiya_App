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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Comment
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Send
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
import coil.compose.AsyncImage
import com.atmiya.innovation.data.Comment
import com.atmiya.innovation.data.WallPost
import com.atmiya.innovation.repository.FirestoreRepository
import com.atmiya.innovation.repository.StorageRepository
import com.atmiya.innovation.ui.components.*
import com.atmiya.innovation.ui.theme.AtmiyaPrimary
import com.atmiya.innovation.ui.theme.AtmiyaSecondary
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WallScreen(
    viewModel: WallViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val posts by viewModel.posts.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val filterType by viewModel.filterType.collectAsState()
    val selectedSector by viewModel.selectedSector.collectAsState()
    
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val scope = rememberCoroutineScope()
    
    val sectors = listOf("All", "Tech", "Fintech", "Healthcare", "EdTech", "AgriTech", "CleanTech")
    
    var showCreateDialog by remember { mutableStateOf(false) }

    SoftScaffold(
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Header & Filters
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Community Wall",
                        style = MaterialTheme.typography.headlineMedium,
                        color = AtmiyaPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    // Refresh Button
                    IconButton(onClick = { 
                        viewModel.refresh()
                        Toast.makeText(context, "Refreshing...", Toast.LENGTH_SHORT).show()
                    }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                
                // Filter Tabs
                Row(modifier = Modifier.fillMaxWidth()) {
                    FilterChip(
                        selected = filterType == "all",
                        onClick = { viewModel.setFilter("all") },
                        label = { Text("All Posts") }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    FilterChip(
                        selected = filterType == "funding_call",
                        onClick = { viewModel.setFilter("funding_call") },
                        label = { Text("Funding Calls") }
                    )
                }
                
                // Sector Chips (Visible only for Funding Calls)
                if (filterType == "funding_call") {
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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

            // Posts List
            if (isLoading && posts.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = AtmiyaPrimary)
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    if (posts.isEmpty()) {
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
                                onVote = { optionId -> viewModel.voteOnPoll(post, optionId) }
                            )
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
    var pollOptions by remember { mutableStateOf(listOf("", "")) } // Start with 2 empty options

    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val firestoreRepository = remember { FirestoreRepository() }
    var isAdmin by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val user = firestoreRepository.getUser(auth.currentUser?.uid ?: "")
        isAdmin = user?.role == "admin"
    }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            selectedUri = uri
            isVideo = false
            isPollMode = false // Disable poll if media selected
        }
    }
    
    val videoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            selectedUri = uri
            isVideo = true
            isPollMode = false // Disable poll if media selected
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
                    
                    // Media Preview
                    if (selectedUri != null) {
                        Box(modifier = Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(12.dp))) {
                            if (isVideo) {
                                // Video Placeholder
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
                            // Remove Button
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
                    // Poll UI
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

                // Media Buttons (Only if not poll mode)
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
                
                // Poll Toggle (Admin Only)
                if (isAdmin) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { 
                        isPollMode = !isPollMode 
                        if (isPollMode) selectedUri = null // Clear media if switching to poll
                    }) {
                        Checkbox(checked = isPollMode, onCheckedChange = { 
                            isPollMode = it 
                            if (it) selectedUri = null
                        })
                        Text("Create a Poll", fontWeight = FontWeight.Bold)
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

// Helper for IST Timestamp
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
    onVote: (String) -> Unit
) {
    val repository = remember { FirestoreRepository() }
    val context = LocalContext.current
    
    // Real-time Like Status
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

    // Inline Comments State
    var showComments by rememberSaveable { mutableStateOf(false) }

    SoftCard(modifier = Modifier.fillMaxWidth()) {
        Column {
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
                    Text(text = post.authorName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = "${post.authorRole} • ${formatTimestampIST(post.createdAt)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
                if (post.postType == "funding_call" && post.sector != null) {
                    Spacer(modifier = Modifier.weight(1f))
                    Surface(
                        color = AtmiyaSecondary.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = post.sector,
                            color = AtmiyaSecondary,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            
            // Content (or Poll Question)
            if (post.postType == "poll") {
                Text(text = post.pollQuestion ?: "", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(12.dp))
                
                PollSection(post = post, currentUserId = currentUserId, onVote = onVote)
                
            } else {
                if (post.content.isNotBlank()) {
                    Text(text = post.content, style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Media (Dynamic Size)
                if (post.mediaType == "image" && post.mediaUrl != null) {
                    AsyncImage(
                        model = post.mediaUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { 
                                Toast.makeText(context, "Opening Image...", Toast.LENGTH_SHORT).show()
                            },
                        contentScale = ContentScale.FillWidth
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                } else if (post.mediaType == "video" && post.mediaUrl != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(250.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.Black)
                            .clickable {
                                Toast.makeText(context, "Playing Video...", Toast.LENGTH_SHORT).show()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.PlayCircle, contentDescription = "Play Video", tint = Color.White, modifier = Modifier.size(48.dp))
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
            Spacer(modifier = Modifier.height(8.dp))

            // Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(onClick = onLike) {
                    Icon(
                        if (isLiked) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = "Like",
                        tint = if (isLiked) Color.Red else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("${post.likesCount}", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                }
                
                TextButton(onClick = { showComments = !showComments }) {
                    Icon(Icons.Outlined.Comment, contentDescription = "Comment", tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("${post.commentsCount}", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                }
            }
            
            // Inline Comments Section
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
fun PollSection(
    post: WallPost,
    currentUserId: String,
    onVote: (String) -> Unit
) {
    val repository = remember { FirestoreRepository() }
    var votedOptionId by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    
    LaunchedEffect(post.id, currentUserId) {
        votedOptionId = repository.getPollVote(post.id, currentUserId)
        isLoading = false
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
                            votedOptionId = option.id // Optimistic update
                        }
                    }
            ) {
                // Progress Bar
                if (votedOptionId != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(percentage)
                            .background(if (isSelected) AtmiyaPrimary.copy(alpha = 0.2f) else Color.Gray.copy(alpha = 0.1f))
                    )
                }
                
                // Content
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

    Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
        // List (Limit to last 3 for preview, or scrollable if needed)
        val displayComments = comments.takeLast(5)
        
        displayComments.forEach { comment ->
            Row(
                modifier = Modifier.padding(vertical = 4.dp),
                verticalAlignment = Alignment.Top
            ) {
                AsyncImage(
                    model = comment.authorPhotoUrl,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp).clip(CircleShape).background(Color.Gray),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        .padding(8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(comment.authorName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(formatTimestampIST(comment.createdAt), style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    }
                    Text(comment.text, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        
        if (comments.size > 5) {
            Text(
                "View all ${comments.size} comments", 
                style = MaterialTheme.typography.labelSmall, 
                color = AtmiyaPrimary,
                modifier = Modifier.padding(vertical = 4.dp).clickable { /* Expand? */ }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Input
        Row(verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = null, // TODO: Current User Photo
                contentDescription = null,
                modifier = Modifier.size(32.dp).clip(CircleShape).background(Color.Gray),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(8.dp))
            
            OutlinedTextField(
                value = newComment,
                onValueChange = { newComment = it },
                placeholder = { Text("Write a comment...", style = MaterialTheme.typography.bodyMedium, color = Color.Gray) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = Color.Transparent,
                    focusedBorderColor = Color.Transparent,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                ),
                maxLines = 3,
                trailingIcon = {
                    if (newComment.isNotBlank()) {
                        IconButton(
                            onClick = {
                                if (!isPosting) {
                                    isPosting = true
                                    scope.launch {
                                        try {
                                            val userProfile = repository.getUser(currentUserId)
                                            val comment = Comment(
                                                id = UUID.randomUUID().toString(),
                                                authorUserId = currentUserId,
                                                authorName = userProfile?.name ?: "Anonymous",
                                                authorRole = userProfile?.role ?: "User",
                                                authorPhotoUrl = userProfile?.profilePhotoUrl,
                                                text = newComment.trim(),
                                                createdAt = Timestamp.now()
                                            )
                                            repository.addComment(postId, comment)
                                            newComment = ""
                                        } catch (e: Exception) {
                                            android.util.Log.e("Comments", "Error adding comment", e)
                                            Toast.makeText(context, "Failed: ${e.message}", Toast.LENGTH_LONG).show()
                                        } finally {
                                            isPosting = false
                                        }
                                    }
                                }
                            }
                        ) {
                            if (isPosting) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp, color = AtmiyaPrimary)
                            } else {
                                Icon(Icons.Default.Send, contentDescription = "Send", tint = AtmiyaPrimary)
                            }
                        }
                    }
                }
            )
        }
    }
}

