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

    val maxItems = 10 
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(maxItems)) { uris ->
        if (uris.isNotEmpty()) {
            selectedMediaItems = selectedMediaItems + uris.map { it to false }
            isPollMode = false 
        }
    }
    
    val videoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(maxItems)) { uris ->
        if (uris.isNotEmpty()) {
            selectedMediaItems = selectedMediaItems + uris.map { it to true }
            isPollMode = false 
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Scaffold(
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
                            colors = ButtonDefaults.buttonColors(containerColor = AtmiyaPrimary),
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Text(if (isLoading) "Posting" else "Post")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
                )
            },
            bottomBar = {
                if (!isPollMode) {
                    Column(modifier = Modifier.fillMaxWidth().background(Color.White)) {
                        Divider()
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Add to your post", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            
                            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                IconButton(onClick = { 
                                    imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                }) {
                                    Icon(TablerIcons.Photo, contentDescription = "Photo", tint = Color(0xFF4CAF50), modifier = Modifier.size(24.dp))
                                }
                                IconButton(onClick = { 
                                    videoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))
                                }) {
                                    Icon(TablerIcons.PlayerPlay, contentDescription = "Video", tint = Color(0xFFE91E63), modifier = Modifier.size(24.dp))
                                }
                                IconButton(onClick = { isPollMode = true; selectedMediaItems = emptyList() }) {
                                    Icon(TablerIcons.List, contentDescription = "Poll", tint = AtmiyaPrimary, modifier = Modifier.size(24.dp))
                                }
                            }
                        }
                    }
                }
            },
            containerColor = Color.White
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
                        modifier = Modifier.size(48.dp).clip(CircleShape).background(Color.Gray),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(userName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color.LightGray.copy(alpha = 0.3f),
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            Text(
                                "Public", 
                                style = MaterialTheme.typography.labelSmall, 
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                color = Color.Gray
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                if (isPollMode) {
                    OutlinedTextField(
                        value = pollQuestion,
                        onValueChange = { pollQuestion = it },
                        placeholder = { Text("Ask a question...", fontSize = 20.sp, color = Color.Gray) },
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
                        placeholder = { Text("What's on your mind?", fontSize = 24.sp, color = Color.Gray) }, // Larger placeholder
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
