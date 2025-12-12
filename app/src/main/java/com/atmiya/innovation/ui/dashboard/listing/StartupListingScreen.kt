package com.atmiya.innovation.ui.dashboard.listing

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import compose.icons.TablerIcons
import compose.icons.tablericons.ArrowLeft
import compose.icons.tablericons.PlayerPlay
import compose.icons.tablericons.Star
import compose.icons.tablericons.InfoCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.atmiya.innovation.repository.FirestoreRepository
import com.atmiya.innovation.ui.theme.AtmiyaPrimary
import com.atmiya.innovation.ui.theme.AtmiyaSecondary
import com.atmiya.innovation.ui.components.CommonListingCard
import com.atmiya.innovation.data.User // Assuming we pull 'Startup' users here
import androidx.compose.ui.unit.sp

// Define Startup Model roughly if separate or just use User
import kotlinx.coroutines.launch
import com.atmiya.innovation.data.Startup

import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StartupListingScreen(
    onBack: () -> Unit,
    onStartupClick: (String) -> Unit,
    onChatClick: (String, String) -> Unit = { _, _ -> }
) {
    val repository = remember { FirestoreRepository() }
    val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
    val scope = rememberCoroutineScope()
    val currentUser = auth.currentUser

    var connectionStatusMap by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var isLoading by remember { mutableStateOf(true) }

    var key by remember { mutableStateOf(0) }
    val startupsFlow = remember(key) { repository.getStartupsFlow() }
    val startups by startupsFlow.collectAsState(initial = emptyList())

    // Connection Logic - Fetching this once on mount for now
    LaunchedEffect(currentUser) {
        try {
            if (currentUser != null) {
                // We should ideally use flows here too for connection status updates, 
                // but prioritizing the list update first.
                val accepted = repository.getAcceptedConnections(currentUser.uid)
                val sentPending = repository.getSentConnectionRequests(currentUser.uid)
                val map = mutableMapOf<String, String>()
                accepted.forEach { 
                    val otherId = if (it.senderId == currentUser.uid) it.receiverId else it.senderId
                    map[otherId] = "connected"
                }
                sentPending.forEach { map[it.receiverId] = "pending_sent" }
                connectionStatusMap = map
            }
        } catch (e: Exception) {
            // Handle error
        } finally {
            isLoading = false
        }
    }
    
    LaunchedEffect(currentUser) {
        if (currentUser != null) {
            repository.getConnectionRequests(currentUser.uid).collect { requests ->
                 val receivedMap = requests.associate { it.senderId to "pending_received" }
                 connectionStatusMap = connectionStatusMap + receivedMap
            }
        }
    }

    var isRefreshing by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Startups", fontWeight = FontWeight.Bold, color = AtmiyaPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(TablerIcons.ArrowLeft, contentDescription = "Back", tint = AtmiyaPrimary, modifier = Modifier.size(28.dp))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White // White
                )
            )
        },
        containerColor = Color.White
    ) { padding ->
         SwipeRefresh(
            state = rememberSwipeRefreshState(isRefreshing),
            onRefresh = {
                scope.launch {
                    isRefreshing = true
                    key++
                    kotlinx.coroutines.delay(1000)
                    isRefreshing = false
                }
            },
            modifier = Modifier.fillMaxSize().padding(padding)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // DEBUG SECTION
                // Debug info removed as per request

                items(startups) { startup ->
                    val status = if (currentUser?.uid == startup.uid) "self" else connectionStatusMap[startup.uid] ?: "none"
                    StartupCard(
                        startup = startup, 
                        connectionStatus = status,
                        onClick = { onStartupClick(startup.uid) },
                        onConnect = {
                            if (currentUser != null) {
                                if (status == "connected") {
                                    onChatClick(startup.uid, startup.startupName)
                                } else {
                                    scope.launch {
                                        connectionStatusMap = connectionStatusMap + (startup.uid to "pending_sent")
                                        val sender = repository.getUser(currentUser.uid)
                                        if (sender != null) {
                                            repository.sendConnectionRequest(sender, startup.uid)
                                        }
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

@Composable
fun StartupCard(
    startup: Startup, 
    connectionStatus: String,
    onClick: () -> Unit,
    onConnect: () -> Unit
) {
    val tags = listOf(startup.sector, startup.stage).filter { it.isNotBlank() }
    
    CommonListingCard(
        imageModel = startup.logoUrl,
        title = startup.startupName,
        subtitle = startup.description.ifBlank { "No description" },
        tags = tags,
        metricValue = startup.fundingAsk.ifBlank { "N/A" },
        metricLabel = "Asking",
        footerValue = "Investment Opportunity",
        footerIcon = TablerIcons.PlayerPlay,
        connectionStatus = connectionStatus,
        onConnectAction = onConnect,
        onClick = onClick
    )
}
