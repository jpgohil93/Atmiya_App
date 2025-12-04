package com.atmiya.innovation.ui.dashboard

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.atmiya.innovation.ui.components.SoftScaffold
import com.atmiya.innovation.ui.components.SoftCard
import com.atmiya.innovation.ui.theme.AtmiyaPrimary
import com.atmiya.innovation.ui.theme.AtmiyaSecondary
import com.atmiya.innovation.ui.theme.AtmiyaAccent
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color

import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.filled.List
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

import androidx.compose.foundation.ExperimentalFoundationApi

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DashboardScreen(
    role: String,
    startDestination: String? = null,
    startId: String? = null,
    onLogout: () -> Unit
) {
    val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
    val firestoreRepo = remember { com.atmiya.innovation.repository.FirestoreRepository() }
    var currentUser by remember { mutableStateOf<com.atmiya.innovation.data.User?>(null) }

    LaunchedEffect(Unit) {
        val uid = auth.currentUser?.uid
        if (uid != null) {
            currentUser = firestoreRepo.getUser(uid)
        }
    }

    val navController = rememberNavController()
    
    // Main Tabs Pager State
    // Main Tabs Pager State
    val pagerState = rememberPagerState(pageCount = { 2 }, initialPage = 0) // Default to Wall (0)
    val coroutineScope = rememberCoroutineScope()

    val items = listOf(
        Screen.Wall,
        Screen.Dashboard
    )

    // Handle Deep Links
    LaunchedEffect(startDestination, startId) {
        if (startDestination != null) {
            val route = when(startDestination) {
                "funding" -> "main_tabs" // Navigate to tabs, then scroll
                "wall" -> "main_tabs"
                "profile" -> "profile_screen"
                "funding_call" -> if (startId != null) "funding_call/$startId" else "main_tabs"
                "wall_post" -> if (startId != null) "wall_post/$startId" else "main_tabs"
                "mentor_video" -> if (startId != null) "mentor_video/$startId" else "mentor_videos"
                else -> null
            }
            
            if (route != null) {
                if (route == "main_tabs") {
                    // Scroll to specific tab
                    val page = when(startDestination) {
                        "wall" -> 0
                        "dashboard" -> 1
                        "funding" -> 1 // Dashboard contains funding
                        else -> 0
                    }
                    pagerState.scrollToPage(page)
                } else {
                    navController.navigate(route)
                }
            }
        }
    }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    AppNavigationDrawer(
        drawerState = drawerState,
        user = currentUser,
        onNavigateToDashboard = { 
            scope.launch { 
                pagerState.animateScrollToPage(1) 
            }
        },
        onNavigateToProfile = { navController.navigate("profile_screen") },
        onNavigateToSettings = { navController.navigate("settings_screen") },
        onLogout = onLogout
    ) {
        SoftScaffold(
            bottomBar = {
                // Only show bottom bar on main tabs
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route
                
                // Check if we are on a route that should show the bottom bar
                val bottomBarRoutes = listOf(
                    "main_tabs", 
                    "network", 
                    "mentor_videos", 
                    "edit_profile"
                )
                
                // For dynamic routes, we check if the current route starts with the base path
                val isBottomBarVisible = currentRoute in bottomBarRoutes || 
                                         currentRoute?.startsWith("user_detail") == true ||
                                         currentRoute?.startsWith("mentor_detail") == true ||
                                         currentRoute?.startsWith("investor_detail") == true ||
                                         currentRoute?.startsWith("funding_call") == true ||
                                         currentRoute?.startsWith("wall_post") == true

                if (isBottomBarVisible) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        NavigationBar(
                            modifier = Modifier
                                .widthIn(max = 300.dp) // Reduced width for 2 items
                                .fillMaxWidth()
                                .height(70.dp), // Slightly smaller height
                            containerColor = Color.Transparent,
                            contentColor = Color.White,
                            tonalElevation = 0.dp,
                            windowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(35.dp))
                                    .background(Color.Black), // Apply background here
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                items.forEachIndexed { index, screen ->
                                    val isSelected = pagerState.currentPage == index
                                    val color = if (isSelected) AtmiyaAccent else Color.Gray
                                    
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center,
                                        modifier = Modifier
                                            .clickable {
                                                coroutineScope.launch {
                                                    pagerState.animateScrollToPage(index)
                                                }
                                            }
                                            .padding(vertical = 8.dp)
                                            .weight(1f)
                                    ) {
                                        // Icon Container
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp) // Fixed size for touch target and background
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(if (isSelected) AtmiyaAccent else Color.Transparent),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = screen.icon,
                                                contentDescription = screen.route,
                                                tint = if (isSelected) Color.White else Color.Gray,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                        
                                        Spacer(modifier = Modifier.height(4.dp))
                                        
                                        Text(
                                            text = screen.route,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = color,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        ) { paddingValues ->
            NavHost(
                navController = navController,
                startDestination = "main_tabs",
                modifier = Modifier.padding(paddingValues)
            ) {
                // Main Tabs Route containing the Pager
                composable("main_tabs") {
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize(),
                        userScrollEnabled = false
                    ) { page ->
                        when (page) {
                            0 -> WallScreen(
                                onNavigateToProfile = { navController.navigate("profile_screen") },
                                onNavigateToSettings = { navController.navigate("settings_screen") },
                                onNavigateToDashboard = { coroutineScope.launch { pagerState.animateScrollToPage(1) } },
                                onLogout = onLogout,
                                onOpenDrawer = { scope.launch { drawerState.open() } }
                            )
                            1 -> {
                                // Dashboard Logic
                                val onNavigateToRoute: (String) -> Unit = { route ->
                                    val normalizedRoute = route.lowercase()
                                    when (normalizedRoute) {
                                        "wall" -> coroutineScope.launch { pagerState.animateScrollToPage(0) }
                                        "funding" -> navController.navigate("funding_calls") // Separate route for funding
                                        else -> navController.navigate(route)
                                    }
                                }

                                when(role) {
                                    "startup" -> com.atmiya.innovation.ui.dashboard.startup.StartupDashboardScreen(
                                        isTabVisible = pagerState.currentPage == 1,
                                        onNavigate = onNavigateToRoute
                                    )
                                    "investor" -> com.atmiya.innovation.ui.dashboard.home.InvestorHome(onNavigate = onNavigateToRoute)
                                    "mentor" -> com.atmiya.innovation.ui.dashboard.home.MentorHome(onNavigate = onNavigateToRoute)
                                    "admin" -> com.atmiya.innovation.ui.dashboard.home.AdminHome(onNavigate = onNavigateToRoute)
                                    else -> Text("Unknown Role", modifier = Modifier.padding(16.dp))
                                }
                            }
                        }
                    }
                }

                // Detail Routes
                composable("settings_screen") {
                    com.atmiya.innovation.ui.settings.SettingsScreen(
                        onBack = { navController.popBackStack() },
                        onLogout = onLogout
                    )
                }



            // Detail Routes
            composable("settings") {
                com.atmiya.innovation.ui.settings.SettingsScreen(
                    onBack = { navController.popBackStack() },
                    onLogout = onLogout
                )
            }

            composable("funding_calls") {
                Column(modifier = Modifier.fillMaxSize()) {
                    com.atmiya.innovation.ui.funding.FundingCallsScreen(
                        role = role,
                        onNavigate = { route -> navController.navigate(route) },
                        onBack = { navController.popBackStack() }
                    )
                }
            }

            // ... (Rest of the routes remain mostly same)
            composable("network") { 
                NetworkScreen(
                    role = role,
                    onMentorClick = { id -> navController.navigate("mentor_detail/$id") },
                    onInvestorClick = { id -> navController.navigate("investor_detail/$id") }
                ) 
            }
            
            composable("edit_profile") {
                EditProfileScreen(onBack = { navController.popBackStack() })
            }
            
            composable("mentor_videos") { com.atmiya.innovation.ui.video.MentorVideoScreen(role) }
            
            composable("create_funding_call") {
                com.atmiya.innovation.ui.funding.CreateFundingCallScreen(
                    onBack = { navController.popBackStack() },
                    onCallCreated = { navController.popBackStack() }
                )
            }

            composable(
                "funding_apps_list/{callId}",
                arguments = listOf(androidx.navigation.navArgument("callId") { type = androidx.navigation.NavType.StringType })
            ) { backStackEntry ->
                val callId = backStackEntry.arguments?.getString("callId") ?: ""
                com.atmiya.innovation.ui.funding.FundingAppsListScreen(
                    callId = callId,
                    onBack = { navController.popBackStack() },
                    onAppClick = { appId -> navController.navigate("application_detail/$callId/$appId") }
                )
            }

            composable(
                "application_detail/{callId}/{appId}",
                arguments = listOf(
                    androidx.navigation.navArgument("callId") { type = androidx.navigation.NavType.StringType },
                    androidx.navigation.navArgument("appId") { type = androidx.navigation.NavType.StringType }
                )
            ) { backStackEntry ->
                val callId = backStackEntry.arguments?.getString("callId") ?: ""
                val appId = backStackEntry.arguments?.getString("appId") ?: ""
                com.atmiya.innovation.ui.funding.ApplicationDetailScreen(
                    callId = callId,
                    applicationId = appId,
                    onBack = { navController.popBackStack() },
                    onChatCreated = { channelId, otherUserName ->
                        navController.navigate("chat_detail/$channelId/$otherUserName")
                    }
                )
            }
            
            composable("my_applications") {
                com.atmiya.innovation.ui.funding.MyApplicationsScreen(
                    onBack = { navController.popBackStack() },
                    onChatClick = { channelId, otherUserName ->
                        navController.navigate("chat_detail/$channelId/$otherUserName")
                    }
                )
            }
            
            composable("chat_list") {
            com.atmiya.innovation.ui.chat.ChatListScreen(
                onBack = { navController.popBackStack() },
                onChatClick = { channelId, otherUserName -> 
                    navController.navigate("chat_detail/$channelId/$otherUserName") 
                }
            )
        }
        
        composable(
            "chat_detail/{channelId}/{otherUserName}",
            arguments = listOf(
                androidx.navigation.navArgument("channelId") { type = androidx.navigation.NavType.StringType },
                androidx.navigation.navArgument("otherUserName") { type = androidx.navigation.NavType.StringType }
            )
        ) { backStackEntry ->
            val channelId = backStackEntry.arguments?.getString("channelId") ?: ""
            val otherUserName = backStackEntry.arguments?.getString("otherUserName") ?: ""
            com.atmiya.innovation.ui.chat.ChatScreen(
                channelId = channelId,
                otherUserName = otherUserName,
                onBack = { navController.popBackStack() }
            )
        }          
            composable("mentor_detail/{mentorId}") { backStackEntry ->
                val mentorId = backStackEntry.arguments?.getString("mentorId") ?: return@composable
                MentorDetailScreen(mentorId = mentorId, onBack = { navController.popBackStack() })
            }
            composable("investor_detail/{investorId}") { backStackEntry ->
                val investorId = backStackEntry.arguments?.getString("investorId") ?: return@composable
                InvestorDetailScreen(investorId = investorId, onBack = { navController.popBackStack() })
            }
            
            composable("funding_call/{callId}") { backStackEntry ->
                val callId = backStackEntry.arguments?.getString("callId") ?: return@composable
                com.atmiya.innovation.ui.funding.FundingCallDetailScreen(
                    callId = callId,
                    onBack = { navController.popBackStack() }
                )
            }
            
            composable("apply_proposal/{callId}") { backStackEntry ->
                val callId = backStackEntry.arguments?.getString("callId") ?: return@composable
                com.atmiya.innovation.ui.funding.ApplyProposalScreen(
                    callId = callId,
                    onBack = { navController.popBackStack() },
                    onSuccess = { 
                        navController.popBackStack() 
                    }
                )
            }
            
            composable("wall_post/{postId}") { backStackEntry ->
                val postId = backStackEntry.arguments?.getString("postId") ?: return@composable
                com.atmiya.innovation.ui.dashboard.WallPostDetailScreen(
                    postId = postId,
                    onBack = { navController.popBackStack() }
                )
            }
            composable("mentor_video/{videoId}") { backStackEntry ->
                val videoId = backStackEntry.arguments?.getString("videoId") ?: return@composable
                com.atmiya.innovation.ui.video.MentorVideoDetailScreen(
                    videoId = videoId,
                    onBack = { navController.popBackStack() }
                )
            }
            
            composable("user_detail/{userId}") { backStackEntry ->
                val userId = backStackEntry.arguments?.getString("userId") ?: return@composable
                com.atmiya.innovation.ui.admin.UserDetailScreen(
                    userId = userId,
                    onBack = { navController.popBackStack() }
                )
            }
            
            // Verification Routes
            composable("startup_verification") {
                com.atmiya.innovation.ui.dashboard.startup.StartupVerificationScreen(
                    onBack = { navController.popBackStack() }
                )
            }
            
            composable("admin_verification") {
                com.atmiya.innovation.ui.admin.AdminVerificationScreen(
                    onBack = { navController.popBackStack() }
                )
            }
            
            // Event Detail Route
            composable("event_detail/{eventId}") { backStackEntry ->
                val eventId = backStackEntry.arguments?.getString("eventId") ?: return@composable
                com.atmiya.innovation.ui.event.EventDetailScreen(
                    eventId = eventId,
                    onBack = { navController.popBackStack() }
                )
            }
            
            // Video Detail Route - use FeaturedVideoDetailScreen for startup dashboard videos
            composable("video_detail/{videoId}") { backStackEntry ->
                val videoId = backStackEntry.arguments?.getString("videoId") ?: return@composable
                com.atmiya.innovation.ui.dashboard.startup.FeaturedVideoDetailScreen(
                    videoId = videoId,
                    onBack = { navController.popBackStack() }
                )
            }
            
            composable("profile_screen") {
                ProfileScreen(
                    onLogout = onLogout,
                    onEditProfile = { navController.navigate("edit_profile") },
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
}

sealed class Screen(val route: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    object Wall : Screen("Wall", Icons.Filled.Forum)
    object Dashboard : Screen("Dashboard", Icons.Filled.Dashboard)
}

@Composable
fun NetworkScreen(
    role: String,
    onMentorClick: (String) -> Unit,
    onInvestorClick: (String) -> Unit
) {
    Column {
        var selectedTab by androidx.compose.runtime.saveable.rememberSaveable { mutableIntStateOf(0) }
        val tabs = listOf("Investors", "Mentors")

        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = AtmiyaPrimary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            indicator = { tabPositions ->
                TabRowDefaults.Indicator(
                    Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = AtmiyaSecondary
                )
            }
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    text = { Text(title) },
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    selectedContentColor = AtmiyaSecondary,
                    unselectedContentColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.6f)
                )
            }
        }

        when (selectedTab) {
            0 -> InvestorsScreen(onInvestorClick = onInvestorClick)
            1 -> MentorsScreen(onViewProfile = onMentorClick)
        }
    }
}



