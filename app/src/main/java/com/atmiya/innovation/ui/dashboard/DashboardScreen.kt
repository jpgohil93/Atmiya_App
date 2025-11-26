package com.atmiya.innovation.ui.dashboard

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.atmiya.innovation.ui.theme.AtmiyaPrimary
import com.atmiya.innovation.ui.theme.AtmiyaSecondary

@Composable
fun DashboardScreen(
    role: String,
    startDestination: String? = null
) {
    val navController = rememberNavController()
    
    LaunchedEffect(startDestination) {
        if (startDestination != null) {
            // Map simple names to routes if needed, or assume exact match
            val route = when(startDestination) {
                "funding" -> Screen.Opportunities.route
                "wall" -> Screen.Home.route
                "profile" -> Screen.Profile.route
                else -> null
            }
            
            if (route != null) {
                navController.navigate(route) {
                    popUpTo(navController.graph.findStartDestination().id) {
                        saveState = true
                    }
                    launchSingleTop = true
                    restoreState = true
                }
            }
        }
    }
    val items = listOf(
        Screen.Home,
        Screen.Network,
        Screen.Opportunities,
        Screen.Profile
    )

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = AtmiyaPrimary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination
                items.forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = null) },
                        label = { Text(screen.route) },
                        selected = currentDestination?.route == screen.route,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = AtmiyaSecondary,
                            unselectedIconColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.6f),
                            selectedTextColor = AtmiyaSecondary,
                            unselectedTextColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.6f)
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Home.route) { WallScreen() }
            composable(Screen.Network.route) { NetworkScreen(role) }
            composable(Screen.Opportunities.route) { com.atmiya.innovation.ui.funding.FundingCallsScreen(role) }
            composable(Screen.Profile.route) { ProfileScreen() }
        }
    }
}

sealed class Screen(val route: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    object Home : Screen("Home", Icons.Filled.Home)
    object Network : Screen("Network", Icons.Filled.Star)
    object Opportunities : Screen("Funding", Icons.Filled.Add) // Using Add icon as placeholder for Funding
    object Profile : Screen("Profile", Icons.Filled.Person)
}

// Placeholder Screens
// WallScreen is now in its own file

@Composable
fun NetworkScreen(role: String) {
    var showVideoLibrary by remember { mutableStateOf(false) }

    if (showVideoLibrary) {
        // Show Video Library for the selected mentor (or all for now)
        Column {
            Button(onClick = { showVideoLibrary = false }) {
                Text("Back to Mentors")
            }
            com.atmiya.innovation.ui.video.MentorVideoScreen(role = role)
        }
    } else {
        Column {
            var selectedTab by remember { mutableIntStateOf(0) }
            val tabs = listOf("Investors", "Mentors")

            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        text = { Text(title) },
                        selected = selectedTab == index,
                        onClick = { selectedTab = index }
                    )
                }
            }

            when (selectedTab) {
                0 -> InvestorsScreen()
                1 -> MentorsScreen(onViewVideos = { mentorId ->
                    showVideoLibrary = true
                })
            }
        }
    }
}


