package com.atmiya.innovation.ui.dashboard
import androidx.compose.ui.zIndex
import androidx.compose.ui.input.nestedscroll.nestedScroll

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.foundation.layout.width // Added
import compose.icons.TablerIcons
import compose.icons.tablericons.Home
import compose.icons.tablericons.Users
import compose.icons.tablericons.Building
import compose.icons.tablericons.Bell
// Removed unused import
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
import com.atmiya.innovation.ui.components.CommonTopBar
import androidx.compose.ui.platform.LocalDensity 
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime

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
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.atmiya.innovation.ui.funding.FundingCallsScreen
import com.atmiya.innovation.ui.dashboard.listing.EventsListingScreen
import com.atmiya.innovation.ui.dashboard.listing.StartupListingScreen
import com.atmiya.innovation.ui.dashboard.listing.GovernanceListingScreen
import com.atmiya.innovation.ui.dashboard.listing.IncubatorListingScreen


import com.atmiya.innovation.ui.funding.FundingCallDetailScreen
import com.atmiya.innovation.ui.funding.CreateFundingCallScreen
import com.atmiya.innovation.ui.dashboard.startup.FeaturedVideoDetailScreen
import com.atmiya.innovation.ui.event.EventDetailScreen
import com.atmiya.innovation.ui.dashboard.MentorDetailScreen
// import com.atmiya.innovation.ui.dashboard.ProfileDetailScreen // REMOVED: does not exist
import com.atmiya.innovation.ui.dashboard.WallPostDetailScreen
import com.atmiya.innovation.ui.video.MentorVideoScreen
import com.atmiya.innovation.ui.video.MentorVideoDetailScreen
import com.atmiya.innovation.ui.dashboard.ProfileScreen
import com.atmiya.innovation.ui.dashboard.EditProfileScreen
import com.atmiya.innovation.ui.settings.SettingsScreen
import com.atmiya.innovation.ui.dashboard.news.NewsListScreen
import com.atmiya.innovation.ui.dashboard.news.NewsWebViewScreen


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
    
    val currentUserId = auth.currentUser?.uid
    val currentUser by remember(currentUserId) {
        if (currentUserId != null) {
            firestoreRepo.getUserFlow(currentUserId)
        } else {
            kotlinx.coroutines.flow.flowOf(null)
        }
    }.collectAsState(initial = null)

    val navController = rememberNavController()
    
    // Main Tabs Pager State
    // Main Tabs Pager State
    val pagerState = rememberPagerState(pageCount = { 2 }, initialPage = 0) // Default to Wall (0)
    val coroutineScope = rememberCoroutineScope()

    val items = listOf(
        Screen.Dashboard,
        Screen.Wall
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
                "connection_requests" -> "network_hub?tab=2"
                "investor_detail" -> if (startId != null) "investor_detail/$startId" else "investors_list"
                "mentor_detail" -> if (startId != null) "mentor_detail/$startId" else "mentors_list"
                "startup_detail" -> if (startId != null) "startup_detail/$startId" else "startups_list"
                else -> null
            }
            
            if (route != null) {
                if (route == "main_tabs") {
                    // Scroll to specific tab
                    val page = when(startDestination) {
                        "dashboard" -> 0
                        "funding" -> 0 // Dashboard contains funding
                        "wall" -> 1
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

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Check for Keyboard Visibility
    val density = LocalDensity.current
    val isKeyboardOpen = WindowInsets.ime.getBottom(density) > 0

    // Auto-Hide Bottom Bar Logic
    var isPillVisible by remember { mutableStateOf(true) }
    var interactionTrigger by remember { mutableLongStateOf(System.currentTimeMillis()) }

    // Timer to hide pill after 10 seconds of inactivity
    LaunchedEffect(interactionTrigger) {
        isPillVisible = true
        kotlinx.coroutines.delay(10000) // 10 seconds
        isPillVisible = false
    }

    // Detect Scroll/Interaction
    val nestedScrollConnection = remember {
        object : androidx.compose.ui.input.nestedscroll.NestedScrollConnection {
             override fun onPreScroll(available: androidx.compose.ui.geometry.Offset, source: androidx.compose.ui.input.nestedscroll.NestedScrollSource): androidx.compose.ui.geometry.Offset {
                interactionTrigger = System.currentTimeMillis()
                return super.onPreScroll(available, source)
            }
            override suspend fun onPostFling(consumed: androidx.compose.ui.unit.Velocity, available: androidx.compose.ui.unit.Velocity): androidx.compose.ui.unit.Velocity {
                interactionTrigger = System.currentTimeMillis()
                return super.onPostFling(consumed, available)
            }
        }
    }

    AppNavigationDrawer(
        drawerState = drawerState,
        user = currentUser,
        onNavigate = { route ->
             // ... existing nav logic ...
            when(route) {
                "dashboard_tab" -> { scope.launch { pagerState.scrollToPage(0) } }
                "wall_tab" -> { scope.launch { pagerState.scrollToPage(1) } }
                "profile_screen" -> { scope.launch { drawerState.close() }; navController.navigate("profile_screen") }
                "settings_screen" -> { scope.launch { drawerState.close() }; navController.navigate("settings_screen") }
                "funding_calls_list" -> { scope.launch { drawerState.close() }; navController.navigate("funding_calls_list") }
                "events_list" -> { scope.launch { drawerState.close() }; navController.navigate("events_list") }
                "network" -> { scope.launch { drawerState.close() }; navController.navigate("network_hub") }
                "connection_requests" -> { scope.launch { drawerState.close() }; navController.navigate("network_hub?tab=2") }
                else -> { scope.launch { drawerState.close() }; navController.navigate(route) }
            }
        },
        onLogout = onLogout
    ) {
        SoftScaffold(
            topBar = {
                if (currentRoute == "main_tabs") {
                   CommonTopBar(
                        onOpenDrawer = { scope.launch { drawerState.open() } },
                        onNavigateToProfile = { navController.navigate("profile_screen") },
                        onNavigateToNotifications = { navController.navigate("notifications") },
                        userPhotoUrl = currentUser?.profilePhotoUrl
                    )
                }
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .nestedScroll(nestedScrollConnection) // Attach scroll listener
            ) {
                NavHost(
                    navController = navController,
                    startDestination = "main_tabs",
                    modifier = Modifier.fillMaxSize()
                ) {
                    // ... (NavHost content remains same) ...
                    // Main Tabs Route containing the Pager
                    composable("main_tabs") {
                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier.fillMaxSize(),
                            userScrollEnabled = true
                        ) { page ->
                            when (page) {
                                0 -> {
                                    // Dashboard Logic
                                    val onNavigateToRoute: (String) -> Unit = { route ->
                                        val normalizedRoute = route.lowercase()
                                        when (normalizedRoute) {
                                            "wall" -> coroutineScope.launch { pagerState.animateScrollToPage(1) }
                                            "funding" -> navController.navigate("funding_calls_list") 
                                            // Make sure other grid items map correctly
                                            else -> navController.navigate(route)
                                        }
                                    }

                                    when(role) {
                                        "startup" -> com.atmiya.innovation.ui.dashboard.startup.StartupDashboardScreen(
                                            isTabVisible = pagerState.currentPage == 0,
                                            onNavigate = onNavigateToRoute
                                        )
                                        "investor" -> com.atmiya.innovation.ui.dashboard.home.InvestorHome(
                                            isTabVisible = pagerState.currentPage == 0,
                                            onNavigate = onNavigateToRoute
                                        )
                                        "mentor" -> com.atmiya.innovation.ui.dashboard.home.MentorHome(
                                            isTabVisible = pagerState.currentPage == 0,
                                            onNavigate = onNavigateToRoute
                                        )
                                        "admin" -> com.atmiya.innovation.ui.dashboard.home.AdminHome(onNavigate = onNavigateToRoute)
                                        else -> Text("Unknown Role", modifier = Modifier.padding(16.dp))
                                    }
                                }
                                1 -> WallScreen(
                                    onNavigateToProfile = { navController.navigate("profile_screen") },
                                    onNavigateToSettings = { navController.navigate("settings_screen") },
                                    onNavigateToDashboard = { coroutineScope.launch { pagerState.animateScrollToPage(0) } },
                                    onNavigateToFundingCall = { id -> navController.navigate("funding_call/$id") }, // Added
                                    onLogout = onLogout,
                                    onOpenDrawer = { scope.launch { drawerState.open() } }
                                )
                            }
                        }
                    }

                    // --- Inner Routes ---

                    composable("notifications") {
                        com.atmiya.innovation.ui.notifications.NotificationScreen(
                            onBack = { navController.popBackStack() },
                            onNotificationClick = { type, id ->
                                // Reuse deep link logic
                                when (type) {
                                    "wall_post" -> navController.navigate("wall_post/$id")
                                    "mentor_video" -> navController.navigate("mentor_video/$id")
                                    "funding_application" -> navController.navigate("startup_detail/$id")
                                    "connection_request" -> navController.navigate("network_hub?tab=2")
                                    else -> { /* No action */ }
                                }
                            }
                        )
                    }

                    composable("profile_screen") {
                        ProfileScreen(
                            onLogout = onLogout,
                            onEditProfile = { navController.navigate("edit_profile_screen") },
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable("edit_profile_screen") {
                        EditProfileScreen(
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable("settings_screen") {
                        SettingsScreen(
                            onBack = { navController.popBackStack() },
                            onLogout = onLogout
                        )
                    }

                    composable("idea_generator") {
                        com.atmiya.innovation.ui.generator.IdeaGeneratorScreen(
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable("saved_ideas") {
                        com.atmiya.innovation.ui.generator.SavedIdeasScreen(
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable("funding_calls_list") {
                         FundingCallsScreen(
                             role = role,
                             onNavigate = { route -> navController.navigate(route) },
                             onBack = { navController.popBackStack() }
                         )
                    }

                    composable("create_funding_call") {
                         CreateFundingCallScreen(
                             onBack = { navController.popBackStack() },
                             onCallCreated = { navController.popBackStack() }
                         )
                    }

                    composable(
                        "funding_call/{callId}",
                        arguments = listOf(navArgument("callId") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val callId = backStackEntry.arguments?.getString("callId") ?: return@composable
                        FundingCallDetailScreen(
                            callId = callId,
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable(
                        "video_detail/{videoId}",
                        arguments = listOf(navArgument("videoId") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val videoId = backStackEntry.arguments?.getString("videoId") ?: return@composable
                        FeaturedVideoDetailScreen(
                            videoId = videoId,
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable("events_list") {
                        EventsListingScreen(
                            onBack = { navController.popBackStack() },
                            onEventClick = { eventId -> navController.navigate("event_detail/$eventId") }
                        )
                    }
                    
                    composable("incubators_list") {
                        com.atmiya.innovation.ui.dashboard.listing.IncubatorListingScreen(
                            onBack = { navController.popBackStack() },
                            onItemClick = { incubator ->
                                val encodedName = java.net.URLEncoder.encode(incubator.name, java.nio.charset.StandardCharsets.UTF_8.toString())
                                val encodedLogo = java.net.URLEncoder.encode(incubator.logoUrl ?: "", java.nio.charset.StandardCharsets.UTF_8.toString())
                                val encodedWeb = java.net.URLEncoder.encode(incubator.website, java.nio.charset.StandardCharsets.UTF_8.toString())
                                val encodedCity = java.net.URLEncoder.encode(incubator.city, java.nio.charset.StandardCharsets.UTF_8.toString())
                                val encodedState = java.net.URLEncoder.encode(incubator.state, java.nio.charset.StandardCharsets.UTF_8.toString())
                                val encodedSector = java.net.URLEncoder.encode(incubator.sector, java.nio.charset.StandardCharsets.UTF_8.toString())
                                val encodedEmail = java.net.URLEncoder.encode(incubator.contactEmail, java.nio.charset.StandardCharsets.UTF_8.toString())
                                val encodedFunding = java.net.URLEncoder.encode(incubator.approvedFunding, java.nio.charset.StandardCharsets.UTF_8.toString())
                                val encodedRemaining = java.net.URLEncoder.encode(incubator.remainingFunding, java.nio.charset.StandardCharsets.UTF_8.toString())
                                
                                navController.navigate("incubator_detail/$encodedName/$encodedLogo/$encodedWeb/$encodedCity/$encodedState/$encodedSector/$encodedEmail/$encodedFunding/$encodedRemaining")
                            }
                        )
                    }

                    composable(
                        "incubator_detail/{name}/{logoUrl}/{website}/{city}/{state}/{sector}/{email}/{funding}/{remaining}",
                        arguments = listOf(
                            navArgument("name") { type = NavType.StringType },
                            navArgument("logoUrl") { type = NavType.StringType },
                            navArgument("website") { type = NavType.StringType },
                            navArgument("city") { type = NavType.StringType },
                            navArgument("state") { type = NavType.StringType },
                            navArgument("sector") { type = NavType.StringType },
                            navArgument("email") { type = NavType.StringType },
                            navArgument("funding") { type = NavType.StringType },
                            navArgument("remaining") { type = NavType.StringType }
                        )
                    ) { backStackEntry ->
                        val name = backStackEntry.arguments?.getString("name") ?: ""
                        val logoUrl = backStackEntry.arguments?.getString("logoUrl") ?: ""
                        val website = backStackEntry.arguments?.getString("website") ?: ""
                        val city = backStackEntry.arguments?.getString("city") ?: ""
                        val state = backStackEntry.arguments?.getString("state") ?: ""
                        val sector = backStackEntry.arguments?.getString("sector") ?: ""
                        val email = backStackEntry.arguments?.getString("email") ?: ""
                        val funding = backStackEntry.arguments?.getString("funding") ?: ""
                        val remaining = backStackEntry.arguments?.getString("remaining") ?: ""

                        com.atmiya.innovation.ui.dashboard.listing.IncubatorDetailScreen(
                            name = java.net.URLDecoder.decode(name, java.nio.charset.StandardCharsets.UTF_8.toString()),
                            logoUrl = java.net.URLDecoder.decode(logoUrl, java.nio.charset.StandardCharsets.UTF_8.toString()),
                            website = java.net.URLDecoder.decode(website, java.nio.charset.StandardCharsets.UTF_8.toString()),
                            city = java.net.URLDecoder.decode(city, java.nio.charset.StandardCharsets.UTF_8.toString()),
                            state = java.net.URLDecoder.decode(state, java.nio.charset.StandardCharsets.UTF_8.toString()),
                            sector = java.net.URLDecoder.decode(sector, java.nio.charset.StandardCharsets.UTF_8.toString()),
                            email = java.net.URLDecoder.decode(email, java.nio.charset.StandardCharsets.UTF_8.toString()),
                            approvedFunding = java.net.URLDecoder.decode(funding, java.nio.charset.StandardCharsets.UTF_8.toString()),
                            remainingFunding = java.net.URLDecoder.decode(remaining, java.nio.charset.StandardCharsets.UTF_8.toString()),
                            onBack = { navController.popBackStack() }
                        )
                    }
                    
                    composable("governance") {
                        com.atmiya.innovation.ui.dashboard.listing.GovernanceListingScreen(
                            onBack = { navController.popBackStack() },
                            onSchemeClick = { url, title ->
                                val encodedUrl = java.net.URLEncoder.encode(url, java.nio.charset.StandardCharsets.UTF_8.toString())
                                val encodedTitle = java.net.URLEncoder.encode(title, java.nio.charset.StandardCharsets.UTF_8.toString())
                                navController.navigate("webview?url=$encodedUrl&title=$encodedTitle")
                            }
                        )
                    }

                    composable(
                        "webview?url={url}&title={title}",
                        arguments = listOf(
                            navArgument("url") { type = NavType.StringType },
                            navArgument("title") { type = NavType.StringType; defaultValue = "Web View" }
                        )
                    ) { backStackEntry ->
                        val url = backStackEntry.arguments?.getString("url") ?: ""
                        val titleArg = backStackEntry.arguments?.getString("title") ?: "Web View"
                        val title = java.net.URLDecoder.decode(titleArg, java.nio.charset.StandardCharsets.UTF_8.toString())
                        
                        com.atmiya.innovation.ui.dashboard.web.GenericWebViewScreen(
                            url = url,
                            title = title,
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable(
                        "event_detail/{eventId}",
                        arguments = listOf(navArgument("eventId") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val eventId = backStackEntry.arguments?.getString("eventId") ?: return@composable
                        EventDetailScreen(
                            eventId = eventId,
                            onBack = { navController.popBackStack() }
                        )
                    }
                    
                    composable("mentors_list") {
                        com.atmiya.innovation.ui.dashboard.listing.MentorListingScreen(
                            onBack = { navController.popBackStack() },
                            onMentorClick = { mentorId -> navController.navigate("mentor_detail/$mentorId") },
                            onWatchVideosClick = { mentorId -> navController.navigate("mentor_videos_list/$mentorId") }
                        )
                    }

                    composable("investors_list") {
                        com.atmiya.innovation.ui.dashboard.listing.InvestorListingScreen(
                            onBack = { navController.popBackStack() },
                            onInvestorClick = { investorId -> navController.navigate("investor_detail/$investorId") }
                        )
                    }

                    composable(
                        "network_hub?tab={tab}",
                        arguments = listOf(navArgument("tab") { type = NavType.IntType; defaultValue = 0 })
                    ) { backStackEntry ->
                        val tab = backStackEntry.arguments?.getInt("tab") ?: 0
                        val initialFilter = if (tab == 2) "Requests" else "All"
                        
                        com.atmiya.innovation.ui.dashboard.network.NetworkHubScreen(
                            onBack = { navController.popBackStack() },
                            onNavigateToProfile = { userId, role ->
                                when (role.lowercase()) {
                                    "startup" -> navController.navigate("startup_detail/$userId")
                                    "investor" -> navController.navigate("investor_detail/$userId")
                                    "mentor" -> navController.navigate("mentor_detail/$userId")
                                    else -> {} 
                                }
                            },
                            initialFilter = initialFilter
                        )
                    }

                    composable("startups_list") {
                        com.atmiya.innovation.ui.dashboard.listing.StartupListingScreen(
                            onBack = { navController.popBackStack() },
                            onStartupClick = { startupId -> navController.navigate("startup_detail/$startupId") }
                        )
                    }

                    composable(
                        "startup_detail/{startupId}",
                        arguments = listOf(navArgument("startupId") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val startupId = backStackEntry.arguments?.getString("startupId") ?: return@composable
                        com.atmiya.innovation.ui.dashboard.StartupDetailScreen(
                            startupId = startupId,
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable(
                        "mentor_detail/{mentorId}",
                         arguments = listOf(navArgument("mentorId") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val mentorId = backStackEntry.arguments?.getString("mentorId") ?: return@composable
                        MentorDetailScreen(
                            mentorId = mentorId,
                            onBack = { navController.popBackStack() }
                        )
                    }
                    
                    composable(
                        "investor_detail/{investorId}",
                        arguments = listOf(navArgument("investorId") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val investorId = backStackEntry.arguments?.getString("investorId") ?: return@composable
                        // Using ProfileDetailScreen as generic detail viewer for user profiles (investors)
                        // Using InvestorDetailScreen
                        InvestorDetailScreen(
                            investorId = investorId,
                            onBack = { navController.popBackStack() }
                        )
                    }
                    
                    composable("mentor_videos") {
                         MentorVideoScreen(
                             role = role
                         )
                    }

                    composable(
                        "mentor_video/{videoId}",
                        arguments = listOf(navArgument("videoId") { type = NavType.StringType })
                    ) { backStackEntry ->
                         val videoId = backStackEntry.arguments?.getString("videoId") ?: return@composable
                         MentorVideoDetailScreen(
                             videoId = videoId,
                             onBack = { navController.popBackStack() }
                         )
                    }

                    composable(
                        "mentor_videos_list/{mentorId}",
                        arguments = listOf(navArgument("mentorId") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val mentorId = backStackEntry.arguments?.getString("mentorId")
                        MentorVideoScreen(
                            role = role,
                            mentorId = mentorId
                        )
                    }

                    composable(
                        "wall_post/{postId}",
                        arguments = listOf(navArgument("postId") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val postId = backStackEntry.arguments?.getString("postId") ?: return@composable
                        WallPostDetailScreen(
                            postId = postId,
                            onBack = { navController.popBackStack() },
                            onFundingCallClick = { callId -> navController.navigate("funding_call/$callId") }
                        )
                    }

                    composable("news_list") {
                        NewsListScreen(
                            onBack = { navController.popBackStack() },
                            onNewsClick = { url ->
                                val encodedUrl = java.net.URLEncoder.encode(url, java.nio.charset.StandardCharsets.UTF_8.toString())
                                navController.navigate("news_detail/$encodedUrl")
                            }
                        )
                    }
                    
                    composable("outreach_generator") {
                        com.atmiya.innovation.ui.generator.OutreachGeneratorScreen(
                            onBack = { navController.popBackStack() }
                        )
                    }

                     composable("funding_applications") {
                          com.atmiya.innovation.ui.dashboard.network.InvestorApplicationsScreen(
                              onBack = { navController.popBackStack() },
                              onNavigateToDetail = { startupId -> navController.navigate("startup_detail/$startupId") }
                          )
                     }

                    composable(
                        "application_detail/{callId}/{appId}",
                        arguments = listOf(
                            navArgument("callId") { type = NavType.StringType },
                            navArgument("appId") { type = NavType.StringType }
                        )
                    ) { backStackEntry ->
                        val callId = backStackEntry.arguments?.getString("callId") ?: return@composable
                        val appId = backStackEntry.arguments?.getString("appId") ?: return@composable
                        com.atmiya.innovation.ui.dashboard.network.ApplicationDetailScreen(
                            callId = callId,
                            applicationId = appId,
                            onBack = { navController.popBackStack() },
                            onViewStartupProfile = { startupId -> navController.navigate("startup_detail/$startupId") }
                        )
                    }

                     composable(
                        "news_detail/{url}",
                        arguments = listOf(navArgument("url") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val url = backStackEntry.arguments?.getString("url") ?: return@composable
                        // Decode handling might be automated by NavController if simple, but double check. 
                        // Usually encoded string is passed safely.
                        // We will decode inside the screen or just pass it to WebView.
                        // WebView loadUrl handles encoded/decoded? Best to decode.
                        val decodedUrl = java.net.URLDecoder.decode(url, java.nio.charset.StandardCharsets.UTF_8.toString())
                        NewsWebViewScreen(
                            url = decodedUrl,
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable("runway_calculator") {
                        com.atmiya.innovation.ui.dashboard.startup.RunwayCalculatorScreen(
                            onBack = { navController.popBackStack() }
                        )
                    }

                }
                
                // Bottom Bar - Only Visible on Main Tabs AND Keyboard Closed
                // Wrapped in AnimatedVisibility
                androidx.compose.animation.AnimatedVisibility(
                    visible = currentRoute == "main_tabs" && !isKeyboardOpen && isPillVisible,
                    enter = androidx.compose.animation.slideInVertically(initialOffsetY = { it * 2 }),
                    exit = androidx.compose.animation.slideOutVertically(targetOffsetY = { it * 2 }),
                    modifier = Modifier.align(Alignment.BottomCenter)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 24.dp)
                            .padding(horizontal = 48.dp) // Indent for floating look
                    ) {
                        Surface(
                            color = Color(0xFF2C2C2C), // Dark pill background
                            shape = RoundedCornerShape(50),
                            shadowElevation = 8.dp,
                            modifier = Modifier.align(Alignment.BottomCenter).height(64.dp)
                        ) {
                            TabRow(
                                selectedTabIndex = pagerState.currentPage,
                                containerColor = Color.Transparent,
                                indicator = { tabPositions ->
                                    if (pagerState.currentPage < tabPositions.size) {
                                        val currentTabPosition = tabPositions[pagerState.currentPage]
                                        Box(
                                            modifier = Modifier
                                                .tabIndicatorOffset(currentTabPosition)
                                                .padding(horizontal = 4.dp, vertical = 4.dp)
                                                .fillMaxSize()
                                                .background(AtmiyaPrimary, RoundedCornerShape(50))
                                                .zIndex(-1f)
                                        )
                                    }
                                },
                                divider = {},
                                modifier = Modifier.padding(4.dp).widthIn(max = 300.dp)
                            ) {
                                items.forEachIndexed { index, screen ->
                                    val selected = pagerState.currentPage == index
                                    Tab(
                                        selected = selected,
                                        onClick = { 
                                            // Reset timer on click too
                                            interactionTrigger = System.currentTimeMillis()
                                            coroutineScope.launch { pagerState.animateScrollToPage(index) } 
                                        },
                                        modifier = Modifier.clip(RoundedCornerShape(50)).height(56.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center
                                        ) {
                                            Icon(
                                              imageVector = screen.icon,
                                              contentDescription = screen.route,
                                              tint = if (selected) Color.White else Color.Gray,
                                              modifier = Modifier.size(28.dp) // Increased size
                                          )
                                            if (selected) {
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = screen.route,
                                                    color = Color.White,
                                                    style = MaterialTheme.typography.labelLarge,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

sealed class Screen(val route: String, val icon: ImageVector) {
    object Dashboard : Screen("Dashboard", TablerIcons.Home)
    object Wall : Screen("Wall", TablerIcons.Users) 
    object Incubators : Screen("Incubators", TablerIcons.Building) // Incubator icon
    object Governance : Screen("Governance", Icons.Filled.Gavel) // Governance icon
}





