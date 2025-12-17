package com.atmiya.innovation.ui.dashboard

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import compose.icons.TablerIcons
import compose.icons.tablericons.X
import compose.icons.tablericons.Photo
import compose.icons.tablericons.List
import compose.icons.tablericons.PlayerPlay
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import android.widget.Toast
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.atmiya.innovation.repository.FirestoreRepository
import com.atmiya.innovation.ui.theme.AtmiyaPrimary
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePostScreen(
    onDismiss: () -> Unit,
    onPost: (String, List<Pair<Uri, Boolean>>, String?, List<String>) -> Unit
) {
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val repository = remember { FirestoreRepository() }
    
    var text by remember { mutableStateOf("") }
    var selectedMediaItems by remember { mutableStateOf<List<Pair<Uri, Boolean>>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) } // This was missing in the state tracking
    
    // Poll State
    var isPollMode by remember { mutableStateOf(false) }
    var pollQuestion by remember { mutableStateOf("") }
    var pollOptions by remember { mutableStateOf(listOf("", "")) }

    var userName by remember { mutableStateOf("User") }
    var userPhotoUrl by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        val user = auth.currentUser
        if (user != null) {
            val userProfile = repository.getUser(user.uid)
            userName = userProfile?.name ?: user.displayName ?: "User"
            userPhotoUrl = userProfile?.profilePhotoUrl ?: user.photoUrl?.toString() ?: ""
        }
    }

    // Calculate remaining slots for media selection
    val remainingSlots = (10 - selectedMediaItems.size).coerceAtLeast(0)
    
    // Use a constant max limit for the launcher to avoid re-registration issues
    val imagePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(10)
    ) { uris ->
        if (uris.isNotEmpty()) {
            val remaining = (10 - selectedMediaItems.size).coerceAtLeast(0)
            val newItems = uris.take(remaining).map { it to false }
            selectedMediaItems = selectedMediaItems + newItems
            if (uris.size > remaining) {
                Toast.makeText(context, "Max 10 items allowed", Toast.LENGTH_SHORT).show()
            }
            isPollMode = false
        }
    }
    
    val videoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(10)
    ) { uris ->
        if (uris.isNotEmpty()) {
            val remaining = (10 - selectedMediaItems.size).coerceAtLeast(0)
            val newItems = uris.take(remaining).map { it to true }
            selectedMediaItems = selectedMediaItems + newItems
             if (uris.size > remaining) {
                Toast.makeText(context, "Max 10 items allowed", Toast.LENGTH_SHORT).show()
            }
            isPollMode = false
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding() // Avoid system navigation bar
                .imePadding(), // Adjust for keyboard
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .fillMaxHeight(0.85f) // Reduced size (vertical)
                    .clip(RoundedCornerShape(28.dp)),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp
            ) {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        TopAppBar(
                            title = { Text(if (isPollMode) "Create Poll" else "Create Post", fontWeight = FontWeight.SemiBold) },
                            navigationIcon = {
                                IconButton(onClick = onDismiss) {
                                    Icon(TablerIcons.X, contentDescription = "Close", modifier = Modifier.size(24.dp))
                                }
                            },
                            actions = {
                                Button(
                                    onClick = { 
                                        if (isPollMode) {
                                            if (pollQuestion.isBlank() || pollOptions.any { it.isBlank() } || pollOptions.size < 2) return@Button
                                             isLoading = true
                                            onPost("", emptyList(), pollQuestion, pollOptions)
                                        } else {
                                            if (text.isBlank() && selectedMediaItems.isEmpty()) return@Button
                                             isLoading = true
                                            onPost(text, selectedMediaItems, null, emptyList())
                                        }
                                    },
                                    enabled = !isLoading && (if (isPollMode) pollQuestion.isNotBlank() && pollOptions.size >= 2 else (text.isNotBlank() || selectedMediaItems.isNotEmpty())),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                    modifier = Modifier.padding(end = 8.dp)
                                ) {
                                    Text(if (isLoading) "Posting" else "Post")
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
                        )
                    },
                    bottomBar = {
                        if (!isPollMode) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surface)
                            ) {
                                HorizontalDivider()
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text("Add to your post", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                        if (selectedMediaItems.isNotEmpty()) {
                                            Text(
                                                "${selectedMediaItems.size}/10 items",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (selectedMediaItems.size >= 10) Color(0xFFE91E63) else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        IconButton(
                                            onClick = { 
                                                if (selectedMediaItems.size < 10) {
                                                    imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                                }
                                            },
                                            enabled = selectedMediaItems.size < 10
                                        ) {
                                            Icon(
                                                TablerIcons.Photo, 
                                                contentDescription = "Photo", 
                                                tint = if (selectedMediaItems.size < 10) Color(0xFF4CAF50) else Color.Gray,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                        IconButton(
                                            onClick = { 
                                                if (selectedMediaItems.size < 10) {
                                                    videoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))
                                                }
                                            },
                                            enabled = selectedMediaItems.size < 10
                                        ) {
                                            Icon(
                                                TablerIcons.PlayerPlay, 
                                                contentDescription = "Video", 
                                                tint = if (selectedMediaItems.size < 10) Color(0xFFE91E63) else Color.Gray,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                        IconButton(onClick = { isPollMode = true; selectedMediaItems = emptyList() }) {
                                            Icon(TablerIcons.List, contentDescription = "Poll", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                                        }
                                    }
                                }
                            }
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.surface
                ) { padding ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        // User Info
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AsyncImage(
                                model = userPhotoUrl,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp).clip(CircleShape).background(MaterialTheme.colorScheme.onSurfaceVariant),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(userName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                                    modifier = Modifier.padding(top = 2.dp)
                                ) {
                                    Text(
                                        "Public", 
                                        style = MaterialTheme.typography.labelSmall, 
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        if (isPollMode) {
                            OutlinedTextField(
                                value = pollQuestion,
                                onValueChange = { pollQuestion = it },
                                placeholder = { Text("Ask a question...", fontSize = 20.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    unfocusedBorderColor = Color.Transparent,
                                    focusedBorderColor = Color.Transparent
                                ),
                                textStyle = MaterialTheme.typography.headlineSmall
                            )
                             pollOptions.forEachIndexed { index, option ->
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
                                        OutlinedTextField(
                                            value = option,
                                            onValueChange = { newText -> 
                                                pollOptions = pollOptions.toMutableList().apply { set(index, newText) }
                                            },
                                            placeholder = { Text("Option ${index + 1}") },
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        IconButton(onClick = { 
                                            if (pollOptions.size > 2) pollOptions = pollOptions.toMutableList().apply { removeAt(index) }
                                        }) {
                                            Icon(TablerIcons.X, contentDescription = null, modifier = Modifier.size(20.dp))
                                        }
                                    }
                            }
                            if (pollOptions.size < 5) {
                                TextButton(onClick = { pollOptions = pollOptions + "" }) {
                                    Text("+ Add Option")
                                }
                            }

                        } else {
                            // Text Area
                            TextField(
                                value = text,
                                onValueChange = { text = it },
                                placeholder = { Text("What's on your mind?", fontSize = 24.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }, // Larger placeholder
                                modifier = Modifier.fillMaxWidth(),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent
                                ),
                                textStyle = MaterialTheme.typography.headlineSmall
                            )
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            // Media Preview
                            if (selectedMediaItems.isNotEmpty()) {
                                // Grid layout logic or just a column of large images
                                 selectedMediaItems.forEachIndexed { index, (uri, isVideo) ->
                                     Box(modifier = Modifier.padding(vertical = 4.dp).clip(RoundedCornerShape(12.dp))) {
                                        AsyncImage(
                                            model = uri,
                                            contentDescription = null,
                                            modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp),
                                            contentScale = ContentScale.Crop
                                        )
                                        if (isVideo) {
                                             Box(modifier = Modifier.matchParentSize().background(Color.Black.copy(alpha=0.3f)), contentAlignment = Alignment.Center) {
                                                Icon(TablerIcons.PlayerPlay, contentDescription = null, tint = Color.White, modifier = Modifier.size(48.dp))
                                            }
                                        }
                                        IconButton(
                                            onClick = { selectedMediaItems = selectedMediaItems.toMutableList().apply { removeAt(index) } },
                                            modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).background(Color.Black.copy(alpha=0.6f), CircleShape)
                                        ) {
                                            Icon(TablerIcons.X, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                        }
                                     }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
