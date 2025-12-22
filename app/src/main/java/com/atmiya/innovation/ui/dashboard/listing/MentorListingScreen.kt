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
import compose.icons.tablericons.Star
import compose.icons.tablericons.PlayerPlay
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
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.painter.ColorPainter
import coil.compose.AsyncImage
import com.atmiya.innovation.data.Mentor
import com.atmiya.innovation.repository.FirestoreRepository
import com.atmiya.innovation.ui.theme.AtmiyaPrimary
import com.atmiya.innovation.ui.components.SoftScaffold // Assuming SoftScaffold is preferred, else just Scaffold
import com.atmiya.innovation.ui.components.CommonTopBar // Or just standard top bar with back navigation
import com.atmiya.innovation.ui.components.NetworkCard
import com.atmiya.innovation.ui.components.InfoRow
import com.atmiya.innovation.ui.components.PillBadge
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MentorListingScreen(
    onBack: () -> Unit,
    onMentorClick: (String) -> Unit,
    onWatchVideosClick: (String) -> Unit
) {
    val repository = remember { FirestoreRepository() }
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current

    // Auth & User
    val auth = remember { com.google.firebase.auth.FirebaseAuth.getInstance() }
    val currentUserId = auth.currentUser?.uid ?: ""
    var currentUser by remember { mutableStateOf<com.atmiya.innovation.data.User?>(null) }
    
    // Connection State
    var connectionStatusMap by remember { mutableStateOf<Map<String, String>>(emptyMap()) }

    var key by remember { mutableStateOf(0) }
    val mentorsFlow = remember(key) { repository.getMentorsFlow() }
    val mentors by mentorsFlow.collectAsState(initial = emptyList())

    // Fetch user and connections
    LaunchedEffect(currentUserId, key) {
        if (currentUserId.isNotBlank()) {
            currentUser = repository.getUser(currentUserId)
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
        }
    }

    var isRefreshing by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mentors", fontWeight = FontWeight.Bold) },
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
                items(mentors) { mentor ->
                    val status = connectionStatusMap[mentor.uid] ?: "none"
                    
                    MentorCard(
                        user = mentor, 
                        status = status,
                        onClick = { onMentorClick(mentor.uid) },
                        onWatchVideo = { onWatchVideosClick(mentor.uid) },
                        onConnect = { 
                             if (status == "none" && currentUser != null) {
                                scope.launch {
                                    try {
                                        repository.sendConnectionRequest(
                                            sender = currentUser!!,
                                            receiverId = mentor.uid,
                                            receiverName = mentor.name,
                                            receiverRole = "mentor",
                                            receiverPhotoUrl = mentor.profilePhotoUrl
                                        )
                                        connectionStatusMap = connectionStatusMap.toMutableMap().apply {
                                            put(mentor.uid, "pending")
                                        }
                                        android.widget.Toast.makeText(context, "Request sent!", android.widget.Toast.LENGTH_SHORT).show()
                                    } catch (e: Exception) {
                                         android.widget.Toast.makeText(context, e.message ?: "Error sending request", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                }
                            } else if (status == "connected") {
                                onMentorClick(mentor.uid) // Open profile if connected
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun MentorCard(
    user: Mentor, 
    status: String,
    onClick: () -> Unit,
    onWatchVideo: () -> Unit,
    onConnect: () -> Unit
) {
    val (btnText, isBtnEnabled) = when(status) {
        "connected" -> "Connected" to false // Or true to view profile
        "pending" -> "Pending" to false
        "declined" -> "Declined (Wait 24h)" to false
        else -> "Connect Now" to true
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(1.dp), 
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        shadowElevation = 4.dp,
        border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0xFFE0E0E0))
    ) {
         Column(modifier = Modifier.padding(16.dp)) {
            // Header: Image + Info (Same as before)
            Row(
                verticalAlignment = Alignment.Top,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Profile Image
                val imageModifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                
                val initials = user.name.split(" ")
                    .mapNotNull { it.firstOrNull()?.toString() }
                    .take(2)
                    .joinToString("")
                    .uppercase()

                val bgColors = listOf(
                    Color(0xFFEF5350), Color(0xFFAB47BC), Color(0xFF5C6BC0), 
                    Color(0xFF26A69A), Color(0xFF66BB6A), Color(0xFFFFA726), 
                    Color(0xFF8D6E63), Color(0xFF78909C)
                )
                val initialsBg = bgColors[user.name.hashCode().let { if (it < 0) -it else it } % bgColors.size]

                AsyncImage(
                    model = user.profilePhotoUrl ?: "",
                    contentDescription = null,
                    modifier = imageModifier,
                    contentScale = ContentScale.Crop,
                    error = androidx.compose.ui.graphics.painter.ColorPainter(initialsBg) 
                )

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = user.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${user.title}${if(user.experienceYears.isNotBlank()) ", ${user.experienceYears} Yrs Exp." else ""}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.DarkGray,
                        maxLines = 1
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (user.expertiseAreas.isNotEmpty()) {
                             PillBadge(
                                text = "${user.expertiseAreas.first()}",
                                backgroundColor = AtmiyaPrimary.copy(alpha = 0.1f),
                                contentColor = AtmiyaPrimary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Info Content
            Column(modifier = Modifier.fillMaxWidth()) {
                if (user.expertiseAreas.isNotEmpty()) {
                    InfoRow(
                        label = "Sectors",
                        value = user.expertiseAreas.take(3).joinToString(", ")
                    )
                }
                if (user.topicsToTeach.isNotEmpty()) {
                    InfoRow(
                        label = "Mentoring",
                        value = user.topicsToTeach.take(2).joinToString(", ")
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Buttons
            OutlinedButton(
                onClick = onWatchVideo,
                modifier = Modifier.fillMaxWidth().height(40.dp),
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.Black),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Black)
            ) {
                 Icon(TablerIcons.PlayerPlay, contentDescription = null, modifier = Modifier.size(16.dp))
                 Spacer(modifier = Modifier.width(8.dp))
                 Text(text = "Watch Mentor Videos", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onClick,
                    modifier = Modifier.weight(1f).height(40.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Black, 
                        contentColor = Color.White
                    )
                ) {
                    Text(text = "View Profile", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = onConnect,
                    enabled = isBtnEnabled,
                    modifier = Modifier.weight(1f).height(40.dp),
                    shape = RoundedCornerShape(8.dp),
                    border = if (isBtnEnabled) androidx.compose.foundation.BorderStroke(1.dp, Color.Black) else androidx.compose.foundation.BorderStroke(1.dp, Color.Gray),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = if (isBtnEnabled) Color.Black else Color.Gray
                    )
                ) {
                    Text(text = btnText, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
         }
    }
}

// StatBadge removed as it is no longer used in this new design (replaced by NetworkCard structure)
