package com.atmiya.innovation.ui.dashboard.network

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email // Replaced Message
import androidx.compose.material.icons.filled.Check // Replaced Check
import androidx.compose.material.icons.filled.Close // Replaced Close
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
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
import com.atmiya.innovation.data.ConnectionRequest
import com.atmiya.innovation.repository.FirestoreRepository
import com.atmiya.innovation.ui.theme.AtmiyaPrimary
import com.atmiya.innovation.ui.theme.AtmiyaSecondary
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConnectionsScreen(
    onChatClick: (String, String) -> Unit, // userId, userName
    onBack: () -> Unit
) {
    val repository = remember { FirestoreRepository() }
    val currentUser = FirebaseAuth.getInstance().currentUser
    val scope = rememberCoroutineScope()

    var selectedTab by remember { mutableStateOf(0) } // 0: Network, 1: Requests
    val tabs = listOf("My Network", "Requests")

    var requests by remember { mutableStateOf<List<ConnectionRequest>>(emptyList()) }
    var network by remember { mutableStateOf<List<ConnectionRequest>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    // Fetch Data
    LaunchedEffect(currentUser) {
        if (currentUser != null) {
            // Real-time listener for Requests
            launch {
                repository.getConnectionRequests(currentUser.uid).collect {
                    requests = it
                }
            }
            // One-time fetch for Network (refresh on tab switch or periodically?)
            // For now, fetch initially.
            launch {
                network = repository.getAcceptedConnections(currentUser.uid)
                isLoading = false
            }
        }
    }

    // Refresh Network when tab switched
    LaunchedEffect(selectedTab) {
        if (selectedTab == 0 && currentUser != null) {
            network = repository.getAcceptedConnections(currentUser.uid)
        }
    }

    Scaffold(
        topBar = {
            Column {
                CenterAlignedTopAppBar(
                    title = { Text("My Connections", fontWeight = FontWeight.Bold) },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = Color.White
                    )
                )
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.White,
                    contentColor = AtmiyaPrimary,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = AtmiyaSecondary
                        )
                    }
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = { 
                                BadgeBox(
                                    content = { Text(title, fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal) },
                                    badge = {
                                        if (index == 1 && requests.isNotEmpty()) {
                                            Badge(containerColor = AtmiyaSecondary) { Text("${requests.size}") }
                                        }
                                    }
                                )
                            }
                        )
                    }
                }
            }
        },
        containerColor = Color.White
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = AtmiyaSecondary)
                }
            } else {
                if (selectedTab == 0) {
                    NetworkList(
                        connections = network, 
                        currentUserId = currentUser?.uid ?: "",
                        onChatClick = onChatClick
                    )
                } else {
                    RequestList(
                        requests = requests,
                        onAccept = { req ->
                            scope.launch {
                                repository.updateConnectionStatus(req.id, "accepted")
                                // Refresh network immediately
                                network = repository.getAcceptedConnections(currentUser?.uid ?: "")
                            }
                        },
                        onIgnore = { req ->
                            scope.launch {
                                repository.updateConnectionStatus(req.id, "ignored")
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun NetworkList(
    connections: List<ConnectionRequest>,
    currentUserId: String,
    onChatClick: (String, String) -> Unit
) {
    if (connections.isEmpty()) {
        EmptyState("No connections yet", "Connect with Startups, Investors, or Mentors from the listings.")
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(connections) { conn ->
                // Identify the "other" person
                val isSender = conn.senderId == currentUserId
                val otherId = if (isSender) conn.receiverId else conn.senderId
                // We don't have "otherName" stored in ConnectionRequest for Receiver side if I am sender.
                // Wait, ConnectionRequest stores senderName. If I am receiver, I know senderName.
                // If I am sender, I only know receiverId. I don't have receiverName stored.
                // FIX: We should fetch User details or store receiverName too.
                // For MVP: Let's assume we need to join. Or better, update Model to store snapshot of both?
                // OR: Just fetch user (expensive list?).
                // Let's store receiverName in the request for simplicity, or just fetch.
                // Given the constraints, let's fetch for now or show "User".
                // Actually, if I accepted a request, I am receiver, so I have senderName.
                // If *I* sent the request and they accepted, I am sender. Sender knows who they sent to?
                // Let's rely on "senderName" if I am receiver.
                // If I am sender, I need to fetch receiver.
                
                // Let's simplify: Display the side we have info for.
                // If I am receiver: Show Sender Info.
                // If I am sender: I need receiver info.
                // CRITICAL GAP: Model lacks receiverName/Photo.
                // Correct approach: Store both snapshots or fetch.
                // Let's fetch usage `AsyncImage` and a placeholder name if needed.
                // Better: Update Repository logic to include receiver details? No.
                // Let's just render what we have. If missing, show "Loading..." (Maybe fetch individually?)
                
                // QUICK FIX: Render senderName if `conn.senderId != currentUserId`.
                // If `conn.senderId == currentUserId` (I Sent), we need receiver name.
                // Let's assume for this sprint we primarily view INCOMING accepted connections properly.
                // OUTGOING accepted connections list might miss names without fetching.
                
                ConnectionItem(
                    connection = conn,
                    currentUserId = currentUserId,
                    onChatClick = onChatClick
                )
            }
        }
    }
}

@Composable
fun ConnectionItem(
    connection: ConnectionRequest,
    currentUserId: String,
    onChatClick: (String, String) -> Unit
) {
    val isIncoming = connection.receiverId == currentUserId
    
    // If incoming, we have sender details.
    // If outgoing, we lack receiver details. 
    // Ideally we fetch. For MVP speed, let's use a "Fetch" effect.
    var displayName by remember { mutableStateOf(if (isIncoming) connection.senderName else "Loading...") }
    var displayRole by remember { mutableStateOf(if (isIncoming) connection.senderRole else "") }
    var displayPhoto by remember { mutableStateOf(if (isIncoming) connection.senderPhotoUrl else null) }
    
    if (!isIncoming) {
        // Fetch receiver details
        val repo = remember { FirestoreRepository() }
        LaunchedEffect(connection.receiverId) {
            val user = repo.getUser(connection.receiverId)
            if (user != null) {
                displayName = user.name
                displayRole = user.role
                displayPhoto = user.profilePhotoUrl
            }
        }
    }
    val targetId = if (isIncoming) connection.senderId else connection.receiverId

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF9F9F9)),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = displayPhoto, 
                contentDescription = null,
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(Color.LightGray),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = displayName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(text = displayRole.capitalize(), style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
            IconButton(onClick = { onChatClick(targetId, displayName) }) {
                Icon(Icons.Default.Email, contentDescription = "Message", tint = AtmiyaSecondary)
            }
        }
    }
}

@Composable
fun RequestList(
    requests: List<ConnectionRequest>,
    onAccept: (ConnectionRequest) -> Unit,
    onIgnore: (ConnectionRequest) -> Unit
) {
    if (requests.isEmpty()) {
        EmptyState("No pending requests", "New connection requests will appear here.")
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(requests) { req ->
                RequestItem(request = req, onAccept = onAccept, onIgnore = onIgnore)
            }
        }
    }
}

@Composable
fun RequestItem(
    request: ConnectionRequest,
    onAccept: (ConnectionRequest) -> Unit,
    onIgnore: (ConnectionRequest) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray.copy(alpha=0.5f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(
                    model = request.senderPhotoUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(50.dp)
                        .clip(CircleShape)
                        .background(Color.LightGray),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(text = request.senderName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(text = request.senderRole.capitalize(), style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    Text(text = "Sent a connection request", style = MaterialTheme.typography.labelSmall, color = AtmiyaPrimary)
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { onAccept(request) },
                    colors = ButtonDefaults.buttonColors(containerColor = AtmiyaSecondary),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Accept")
                }
                OutlinedButton(
                    onClick = { onIgnore(request) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Ignore")
                }
            }
        }
    }
}

@Composable
fun EmptyState(title: String, subtitle: String) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = Color.Gray, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = Color.LightGray, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    }
}

@Composable
fun BadgeBox(content: @Composable () -> Unit, badge: @Composable () -> Unit) {
    Box {
        content()
        Box(modifier = Modifier.align(Alignment.TopEnd).offset(x = 12.dp, y = (-8).dp)) {
            badge()
        }
    }
}

fun String.capitalize(): String {
    return this.replaceFirstChar { if (it.isLowerCase()) it.titlecase(java.util.Locale.getDefault()) else it.toString() }
}
