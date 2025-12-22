package com.atmiya.innovation.ui.dashboard.listing

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import compose.icons.TablerIcons
import compose.icons.tablericons.ArrowLeft
import compose.icons.tablericons.InfoCircle
import compose.icons.tablericons.Home
import compose.icons.tablericons.Check // Added
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.atmiya.innovation.data.Investor
import com.atmiya.innovation.repository.FirestoreRepository
import com.atmiya.innovation.ui.theme.AtmiyaPrimary
import com.atmiya.innovation.ui.components.NetworkCard
import com.atmiya.innovation.ui.components.InfoRow
import com.atmiya.innovation.ui.components.PillBadge
import compose.icons.tablericons.CurrencyRupee

import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvestorListingScreen(
    onBack: () -> Unit,
    onInvestorClick: (String) -> Unit
) {
    val repository = remember { FirestoreRepository() }
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current
    
    // Auth & User
    val auth = remember { com.google.firebase.auth.FirebaseAuth.getInstance() }
    val currentUserId = auth.currentUser?.uid ?: ""
    var currentUser by remember { mutableStateOf<com.atmiya.innovation.data.User?>(null) }

    // Data State
    var key by remember { mutableStateOf(0) }
    val investorsFlow = remember(key) { repository.getInvestorsFlow() }
    val investors by investorsFlow.collectAsState(initial = emptyList())
    
    // Connection State
    // Map of TargetUserID -> Status ("connected", "pending_sent", "pending_received", "none")
    var connectionStatusMap by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    
    // Fetch user and connections
    LaunchedEffect(currentUserId, key) {
        if (currentUserId.isNotBlank()) {
            try {
                // 1. Get Current User (for sending requests)
                currentUser = repository.getUser(currentUserId)
                
                // 2. Build Status Map
                // This is efficient: Fetch all MY requests/connections and map them locally
                // Instead of querying for each card.
                val sentRequests = repository.getSentConnectionRequests(currentUserId)
                val connections = repository.getAcceptedConnections(currentUserId)
                val declinedRequests = repository.getDeclinedConnectionRequests(currentUserId)
                
                val newMap = mutableMapOf<String, String>()
                sentRequests.forEach { newMap[it.receiverId] = "pending" }
                connections.forEach { 
                    val partnerId = if(it.senderId == currentUserId) it.receiverId else it.senderId
                    newMap[partnerId] = "connected"
                }
                declinedRequests.forEach { newMap[it.receiverId] = "declined" }
                connectionStatusMap = newMap
            } catch (e: Exception) {
                // Prevent crash if Firestore fails (e.g. offline, permissions, indexes)
                android.util.Log.e("InvestorListing", "Error loading connection status", e)
            }
        }
    }
    
    var isRefreshing by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Investors", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(TablerIcons.ArrowLeft, contentDescription = "Back", modifier = Modifier.size(28.dp))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
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
                 items(investors) { investor ->
                     val status = connectionStatusMap[investor.uid] ?: "none"
                     
                    InvestorCard(
                        user = investor, 
                        status = status,
                        onClick = { onInvestorClick(investor.uid) },
                        onConnectClick = {
                            if (currentUser != null) {
                                scope.launch {
                                    try {
                                        repository.sendConnectionRequest(
                                            sender = currentUser!!,
                                            receiverId = investor.uid,
                                            receiverName = investor.name,
                                            receiverRole = "investor",
                                            receiverPhotoUrl = investor.profilePhotoUrl
                                        )
                                        // Update local map directly for instant feedback
                                        connectionStatusMap = connectionStatusMap.toMutableMap().apply {
                                            put(investor.uid, "pending")
                                        }
                                        android.widget.Toast.makeText(context, "Request sent!", android.widget.Toast.LENGTH_SHORT).show()
                                    } catch (e: Exception) {
                                         android.widget.Toast.makeText(context, e.message ?: "Error sending request", android.widget.Toast.LENGTH_SHORT).show()
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
fun InvestorCard(
    user: Investor, 
    status: String, // "none", "pending", "connected"
    onClick: () -> Unit,
    onConnectClick: () -> Unit
) {
    // Determine button text and state
    val (btnText, isBtnEnabled, isPrimary) = when(status) {
        "connected" -> Triple("Connected", false, false) // Or true and open profile
        "pending" -> Triple("Pending", false, false)
        "declined" -> Triple("Declined (Wait 24h)", false, false)
        else -> Triple("Connect Now", true, false)
    }

    NetworkCard(
        imageModel = user.profilePhotoUrl ?: "",
        name = user.name,
        roleOrTitle = user.firmName.ifBlank { "Independent Investor" },
        badges = {
             if (user.sectorsOfInterest.isNotEmpty()) {
                 PillBadge(
                    text = user.sectorsOfInterest.first(),
                    backgroundColor = Color(0xFFF3E5F5), // Light Purple
                    contentColor = Color(0xFF7B1FA2)
                )
            }
        },
        infoContent = {
            if (user.sectorsOfInterest.isNotEmpty()) {
                InfoRow(
                    label = "Sectors",
                    value = user.sectorsOfInterest.take(3).joinToString(", ")
                )
            } else {
                 InfoRow(label = "Sectors", value = "General")
            }
            if (user.ticketSizeMin.isNotBlank()) {
                InfoRow(
                    label = "Typical Check",
                    value = user.ticketSizeMin
                )
                if (user.ticketSizeMin.isNotBlank()) {
                    PillBadge(text = "Ticket: ${user.ticketSizeMin}")
                }
            }
             InfoRow(
                label = "Investment Type",
                value = user.investmentType.ifBlank { "Equity" }
            )
        },
        primaryButtonText = "View Profile",
        onPrimaryClick = onClick,
        secondaryButtonText = btnText,
        onSecondaryClick = onConnectClick,
        isSecondaryButtonEnabled = isBtnEnabled // Need to update NetworkCard signature if this param doesn't exist, checking...
    )
}
