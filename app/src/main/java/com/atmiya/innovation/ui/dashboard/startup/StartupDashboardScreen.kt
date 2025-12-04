package com.atmiya.innovation.ui.dashboard.startup

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.atmiya.innovation.repository.FirestoreRepository
import com.atmiya.innovation.ui.components.SoftCard
import com.atmiya.innovation.ui.theme.AtmiyaPrimary
import com.atmiya.innovation.ui.theme.AtmiyaSecondary
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

@Composable
fun StartupDashboardScreen(
    isTabVisible: Boolean = true,
    onNavigate: (String) -> Unit
) {
    val viewModel = remember { StartupDashboardViewModel() }
    val repository = remember { FirestoreRepository() }
    val auth = FirebaseAuth.getInstance()
    
    // Collect states from ViewModel
    val videosState by viewModel.featuredVideos.collectAsState()
    val eventsState by viewModel.aifEvents.collectAsState()
    val eventDebugInfo by viewModel.eventDebugInfo.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val fundingCalls by viewModel.fundingCalls.collectAsState()
    
    var userName by remember { mutableStateOf("Startup") }
    var userPhotoUrl by remember { mutableStateOf<String?>(null) }
    
    // Track scroll state to determine if video is visible
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    
    // Calculate if video is visible. 
    // In new layout, Video is item 2 (Header=0, Search=1, Video=2).
    val isVideoVisible by remember {
        derivedStateOf {
            val firstVisible = listState.firstVisibleItemIndex
            firstVisible <= 2
        }
    }
    
    LaunchedEffect(Unit) {
        val user = auth.currentUser
        if (user != null) {
            try {
                val userProfile = repository.getUser(user.uid)
                userName = userProfile?.name ?: "Startup User"
                userPhotoUrl = userProfile?.profilePhotoUrl
            } catch (e: Exception) {
                android.util.Log.e("StartupDashboard", "Error fetching user", e)
            }
        }
    }
    
    SwipeRefresh(
        state = rememberSwipeRefreshState(isRefreshing = isRefreshing),
        onRefresh = { viewModel.refresh() }
    ) {
        Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) { 
            
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = listState,
                contentPadding = PaddingValues(bottom = 100.dp)
            ) {
                // 1. Header & Search
                item {
                    DashboardTopSection(
                        userName = userName,
                        userPhotoUrl = userPhotoUrl,
                        onNavigate = onNavigate
                    )
                }
                
                // 3. Featured Videos (Hero Slider)
                item {
                    Column(modifier = Modifier.padding(top = 0.dp)) { // Adjust for overlap
                        PaddingTitle(title = "Featured Videos")
                        HeroVideoSlider(
                            videosState = videosState,
                            isVisible = isTabVisible && isVideoVisible,
                            onVideoClick = { videoId ->
                                onNavigate("video_detail/$videoId")
                            }
                        )
                    }
                }
                
                // 4. Netfund Utility Grid
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                    PaddingTitle(title = "Netfund Utility")
                    
                    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                        // Row 1
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            GridCard(
                                title = "Funding Calls",
                                subtitle = "${fundingCalls.size} Active",
                                icon = Icons.Default.AttachMoney,
                                color = Color(0xFFE3F2FD), // Light Blue
                                iconColor = Color(0xFF1565C0),
                                modifier = Modifier.weight(1f),
                                onClick = { onNavigate("funding") }
                            )
                            GridCard(
                                title = "My Applications",
                                subtitle = "Track Status",
                                icon = Icons.Default.Assignment,
                                color = Color(0xFFE8F5E9), // Light Green
                                iconColor = Color(0xFF2E7D32),
                                modifier = Modifier.weight(1f),
                                onClick = { onNavigate("my_applications") }
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        // Row 2
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            GridCard(
                                title = "Investors",
                                subtitle = "Connect",
                                icon = Icons.Default.TrendingUp,
                                color = Color(0xFFFFF3E0), // Light Orange
                                iconColor = Color(0xFFEF6C00),
                                modifier = Modifier.weight(1f),
                                onClick = { onNavigate("network") }
                            )
                            GridCard(
                                title = "Mentors",
                                subtitle = "Get Guidance",
                                icon = Icons.Default.School,
                                color = Color(0xFFF3E5F5), // Light Purple
                                iconColor = Color(0xFF7B1FA2),
                                modifier = Modifier.weight(1f),
                                onClick = { onNavigate("network") }
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                         // Row 3
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            GridCard(
                                title = "Events",
                                subtitle = "${if (eventsState is StartupDashboardViewModel.UiState.Success) (eventsState as StartupDashboardViewModel.UiState.Success).data.size else 0} Upcoming",
                                icon = Icons.Default.Event,
                                color = Color(0xFFFFEBEE), // Light Red
                                iconColor = Color(0xFFC62828),
                                modifier = Modifier.weight(1f),
                                onClick = { /* TODO: Events List */ }
                            )
                            GridCard(
                                title = "Profile",
                                subtitle = "Manage",
                                icon = Icons.Default.Person,
                                color = Color(0xFFECEFF1), // Light Grey
                                iconColor = Color(0xFF455A64),
                                modifier = Modifier.weight(1f),
                                onClick = { onNavigate("profile_screen") }
                            )
                        }
                    }
                }
                
                // 5. AIF Events Section (Horizontal List)
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                    PaddingTitle(title = "Upcoming Events")
                    AIFEventsSection(
                        eventsState = eventsState,
                        debugInfo = eventDebugInfo,
                        onEventClick = { eventId ->
                            onNavigate("event_detail/$eventId")
                        }
                    )
                }
                
                // 6. Government Schemes Section
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                    PaddingTitle(title = "Government Schemes & Policies")
                    GovernmentSchemesSection()
                }

                // 7. Incubation Centers Section
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                    PaddingTitle(title = "Incubation Support")
                    IncubationCentersSection()
                }

                item {
                    Spacer(modifier = Modifier.height(100.dp))
                }
            }
        }
    }
}

@Composable
fun PaddingTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
    )
}

@Composable
fun GridCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    color: Color,
    iconColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .height(110.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(color),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(20.dp)
                )
            }
            
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

