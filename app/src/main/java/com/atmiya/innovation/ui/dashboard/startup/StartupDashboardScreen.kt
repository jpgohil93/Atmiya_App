package com.atmiya.innovation.ui.dashboard.startup

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape

import androidx.compose.foundation.border
import compose.icons.TablerIcons
import compose.icons.tablericons.InfoCircle
import compose.icons.tablericons.CalendarEvent
import compose.icons.tablericons.School
import compose.icons.tablericons.Search
import compose.icons.tablericons.Building
import compose.icons.tablericons.CurrencyRupee
import compose.icons.tablericons.Bulb
import compose.icons.tablericons.TrendingUp
import compose.icons.tablericons.Leaf
import compose.icons.tablericons.ShieldCheck
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.atmiya.innovation.ui.dashboard.news.DashboardNewsSection
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import coil.compose.AsyncImage
import com.atmiya.innovation.R
import com.atmiya.innovation.ui.generator.IdeaGeneratorEntryCard
import com.atmiya.innovation.data.FundingCall
import com.atmiya.innovation.repository.FirestoreRepository
import com.atmiya.innovation.ui.components.SoftCard
import com.atmiya.innovation.ui.components.ViralGifBanner
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
    val viralVideoUrl by viewModel.viralVideoUrl.collectAsState()
    
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
                contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp)
            ) {
                // 1. Header is now outside lazy column or handled by scaffold, 
                // but since we want it scrolling or fixed? 
                // The prompt says "The header of the app should be same for the dashaborad and wall screen". 
                // Wall screen has fixed header. Let's make it fixed here too by using Scaffold if possible, 
                // but since StartupDashboardScreen is a screen content, let's just put it at top if not using Scaffold here.
                // Wait, DashboardScreen calls StartupDashboardScreen. 
                // To match WallScreen structure, we should probably add Scaffold with TopBar here or in DashboardScreen.
                // However, modifying DashboardScreen is complex due to Pager.
                // Simplest is to put CommonTopBar at the top of LazyColumn (scrollable) or Box (fixed).
                // WallScreen has it fixed. Let's try to simulate fixed if possible, or just scrollable for now.
                // Actually, let's just make it part of the content for now to avoid major refactoring of DashboardScreen pager,
                // OR we can wrap this screen in a Scaffold.
                
                // Let's use Scaffold here to have a fixed top bar like WallScreen
                
                // 2. Search Box (Now below header)
                // 2. Search Box removed as per request

                
                // 3. Featured Videos (Hero Slider)
                item {
                    com.atmiya.innovation.ui.dashboard.SharedDashboardHeader(
                        videosState = videosState,
                        isVisible = isTabVisible && isVideoVisible,
                        onVideoClick = { videoId ->
                            onNavigate("video_detail/$videoId")
                        }
                    )
                }
                
                // Idea Generator Entry
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    IdeaGeneratorEntryCard(
                        onClick = { onNavigate("idea_generator") }
                    )
                }
                
                // 4. Accelerate Your Growth
                item {
                    com.atmiya.innovation.ui.dashboard.SharedGrowthSection(
                        onNavigate = onNavigate,
                        partnerCard = {
                            com.atmiya.innovation.ui.dashboard.DashboardCard(
                                title = "Funding Calls",
                                subtitle = "Serve funds & grants",
                                modifier = Modifier.weight(1f),
                                imageResId = R.drawable.ic_funding_calls,
                                onClick = { onNavigate("funding_calls_list") }
                            )
                        }
                    )
                    
                    Column(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Row 2 (Now Row 1 of Grid)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            com.atmiya.innovation.ui.dashboard.DashboardCard(
                                title = "Mentors",
                                subtitle = "Engagement mentors",
                                modifier = Modifier.weight(1f),
                                imageResId = R.drawable.ic_mentors,
                                onClick = { onNavigate("mentors_list") }
                            )
                            
                            com.atmiya.innovation.ui.dashboard.DashboardCard(
                                title = "Investors",
                                subtitle = "Connect with investors",
                                modifier = Modifier.weight(1f),
                                imageResId = R.drawable.ic_investors,
                                onClick = { onNavigate("investors_list") }
                            )
                        }
                        
                        // Row 3
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            com.atmiya.innovation.ui.dashboard.DashboardCard(
                                title = "Incubators",
                                subtitle = "Growth incubating",
                                modifier = Modifier.weight(1f),
                                imageResId = R.drawable.ic_incubators,
                                onClick = { onNavigate("incubators_list") }
                            )
                            
                            com.atmiya.innovation.ui.dashboard.DashboardCard(
                                title = "Governance",
                                subtitle = "Government Policy",
                                modifier = Modifier.weight(1f),
                                imageResId = R.drawable.ic_governance,
                                onClick = { onNavigate("governance") }
                            )
                        }

                        // Row 4
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            com.atmiya.innovation.ui.dashboard.DashboardCard(
                                title = "Runway Calculator",
                                subtitle = "Survival & Planning",
                                modifier = Modifier.weight(1f),
                                imageResId = R.drawable.ic_runway_calculator,
                                onClick = { onNavigate("runway_calculator") }
                            )
                            
                            com.atmiya.innovation.ui.dashboard.DashboardCard(
                                title = "Pitch Generator",
                                subtitle = "AI-powered drafts",
                                modifier = Modifier.weight(1f),
                                imageResId = R.drawable.ic_outreach,
                                onClick = { onNavigate("outreach_generator") }
                            )
                        }
                    }
                }
                
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                    DashboardNewsSection(
                        onViewAllClick = { onNavigate("news_list") },
                        onNewsClick = { url -> 
                             val encodedUrl = URLEncoder.encode(url, StandardCharsets.UTF_8.toString())
                             onNavigate("news_detail/$encodedUrl")
                        }
                    )
                }

                if (viralVideoUrl != null) {
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .wrapContentHeight()
                                .background(Color.Transparent)
                        ) {
                            ViralGifBanner(
                                gifUrl = viralVideoUrl!!,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(100.dp))
                }
            }
        }
    }
}


