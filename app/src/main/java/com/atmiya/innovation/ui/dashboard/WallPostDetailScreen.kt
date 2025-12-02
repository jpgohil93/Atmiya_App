package com.atmiya.innovation.ui.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.atmiya.innovation.data.WallPost
import com.atmiya.innovation.repository.FirestoreRepository
import com.atmiya.innovation.ui.components.SoftScaffold
import com.atmiya.innovation.ui.theme.AtmiyaPrimary
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WallPostDetailScreen(
    postId: String,
    onBack: () -> Unit
) {
    val repository = remember { FirestoreRepository() }
    val auth = FirebaseAuth.getInstance()
    val scope = rememberCoroutineScope()
    
    var post by remember { mutableStateOf<WallPost?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(postId) {
        if (postId.isBlank()) {
            android.util.Log.w("WallPostDetailScreen", "Invalid postId")
            isLoading = false
            return@LaunchedEffect
        }
        
        try {
            val snapshot = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("wallPosts").document(postId).get().await()
            post = snapshot.toObject(WallPost::class.java)?.copy(id = snapshot.id)
        } catch (e: Exception) {
            android.util.Log.e("WallPostDetailScreen", "Error fetching post", e)
        } finally  {
            isLoading = false
        }
    }

    SoftScaffold(
        topBar = {
            TopAppBar(
                title = { Text("Post Detail", fontWeight = FontWeight.Bold) },
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
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = AtmiyaPrimary)
            }
        } else if (post == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Post not found.")
            }
        } else {
            val safePost = post ?: return@SoftScaffold // Extra safety
            
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                PostCard(
                    post = safePost,
                    currentUserId = auth.currentUser?.uid ?: "",
                    onLike = { 
                        scope.launch { 
                            try {
                                repository.toggleUpvote(safePost.id, auth.currentUser?.uid ?: "")
                            } catch (e: Exception) {
                                android.util.Log.e("WallPostDetailScreen", "Error toggling upvote", e)
                            }
                        }
                    },
                    onVote = { optionId ->
                        scope.launch {
                            try {
                                repository.voteOnPoll(safePost.id, auth.currentUser?.uid ?: "", optionId)
                            } catch (e: Exception) {
                                android.util.Log.e("WallPostDetailScreen", "Error voting", e)
                            }
                        }
                    }
                )
            }
        }
    }
}
