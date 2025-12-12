package com.atmiya.innovation.ui.dashboard.network

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search

import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material.icons.filled.Delete // Replaced Archive
import androidx.compose.material.icons.filled.Star // Replaced PushPin
import androidx.compose.material.icons.filled.Email // Replaced Chat
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.atmiya.innovation.data.ChatChannel
import com.atmiya.innovation.data.ConnectionRequest
import com.atmiya.innovation.repository.FirestoreRepository
import com.atmiya.innovation.ui.theme.AtmiyaSecondary
import com.atmiya.innovation.ui.theme.AtmiyaPrimary
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetworkHubScreen(
    onBack: () -> Unit,
    onChatClick: (String) -> Unit // userId
) {
    val repository = remember { FirestoreRepository() }
    val auth = FirebaseAuth.getInstance()
    val currentUser = auth.currentUser
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    
    // Main Tabs: Chats, Network, Requests
    var mainTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Chats", "My Network", "Requests")
    
    // --- Data States ---
    // Chats
    var allChannels by remember { mutableStateOf<List<ChatChannel>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }
    
    // Requests & Network
    var requests by remember { mutableStateOf<List<ConnectionRequest>>(emptyList()) }
    var network by remember { mutableStateOf<List<ConnectionRequest>>(emptyList()) }
    
    // --- Data Fetching ---
    LaunchedEffect(currentUser) {
        if (currentUser != null) {
            // Chats
            launch {
                repository.getChatChannels(currentUser.uid).collect { allChannels = it }
            }
            // Requests
            launch {
                repository.getConnectionRequests(currentUser.uid).collect { requests = it }
            }
            // Network (Accepted)
            launch {
                // Initial fetch
                network = repository.getAcceptedConnections(currentUser.uid)
            }
        }
    }
    
    // Refresh Network on tab switch
    LaunchedEffect(mainTab) {
        if (mainTab == 1 && currentUser != null) {
            network = repository.getAcceptedConnections(currentUser.uid)
        }
    }

    Scaffold(
        topBar = {
            Column(modifier = Modifier.background(Color.White)) {
                CenterAlignedTopAppBar(
                    title = { Text("Connection & Chat", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.White)
                )
                
                // Tabs
                TabRow(
                    selectedTabIndex = mainTab,
                    containerColor = Color.White,
                    contentColor = AtmiyaPrimary,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[mainTab]),
                            color = AtmiyaSecondary
                        )
                    }
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = mainTab == index,
                            onClick = { mainTab = index },
                            text = {
                                if (index == 2 && requests.isNotEmpty()) {
                                    BadgedBox(
                                        badge = { Badge(containerColor = AtmiyaSecondary) { Text("${requests.size}") } }
                                    ) {
                                        Text(title, fontWeight = if (mainTab == index) FontWeight.Bold else FontWeight.Normal)
                                    }
                                } else {
                                    Text(title, fontWeight = if (mainTab == index) FontWeight.Bold else FontWeight.Normal)
                                }
                            }
                        )
                    }
                }
            }
        },
        containerColor = Color.White
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when(mainTab) {
                0 -> ChatListContent(
                    currentUser = currentUser,
                    channels = allChannels,
                    searchQuery = searchQuery,
                    onSearchChange = { searchQuery = it },
                    onChatClick = onChatClick, 
                    repository = repository
                )
                1 -> NetworkListContent(
                    connections = network,
                    currentUserId = currentUser?.uid ?: "",
                    onChatClick = { uid, name -> 
                        android.util.Log.d("NetworkHub", "DEBUG: Chat clicked for userId=$uid, name=$name")
                        Toast.makeText(context, "Opening chat: $name ($uid)", Toast.LENGTH_SHORT).show()
                        try {
                            if (uid.isBlank()) {
                                android.util.Log.e("NetworkHub", "ERROR: userId is blank!")
                                Toast.makeText(context, "ERROR: User ID is blank!", Toast.LENGTH_LONG).show()
                            } else {
                                onChatClick(uid) 
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("NetworkHub", "CRASH in onChatClick", e)
                            Toast.makeText(context, "CRASH: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                )
                2 -> RequestsListContent(
                     requests = requests,
                     onAccept = { req ->
                         scope.launch { 
                            repository.updateConnectionStatus(req.id, "accepted")
                            network = repository.getAcceptedConnections(currentUser?.uid ?: "")
                         }
                     },
                     onIgnore = { req ->
                         scope.launch {
                            repository.updateConnectionStatus(req.id, "ignored")
                         }
                     },
                     scope = scope, 
                     repository = repository
                )
            }
        }
    }
}

// --- Sub-Composables ---

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChatListContent(
    currentUser: com.google.firebase.auth.FirebaseUser?,
    channels: List<ChatChannel>,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    onChatClick: (String) -> Unit,
    repository: FirestoreRepository
) {
    val scope = rememberCoroutineScope()
    
    // Filter
    val filteredChannels = remember(channels, searchQuery, currentUser) {
        channels.filter { channel ->
            val otherId = channel.participants.find { it != currentUser?.uid } ?: return@filter false
            val name = channel.participantNames[otherId] ?: "Unknown"
            name.contains(searchQuery, ignoreCase = true) && !channel.archivedBy.contains(currentUser?.uid)
        }.sortedByDescending { it.lastMessageTimestamp }
    }
    
    val pinnedChannels = remember(filteredChannels, currentUser) {
        filteredChannels.filter { it.pinnedBy.contains(currentUser?.uid) }
    }
    val otherChannels = remember(filteredChannels, currentUser) {
        filteredChannels.filter { !it.pinnedBy.contains(currentUser?.uid) }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Search Bar
        TextField(
             value = searchQuery,
             onValueChange = onSearchChange,
             placeholder = { Text("Search chats...", color = Color.Gray) },
             leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray) },
             modifier = Modifier
                 .fillMaxWidth()
                 .padding(16.dp)
                 .height(56.dp) // Fixed height to standard
                 .border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(28.dp)),
             shape = RoundedCornerShape(28.dp),
             colors = TextFieldDefaults.colors(
                 focusedContainerColor = Color.White,
                 unfocusedContainerColor = Color.White,
                 focusedIndicatorColor = Color.Transparent,
                 unfocusedIndicatorColor = Color.Transparent
             ),
             singleLine = true
         )
         
         if (pinnedChannels.isNotEmpty()) {
             Text("Pinned", fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 16.dp, bottom = 8.dp))
             LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(pinnedChannels) { channel ->
                    val otherId = channel.participants.find { it != currentUser?.uid } ?: ""
                    val name = channel.participantNames[otherId] ?: "Unknown"
                    val photo = channel.participantPhotos[otherId] ?: ""
                    PinnedChatCircle(name, photo) { onChatClick(otherId) }
                }
            }
             Spacer(modifier = Modifier.height(16.dp))
         }
         
         LazyColumn(contentPadding = PaddingValues(bottom = 80.dp)) {
             items(otherChannels) { channel ->
                val otherId = channel.participants.find { it != currentUser?.uid } ?: ""
                val name = channel.participantNames[otherId] ?: "Unknown"
                val photo = channel.participantPhotos[otherId] ?: ""
                val unread = channel.unreadCounts[currentUser?.uid] ?: 0
                
                var showMenu by remember { mutableStateOf(false) }
                
                Box {
                    ConversationItemView(
                        name = name,
                        message = channel.lastMessage,
                        time = formatTimestamp(channel.lastMessageTimestamp),
                        photoUrl = photo,
                        unreadCount = unread,
                        onClick = { onChatClick(otherId) },
                        onLongClick = { showMenu = true }
                    )
                    
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        DropdownMenuItem(
                            text = { Text("Pin Chat") },
                            onClick = { scope.launch { repository.pinChat(channel.id, currentUser?.uid ?: "", true) }; showMenu = false },
                            leadingIcon = { Icon(Icons.Default.Star, null) }
                        )
                         DropdownMenuItem(
                            text = { Text("Archive Chat") },
                            onClick = { scope.launch { repository.archiveChat(channel.id, currentUser?.uid ?: "", true) }; showMenu = false },
                            leadingIcon = { Icon(Icons.Default.Delete, null) }
                        )
                    }
                }
             }
         }
    }
}


// Reuse Components from previous files (re-implemented here to be self-contained in this Hub)

@Composable
fun PinnedChatCircle(name: String, photoUrl: String, onClick: () -> Unit) {
     Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable(onClick = onClick)) {
        if (photoUrl.isNotBlank()) {
            AsyncImage(model = photoUrl, contentDescription = null, modifier = Modifier.size(56.dp).clip(CircleShape), contentScale = ContentScale.Crop)
        } else {
             Box(modifier = Modifier.size(56.dp).clip(CircleShape).background(Color(0xFFEEEEEE)), contentAlignment = Alignment.Center) {
                 Text(name.take(1).uppercase(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
             }
        }
        Text(name.split(" ").first(), style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ConversationItemView(name: String, message: String, time: String, photoUrl: String, unreadCount: Int, onClick: () -> Unit, onLongClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().combinedClickable(onClick = onClick, onLongClick = onLongClick).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
         if (photoUrl.isNotBlank()) {
            AsyncImage(model = photoUrl, contentDescription = null, modifier = Modifier.size(50.dp).clip(CircleShape), contentScale = ContentScale.Crop)
        } else {
             Box(modifier = Modifier.size(50.dp).clip(CircleShape).background(Color(0xFFEEEEEE)), contentAlignment = Alignment.Center) {
                 Text(name.take(1).uppercase(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
             }
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text(name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                Text(time, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text(message, style = MaterialTheme.typography.bodyMedium, color = if (unreadCount > 0) Color.Black else Color.Gray, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (unreadCount > 0) {
                     Box(modifier = Modifier.size(20.dp).background(AtmiyaSecondary, CircleShape), contentAlignment = Alignment.Center) {
                        Text(unreadCount.toString(), color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}



@Composable
fun RequestsListContent(
    requests: List<ConnectionRequest>,
    onAccept: (ConnectionRequest) -> Unit,
    onIgnore: (ConnectionRequest) -> Unit,
    scope: kotlinx.coroutines.CoroutineScope,
    repository: FirestoreRepository
) {
    if (requests.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("No pending requests") }
    } else {
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            items(requests) { req ->
                Card(colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(2.dp)) {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                           AsyncImage(model = req.senderPhotoUrl, contentDescription = null, modifier = Modifier.size(50.dp).clip(CircleShape).background(Color.Gray), contentScale = ContentScale.Crop)
                           Spacer(modifier = Modifier.width(12.dp))
                           Column {
                               Text(req.senderName, fontWeight = FontWeight.Bold)
                               Text(req.senderRole, style = MaterialTheme.typography.bodySmall)
                           }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { onAccept(req) }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = AtmiyaSecondary)) { Text("Accept") }
                            OutlinedButton(onClick = { onIgnore(req) }, modifier = Modifier.weight(1f)) { Text("Ignore") }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NetworkListContent(connections: List<ConnectionRequest>, currentUserId: String, onChatClick: (String, String) -> Unit) {
    if (connections.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("No connections yet") }
    } else {
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
             items(connections) { conn ->
                 val isIncoming = conn.receiverId == currentUserId
                 val name = if (isIncoming) conn.senderName else "Connected User" 
                 val role = if (isIncoming) conn.senderRole else ""
                 val photo = if (isIncoming) conn.senderPhotoUrl else "https://via.placeholder.com/50" // Placeholder
                 val otherId = if (isIncoming) conn.senderId else conn.receiverId
                 
                 Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFF9F9F9))) {
                     Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                         AsyncImage(model = photo, contentDescription = null, modifier = Modifier.size(50.dp).clip(CircleShape).background(Color.Gray), contentScale = ContentScale.Crop)
                         Spacer(modifier = Modifier.width(12.dp))
                         Column(Modifier.weight(1f)) {
                             Text(name, fontWeight = FontWeight.Bold)
                             Text(role, style = MaterialTheme.typography.bodySmall)
                         }
                         IconButton(onClick = { onChatClick(otherId, name) }) {
                             Icon(Icons.Default.Email, contentDescription = "Chat", tint = AtmiyaSecondary)
                         }
                     }
                 }
             }
        }
    }
}

private fun formatTimestamp(timestamp: com.google.firebase.Timestamp?): String {
    if (timestamp == null) return ""
    val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
    return sdf.format(timestamp.toDate())
}
