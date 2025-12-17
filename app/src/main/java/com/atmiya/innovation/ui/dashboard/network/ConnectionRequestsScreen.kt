package com.atmiya.innovation.ui.dashboard.network

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp // Added
import com.atmiya.innovation.data.ConnectionRequest
import com.atmiya.innovation.repository.FirestoreRepository
import com.atmiya.innovation.ui.components.NetworkCard
import com.atmiya.innovation.ui.components.PillBadge
import kotlinx.coroutines.launch

@Composable
fun ConnectionRequestsScreen(
    currentUserId: String,
    onNavigateToProfile: (String) -> Unit
) {
    val repository = remember { FirestoreRepository() }
    val scope = rememberCoroutineScope()
    var key by remember { mutableStateOf(0) }
    
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    // UI State for Requests
    var requests by remember { mutableStateOf<List<ConnectionRequest>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(key, currentUserId) {
        if (currentUserId.isBlank()) {
            errorMessage = "Error: User ID is missing (Not Logged In)"
            isLoading = false
            return@LaunchedEffect
        }
        
        try {
            isLoading = true
            repository.getIncomingConnectionRequests(currentUserId).collect { 
                requests = it 
                isLoading = false
                errorMessage = null // Clear error on success
            }
        } catch (e: Exception) {
            isLoading = false
            errorMessage = "Failed to load requests: ${e.message}"
            android.util.Log.e("ConnectionRequests", "Error loading", e)
        }
    }

    val context = androidx.compose.ui.platform.LocalContext.current

    Box(modifier = Modifier.fillMaxSize()) {
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(androidx.compose.ui.Alignment.Center))
        } else if (errorMessage != null) {
            Column(
                modifier = Modifier.align(androidx.compose.ui.Alignment.Center),
                horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
            ) {
                Text(
                    text = errorMessage!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(16.dp)
                )
                Text(
                    text = "Debug Info: UID Length=${currentUserId.length}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Button(onClick = { key++ }) {
                    Text("Retry")
                }
            }
        } else if (requests.isEmpty()) {
            Column(
                modifier = Modifier.align(androidx.compose.ui.Alignment.Center),
                horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
            ) {
                Text("No pending connection requests.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (currentUserId.isBlank()) {
                    Text("(Debug: UserID is empty)", color = Color.Red, fontSize = 10.sp)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Debug Header
                // item { Text("Debug: UID $currentUserId | Count: ${requests.size}", fontSize = 10.sp) }
                
                items(requests) { request ->
                    ConnectionRequestCard(
                        request = request,
                        onAccept = {
                             scope.launch {
                                 try {
                                     repository.acceptConnectionRequest(request.id)
                                     android.widget.Toast.makeText(context, "Connected!", android.widget.Toast.LENGTH_SHORT).show()
                                 } catch(e: Exception) {
                                     android.widget.Toast.makeText(context, "Error: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                                 }
                             }
                        },
                        onDecline = {
                            scope.launch {
                                try {
                                    repository.declineConnectionRequest(request.id)
                                    android.widget.Toast.makeText(context, "Request declined.", android.widget.Toast.LENGTH_SHORT).show()
                                } catch(e: Exception) {
                                    android.widget.Toast.makeText(context, "Error: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        onProfileClick = { onNavigateToProfile(request.senderId) }
                    )
                }
            }
        }
    }
}

@Composable
fun ConnectionRequestCard(
    request: ConnectionRequest,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
    onProfileClick: () -> Unit
) {
    NetworkCard(
        imageModel = request.senderPhotoUrl ?: "",
        name = request.senderName,
        roleOrTitle = request.senderRole.replaceFirstChar { it.uppercase() },
        badges = {
             PillBadge(text = "Received", backgroundColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer)
        },
        infoContent = {
             // No extra info for now, just name/role
        },
        primaryButtonText = "Accept",
        onPrimaryClick = onAccept,
        secondaryButtonText = "Decline",
        onSecondaryClick = onDecline
    )
}
