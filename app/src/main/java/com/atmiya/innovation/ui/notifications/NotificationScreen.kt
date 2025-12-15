package com.atmiya.innovation.ui.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.atmiya.innovation.ui.components.SoftCard // Assuming exists or will use standard Card
import com.atmiya.innovation.repository.FirestoreRepository
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Locale

// Data model (local for UI)
import com.atmiya.innovation.data.Notification

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationScreen(
    onBack: () -> Unit,
    onNotificationClick: (String, String) -> Unit // type, id
) {
    val firestoreRepo = remember { FirestoreRepository() }
    val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
    val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
    val currentUserId = auth.currentUser?.uid ?: ""
    
    var notifications by remember { mutableStateOf<List<Notification>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    // Local Preferences for "Clearing" notifications
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = remember { context.getSharedPreferences("notification_prefs", android.content.Context.MODE_PRIVATE) }
    var clearedTimestamp by remember { mutableStateOf(prefs.getLong("cleared_timestamp", 0L)) }

    LaunchedEffect(currentUserId) {
        if (currentUserId.isBlank()) {
            isLoading = false
            return@LaunchedEffect
        }
        
        // Real-time listener on User's notifications
        db.collection("users").document(currentUserId)
            .collection("notifications")
            .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .limit(50)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    isLoading = false
                    return@addSnapshotListener
                }
                
                if (snapshot != null) {
                    notifications = snapshot.documents.mapNotNull { it.toObject(Notification::class.java) }
                    isLoading = false
                }
            }
    }
    
    // Filter locally based on cleared timestamp
    val filteredNotifications = notifications.filter { 
        (it.createdAt?.toDate()?.time ?: 0L) > clearedTimestamp 
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notifications") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (filteredNotifications.isNotEmpty()) {
                        TextButton(onClick = {
                            val now = System.currentTimeMillis()
                            prefs.edit().putLong("cleared_timestamp", now).apply()
                            clearedTimestamp = now
                        }) {
                            Text("Clear All")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        if (isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (filteredNotifications.isEmpty()) {
             Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Notifications, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(64.dp))
                    Spacer(Modifier.height(16.dp))
                    Text("No new notifications", style = MaterialTheme.typography.bodyLarge, color = Color.Gray)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredNotifications) { item ->
                    NotificationCard(item) {
                        // Determine target ID based on type
                        val targetId = when (item.type) {
                            "funding_application" -> item.senderId ?: item.referenceId // Navigate to startup profile (sender)
                            else -> item.referenceId
                        }
                        onNotificationClick(item.type, targetId)
                    }
                }
            }
        }
    }
}

@Composable
fun NotificationCard(item: Notification, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.5f)),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon based on type
            Box(
                 modifier = Modifier
                     .size(40.dp)
                     .background(MaterialTheme.colorScheme.primaryContainer, androidx.compose.foundation.shape.CircleShape),
                 contentAlignment = Alignment.Center
            ) {
                 Icon(
                     imageVector = Icons.Default.Notifications, 
                     contentDescription = null,
                     tint = MaterialTheme.colorScheme.primary,
                     modifier = Modifier.size(24.dp)
                 )
            }
            
            Spacer(Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = item.message,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(8.dp))
                val date = item.createdAt?.toDate()
                if (date != null) {
                    Text(
                        text = SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault()).format(date),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray
                    )
                }
            }
        }
    }
}
