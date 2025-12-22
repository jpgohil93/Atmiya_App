package com.atmiya.innovation.ui.dashboard.network

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.widget.Toast
import coil.compose.AsyncImage
import com.atmiya.innovation.data.*
import com.atmiya.innovation.repository.FirestoreRepository
import com.atmiya.innovation.ui.components.UserAvatar
import com.atmiya.innovation.ui.theme.AtmiyaPrimary
import com.atmiya.innovation.ui.theme.AtmiyaSecondary
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.util.*

// --- Models for UI ---
data class NetworkUserItem(
    val uid: String,
    val name: String, // Main display name (Founder Name for Investors viewing Startups)
    val role: String, // "startup", "investor", "mentor"
    val photoUrl: String?,
    val description: String, // Startup Name or Firm Name
    val subDescription: String,
    val connectionStatus: String = "none", // "connected", "sent", "received", "none"
    val requestId: String? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetworkHubScreen(
    onBack: () -> Unit,
    onNavigateToProfile: (String, String) -> Unit, // userId, role
    initialFilter: String = "All"
) {
    val repository = remember { FirestoreRepository() }
    val auth = FirebaseAuth.getInstance()
    val currentUser = auth.currentUser
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    
    // -- Filter State --
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf(initialFilter) } // "All", "Requests", "Startup", "Investor", "Mentor"
    
    // -- Data States --
    // -- Data States (Real-time) --
    val startups by remember { repository.getStartupsFlow() }.collectAsState(initial = emptyList())
    val investors by remember { repository.getInvestorsFlow() }.collectAsState(initial = emptyList())
    val mentors by remember { repository.getMentorsFlow() }.collectAsState(initial = emptyList())
    
    val sentRequestsFlow = remember(currentUser) { 
        if (currentUser != null) repository.getSentConnectionRequestsFlow(currentUser.uid) else kotlinx.coroutines.flow.flowOf(emptyList()) 
    }
    val sentRequests by sentRequestsFlow.collectAsState(initial = emptyList())

    val acceptedFlow = remember(currentUser) {
        if (currentUser != null) repository.getAcceptedConnectionsFlow(currentUser.uid) else kotlinx.coroutines.flow.flowOf(emptyList())
    }
    val acceptedConnections by acceptedFlow.collectAsState(initial = emptyList())
    
    var currentUserRole by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    
    // Optimistic UI State
    var pendingRequestIds by remember { mutableStateOf(emptySet<String>()) }

    // Fetch Role (One-shot)
    LaunchedEffect(currentUser) {
        if (currentUser != null) {
             val user = repository.getUser(currentUser.uid)
             currentUserRole = user?.role ?: ""
             isLoading = false // Simple loading toggle
        }
    }
    
    // Listen to Incoming Requests
    val incomingFlow = remember(currentUser) {
         if (currentUser != null) repository.getIncomingConnectionRequests(currentUser.uid) else kotlinx.coroutines.flow.flowOf(emptyList())
    }
    val incomingRequests by incomingFlow.collectAsState(initial = emptyList())

    // Compute All Users
    val allNetworkUsers = remember(startups, investors, mentors, sentRequests, acceptedConnections, currentUserRole) {
        if (currentUser == null) return@remember emptyList<NetworkUserItem>()
        
        val list = mutableListOf<NetworkUserItem>()
        val isInvestorViewer = currentUserRole == "investor"

        startups.forEach { 
            val displayName = if (it.founderNames.isNotBlank()) it.founderNames else it.startupName
            val displayDesc = if (it.founderNames.isNotBlank()) it.startupName else it.sector
            list.add(NetworkUserItem(it.uid, displayName, "startup", it.logoUrl, displayDesc, it.stage)) 
        }
        investors.forEach { 
            list.add(NetworkUserItem(it.uid, it.name, "investor", it.profilePhotoUrl, it.firmName, it.investmentType)) 
        }
        mentors.forEach { 
            list.add(NetworkUserItem(it.uid, it.name, "mentor", it.profilePhotoUrl, it.title, it.organization)) 
        }

        // Map Status
        val statusMap = mutableMapOf<String, Pair<String, String?>>()
        acceptedConnections.forEach { 
            val otherId = if (it.senderId == currentUser.uid) it.receiverId else it.senderId
            statusMap[otherId] = "connected" to it.id
        }
        sentRequests.forEach { 
            statusMap[it.receiverId] = "sent" to it.id
        }
        
        list.map { user ->
            val (status, reqId) = statusMap[user.uid] ?: ("none" to null)
            user.copy(connectionStatus = status, requestId = reqId)
        }.filter { it.uid != currentUser.uid }
    }

    val isDark = androidx.compose.foundation.isSystemInDarkTheme()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Column(modifier = Modifier.background(MaterialTheme.colorScheme.surface)) {
                CenterAlignedTopAppBar(
                    title = { Text("My Network", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onSurface)
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
                )
                
                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search network...", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedBorderColor = Color.Transparent,
                        focusedBorderColor = AtmiyaPrimary,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                    ),
                    singleLine = true
                )

                // Dedicated Requests Entry
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { selectedFilter = "Requests" },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                             Icon(Icons.Default.Notifications, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        }
                        
                        Spacer(modifier = Modifier.width(16.dp))
                        
                        Text(
                            text = "Connection Requests", 
                            style = MaterialTheme.typography.titleSmall, 
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        
                        // Counter
                        if (incomingRequests.isNotEmpty()) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp) // Fixed circle size
                                    .clip(CircleShape)
                                    .background(Color(0xFFEF4444)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${incomingRequests.size}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        } else {
                             Text(
                                "0",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Medium
                             )
                        }
                        
                        Spacer(modifier = Modifier.width(12.dp))
                        
                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                // Filter Chips
                val filterOptions = listOf("All", "Startup", "Investor", "Mentor")
                
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filterOptions) { filter ->
                        val isSelected = selectedFilter == filter
                        
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedFilter = filter },
                            label = { Text(filter) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                            )
                        )
                    }
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            if (isLoading) {
                 Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            } else {
                if (selectedFilter == "Requests") {
                    RequestsListContent(
                        requests = incomingRequests,
                        onAccept = { req -> scope.launch { repository.acceptConnectionRequest(req.id) } },
                        onIgnore = { req -> scope.launch { repository.declineConnectionRequest(req.id) } },
                        currentUserId = currentUser?.uid ?: ""
                    )
                } else {
                    // Filter Main List
                    val filteredUsers = remember(allNetworkUsers, searchQuery, selectedFilter) {
                        allNetworkUsers.filter { user ->
                            val matchesSearch = user.name.contains(searchQuery, ignoreCase = true) || 
                                                user.description.contains(searchQuery, ignoreCase = true)
                            val baseFilter = selectedFilter
                            
                            val matchesRole = when(baseFilter) {
                                "All" -> true
                                "Requests" -> false // Logic handled by if-else above, but safe guard
                                "Startup" -> user.role == "startup"
                                "Investor" -> user.role == "investor"
                                "Mentor" -> user.role == "mentor"
                                else -> true
                            }
                            matchesSearch && matchesRole
                        }
                    }
                    
                    val connectedUsers = filteredUsers.filter { it.connectionStatus == "connected" }
                    val discoverUsers = filteredUsers.filter { it.connectionStatus != "connected" } // Sent + None

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        if (connectedUsers.isNotEmpty()) {
                            item { SectionHeader("My Connections (${connectedUsers.size})") }
                            items(connectedUsers) { user ->
                                // For connected: Just "View Profile"
                                NetworkUserCard(
                                    user = user, 
                                    onPrimaryAction = { onNavigateToProfile(user.uid, user.role) }, 
                                    primaryLabel = "View Profile",
                                    isPrimaryEnabled = true,
                                    onSecondaryAction = null,
                                    secondaryLabel = null,
                                    isInvestorViewer = currentUserRole == "investor"
                                )
                            }
                        }
                        
                        if (discoverUsers.isNotEmpty()) {
                            item { SectionHeader("Discover & Connect") }
                            items(discoverUsers) { user ->
                                val isSent = user.connectionStatus == "sent" || pendingRequestIds.contains(user.uid)
                                
                                // For Discover/Sent: 
                                // Primary: "Connect Now" or "Request Pending"
                                // Secondary: "View Profile"
                                
                                NetworkUserCard(
                                    user = user, 
                                    onPrimaryAction = { 
                                        if (isSent) {
                                            // Withdraw Logic
                                            if (user.requestId != null) {
                                                scope.launch {
                                                    try {
                                                        repository.cancelConnectionRequest(user.requestId)
                                                        Toast.makeText(context, "Request withdrawn", Toast.LENGTH_SHORT).show()
                                                    } catch(e: Exception) {
                                                        Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                            } else {
                                                // If it was optimistic (in pendingRequestIds but not yet confirmed with ID), just remove from local state
                                                 pendingRequestIds = pendingRequestIds - user.uid
                                            }
                                        } else {
                                            // Connect Logic
                                            // Optimistic Update
                                            pendingRequestIds = pendingRequestIds + user.uid
                                            
                                            scope.launch {
                                                try {
                                                    val me = repository.getUser(currentUser?.uid ?: "")
                                                    if (me != null) {
                                                       repository.sendConnectionRequest(
                                                           sender = me,
                                                           receiverId = user.uid,
                                                           receiverName = user.name,
                                                           receiverRole = user.role,
                                                           receiverPhotoUrl = user.photoUrl
                                                       )
                                                       Toast.makeText(context, "Request sent to ${user.name}", Toast.LENGTH_SHORT).show()
                                                    }
                                                } catch(e: Exception) {
                                                    // Revert optimistic on error
                                                    pendingRequestIds = pendingRequestIds - user.uid
                                                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        }
                                    },
                                    primaryLabel = if (isSent) "Withdraw Request" else "Connect Now",
                                    isPrimaryEnabled = true, // Always enabled so user can withdraw
                                    onSecondaryAction = { onNavigateToProfile(user.uid, user.role) },
                                    secondaryLabel = "View Profile",
                                    isInvestorViewer = currentUserRole == "investor"
                                )
                            }
                        }
                        
                        if (connectedUsers.isEmpty() && discoverUsers.isEmpty()) {
                            item {
                                Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                    Text("No users found.", color = Color.Gray)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(vertical = 4.dp)
    )
}

@Composable
fun NetworkUserCard(
    user: NetworkUserItem,
    onPrimaryAction: () -> Unit,
    primaryLabel: String,
    isPrimaryEnabled: Boolean = true,
    onSecondaryAction: (() -> Unit)? = null,
    secondaryLabel: String? = null,
    isInvestorViewer: Boolean = false
) {
    val repository = remember { FirestoreRepository() }
    var finalName by remember(user) { mutableStateOf(user.name) }
    var finalDesc by remember(user) { mutableStateOf(user.description) }
    
    LaunchedEffect(user, isInvestorViewer) {
         if (isInvestorViewer && user.role == "startup") {
              val u = repository.getUser(user.uid)
              if (u != null && u.name.isNotBlank() && !u.name.equals(user.name, ignoreCase=true)) {
                   finalDesc = "${user.name} • ${user.description}"
                   finalName = u.name
              }
         }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = 2.dp, shape = RoundedCornerShape(16.dp), spotColor = Color(0x1A000000)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Photo
                UserAvatar(
                    model = user.photoUrl,
                    name = finalName,
                    modifier = Modifier.size(56.dp),
                    size = 56.dp
                )
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(finalName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                    if (finalDesc.isNotBlank()) {
                         Text(finalDesc, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    
                    // Role Tag
                    val (roleColor, roleBg) = when(user.role.lowercase()) {
                        "investor" -> Color(0xFF0284C7) to Color(0xFFE0F2FE)
                        "mentor" -> Color(0xFF7E22CE) to Color(0xFFF3E8FF)
                        else -> Color(0xFF15803D) to Color(0xFFDCFCE7)
                    }
                    
                    Surface(
                        color = roleBg,
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Text(
                            text = user.role.capitalize(Locale.ROOT),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = roleColor,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                horizontalArrangement = if (onSecondaryAction == null) Arrangement.Center else Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Secondary Button (View Profile) - Optional
                if (onSecondaryAction != null && secondaryLabel != null) {
                    OutlinedButton(
                        onClick = onSecondaryAction,
                        shape = RoundedCornerShape(50),
                        modifier = Modifier.weight(1f).height(40.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.onSurface
                        )
                    ) {
                        Text(secondaryLabel)
                    }
                }
                
                // Primary Button (Connect / Pending / View Profile)
                val isViewProfile = primaryLabel.equals("View Profile", ignoreCase = true)
                
                if (isViewProfile) {
                     OutlinedButton(
                        onClick = onPrimaryAction,
                        enabled = isPrimaryEnabled,
                        shape = RoundedCornerShape(50),
                        modifier = if (onSecondaryAction == null) Modifier.fillMaxWidth(0.5f).height(40.dp) else Modifier.weight(1f).height(40.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.onSurface
                        )
                    ) {
                        Text(primaryLabel)
                    }
                } else {
                    Button(
                        onClick = onPrimaryAction,
                        enabled = isPrimaryEnabled,
                        shape = RoundedCornerShape(50),
                        modifier = if (onSecondaryAction == null) Modifier.fillMaxWidth(0.5f).height(40.dp) else Modifier.weight(1f).height(40.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isPrimaryEnabled) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (isPrimaryEnabled) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    ) {
                        Text(primaryLabel)
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
    currentUserId: String
) {
    if (requests.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { 
            Text("No pending requests", color = MaterialTheme.colorScheme.onSurfaceVariant) 
        }
    } else {
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            items(requests) { req ->
                Card(
                    modifier = Modifier.fillMaxWidth().shadow(2.dp, RoundedCornerShape(16.dp)),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                           UserAvatar(
                               model = req.senderPhotoUrl,
                               name = req.senderName,
                               modifier = Modifier.size(50.dp),
                               size = 50.dp
                           )
                           Spacer(modifier = Modifier.width(12.dp))
                           Column {
                               Text(req.senderName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                               Text(req.senderRole.capitalize(Locale.ROOT), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                           }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Button(
                                onClick = { onAccept(req) }, 
                                modifier = Modifier.weight(1f), 
                                colors = ButtonDefaults.buttonColors(containerColor = AtmiyaSecondary),
                                shape = RoundedCornerShape(50)
                            ) { Text("Accept") }
                            
                            OutlinedButton(
                                onClick = { onIgnore(req) }, 
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(50),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.onSurface
                                )
                            ) { Text("Ignore") }
                        }
                    }
                }
            }
        }
    }
}
