package com.atmiya.innovation.ui.funding

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Email // Replaced Chat
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.atmiya.innovation.data.ChatChannel
import com.atmiya.innovation.data.FundingApplication
import com.atmiya.innovation.repository.FirestoreRepository
import com.atmiya.innovation.ui.components.SoftButton
import com.atmiya.innovation.ui.theme.AtmiyaPrimary
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApplicationDetailScreen(
    callId: String,
    applicationId: String,
    onBack: () -> Unit,
    onChatCreated: (String, String) -> Unit // channelId, otherUserName
) {
    val repository = remember { FirestoreRepository() }
    val auth = FirebaseAuth.getInstance()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    var application by remember { mutableStateOf<FundingApplication?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isProcessing by remember { mutableStateOf(false) }

    LaunchedEffect(callId, applicationId) {
        try {
            application = repository.getApplication(callId, applicationId)
        } catch (e: Exception) {
            // Handle error
        } finally {
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = { Text("Application Details", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.smallTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = AtmiyaPrimary
                )
            )
        }
    ) { padding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = AtmiyaPrimary)
            }
        } else {
            val app = application
            if (app == null) {
                Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Text("Application not found.", color = Color.Gray)
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                ) {
                    // Startup Header
                    Text(app.startupName, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    Text(app.startupSector, style = MaterialTheme.typography.bodyLarge, color = Color.Gray)
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    StatusBadge(status = app.status)
                    Spacer(modifier = Modifier.height(24.dp))

                    // Details
                    DetailItem("Funding Ask", "₹${app.fundingAsk}")
                    DetailItem("Stage", app.startupStage)
                    DetailItem("Contact", "${app.startupEmail}\n${app.startupPhone}")
                    DetailItem("Location", "${app.city}, ${app.state}")
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    if (!app.additionalNote.isNullOrBlank()) {
                        Text("Note / Cover Letter", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(app.additionalNote, style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // Pitch Deck
                    if (!app.pitchDeckUrl.isNullOrBlank()) {
                        SoftButton(
                            text = "View Pitch Deck",
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(app.pitchDeckUrl))
                                context.startActivity(intent)
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                    }

                    // Actions
                    if (app.status == "applied" || app.status == "ignored") {
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            OutlinedButton(
                                onClick = {
                                    scope.launch {
                                        isProcessing = true
                                        repository.updateApplicationStatus(callId, applicationId, "ignored")
                                        application = application?.copy(status = "ignored")
                                        isProcessing = false
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                enabled = !isProcessing,
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Gray)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Ignore")
                            }
                            
                            Button(
                                onClick = {
                                    scope.launch {
                                        isProcessing = true
                                        // 1. Update Status
                                        repository.updateApplicationStatus(callId, applicationId, "accepted")
                                        application = application?.copy(status = "accepted")
                                        
                                        // 2. Create Chat Channel
                                        val currentUser = auth.currentUser
                                        if (currentUser != null) {
                                            val channel = ChatChannel(
                                                participants = listOf(currentUser.uid, app.startupId),
                                                participantNames = mapOf(
                                                    currentUser.uid to (currentUser.displayName ?: "Investor"),
                                                    app.startupId to app.startupName
                                                ),
                                                lastMessage = "Application Accepted",
                                                lastMessageTimestamp = com.google.firebase.Timestamp.now()
                                            )
                                            val channelId = repository.createChatChannel(channel)
                                            onChatCreated(channelId, app.startupName)
                                        }
                                        isProcessing = false
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                enabled = !isProcessing,
                                colors = ButtonDefaults.buttonColors(containerColor = AtmiyaPrimary)
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Accept")
                            }
                        }
                    } else if (app.status == "accepted") {
                        Button(
                            onClick = {
                                scope.launch {
                                    // Navigate to existing chat
                                    val currentUser = auth.currentUser
                                    if (currentUser != null) {
                                        val channel = ChatChannel(
                                            participants = listOf(currentUser.uid, app.startupId)
                                        )
                                        // Create/Get channel ID deterministically
                                        val channelId = repository.createChatChannel(channel) 
                                        onChatCreated(channelId, app.startupName)
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = AtmiyaPrimary)
                        ) {
                            Icon(Icons.Default.Email, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Chat with Startup")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DetailItem(label: String, value: String) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = Color.Gray)
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}
