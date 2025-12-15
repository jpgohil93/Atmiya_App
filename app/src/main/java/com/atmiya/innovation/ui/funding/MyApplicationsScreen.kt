package com.atmiya.innovation.ui.funding

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.atmiya.innovation.data.ChatChannel
import com.atmiya.innovation.data.FundingApplication
import com.atmiya.innovation.repository.FirestoreRepository
import com.atmiya.innovation.ui.theme.AtmiyaPrimary
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyApplicationsScreen(
    onBack: () -> Unit,
    onChatClick: (String, String) -> Unit // channelId, otherUserName
) {
    val repository = remember { FirestoreRepository() }
    val auth = FirebaseAuth.getInstance()
    val scope = rememberCoroutineScope()
    val currentUser = auth.currentUser
    
    var applications by remember { mutableStateOf<List<FundingApplication>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorText by remember { mutableStateOf<String?>(null) } // Error State

    LaunchedEffect(currentUser) {
        if (currentUser != null) {
            repository.getMyApplications(currentUser.uid, onError = { msg ->
                errorText = msg
                isLoading = false
            }).collect {
                applications = it
                isLoading = false
            }
        }
    }

    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = { Text("My Applications", fontWeight = FontWeight.Bold) },
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
            Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                 // ERROR DISPLAY
                if (errorText != null) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        modifier = Modifier.fillMaxWidth().padding(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Error Loading Applications",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = errorText ?: "",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Hint: If this is an Index Error, look at Logcat for the link.",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
                
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (applications.isEmpty() && errorText == null) {
                        item {
                            Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                                Text("You haven't applied to any funding calls yet.", color = Color.Gray)
                            }
                        }
                    } else {
                        items(applications) { app ->
                            MyApplicationCard(
                                app = app,
                                onChatClick = {
                                    scope.launch {
                                        if (currentUser != null) {
                                            try {
                                                val call: com.atmiya.innovation.data.FundingCall? = repository.getFundingCall(app.callId)
                                                if (call != null) {
                                                    val iName = call.investorName
                                                    val iId = call.investorId
                                                    val channel = ChatChannel(
                                                        participants = listOf(currentUser.uid, iId),
                                                        participantNames = mapOf(
                                                            currentUser.uid to (currentUser.displayName ?: "Startup"),
                                                            iId to iName
                                                        )
                                                    )
                                                    val channelId = repository.createChatChannel(channel)
                                                    onChatClick(channelId, iName)
                                                }
                                            } catch (e: Exception) {
                                                // Handle error
                                            }
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MyApplicationCard(
    app: FundingApplication,
    onChatClick: () -> Unit
) {
    // Mask "ignored" status as "applied" (Pending)
    val displayStatus = if (app.status == "ignored") "applied" else app.status
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Application for Funding", // Ideally Call Title
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                StatusBadge(status = displayStatus)
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Ask: ₹${app.fundingAsk}",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )
            
            if (displayStatus == "accepted") {
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onChatClick,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = AtmiyaPrimary)
                ) {
                    Icon(Icons.Default.Email, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Chat with Investor")
                }
            }
        }
    }
}
