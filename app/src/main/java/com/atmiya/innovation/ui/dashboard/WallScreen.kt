package com.atmiya.innovation.ui.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.atmiya.innovation.ui.theme.AtmiyaPrimary
import com.atmiya.innovation.ui.theme.AtmiyaSecondary

data class WallPost(
    val id: String,
    val authorName: String,
    val content: String,
    val timestamp: String
)

@Composable
fun WallScreen() {
    val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
    val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
    
    // State for posts
    var posts by remember { mutableStateOf(listOf<WallPost>()) }
    var searchQuery by remember { mutableStateOf("") }
    
    // Listen for real-time updates
    LaunchedEffect(Unit) {
        db.collection("posts")
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    return@addSnapshotListener
                }
                
                if (snapshot != null) {
                    val newPosts = snapshot.documents.map { doc ->
                        WallPost(
                            id = doc.id,
                            authorName = doc.getString("authorName") ?: "Unknown",
                            content = doc.getString("content") ?: "",
                            timestamp = "Just now" // Simplified for MVP (Use relative time lib in prod)
                        )
                    }
                    posts = newPosts
                }
            }
    }
    
    val filteredPosts = posts.filter { 
        it.content.contains(searchQuery, ignoreCase = true) || 
        it.authorName.contains(searchQuery, ignoreCase = true) 
    }

    var showCreateDialog by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            // ...
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = "Community Wall",
                    style = MaterialTheme.typography.headlineMedium,
                    color = AtmiyaPrimary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Search Posts") },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    shape = RoundedCornerShape(12.dp)
                )
            }
            if (filteredPosts.isEmpty()) {
                item {
                    Text("No posts found.", color = Color.Gray)
                }
            } else {
                items(filteredPosts) { post ->
                    PostCard(post)
                }
            }
        }
        // ...
    }
        
        if (showCreateDialog) {
            CreatePostDialog(
                onDismiss = { showCreateDialog = false },
                onPost = { content ->
                    val user = auth.currentUser
                    if (user != null && content.isNotBlank()) {
                        val post = hashMapOf(
                            "authorId" to user.uid,
                            "authorName" to (user.phoneNumber ?: "User"), // TODO: Fetch real name
                            "content" to content,
                            "timestamp" to com.google.firebase.Timestamp.now()
                        )
                        db.collection("posts").add(post)
                    }
                    showCreateDialog = false
                }
            )
        }
    }
}

@Composable
fun CreatePostDialog(onDismiss: () -> Unit, onPost: (String) -> Unit) {
    var text by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Post") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("What's on your mind?") },
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(onClick = { onPost(text) }) {
                Text("Post")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun PostCard(post: WallPost) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Avatar Placeholder
                Surface(
                    modifier = Modifier.size(40.dp),
                    shape = MaterialTheme.shapes.small,
                    color = Color.LightGray
                ) {}
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(text = post.authorName, fontWeight = FontWeight.Bold)
                    Text(text = post.timestamp, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = post.content)
        }
    }
}
