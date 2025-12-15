package com.atmiya.innovation.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import compose.icons.TablerIcons
import compose.icons.tablericons.*
import androidx.compose.foundation.verticalScroll // Added
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.atmiya.innovation.data.User
import com.atmiya.innovation.ui.theme.AtmiyaPrimary
import com.atmiya.innovation.ui.theme.AtmiyaSecondary
import kotlinx.coroutines.launch

@Composable
fun AppNavigationDrawer(
    drawerState: DrawerState,
    user: User?,
    onNavigate: (String) -> Unit, // Unified navigation callback
    onLogout: () -> Unit,
    content: @Composable () -> Unit
) {
    val scope = rememberCoroutineScope()
    val userName = user?.name ?: "User"
    val userPhotoUrl = user?.profilePhotoUrl ?: ""
    val userRole = user?.role ?: ""

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = true,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = Color.White,
                drawerContentColor = Color.Black,
                modifier = Modifier.width(320.dp), // Slightly wider
                drawerShape = RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp)
            ) {
                // --- Graphical Header ---
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .background(Color.Black)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.Bottom,
                        horizontalAlignment = Alignment.Start
                    ) {
                        Surface(
                            modifier = Modifier
                                .size(80.dp) // Larger Avatar
                                .clickable { onNavigate("profile_screen") },
                            shape = CircleShape,
                            color = Color.White,
                            border = androidx.compose.foundation.BorderStroke(2.dp, Color.White),
                            shadowElevation = 8.dp
                        ) {
                            if (userPhotoUrl.isNotBlank()) {
                                AsyncImage(
                                    model = userPhotoUrl,
                                    contentDescription = "Profile",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.fillMaxSize().background(Color.White)
                                ) {
                                    Text(
                                        text = userName.take(1).uppercase(),
                                        style = MaterialTheme.typography.headlineLarge,
                                        color = AtmiyaPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text(
                            text = userName,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = if (userRole.isNotBlank()) userRole.replaceFirstChar { it.uppercase() } else "Member",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                    
                    // Close Button Overlay
                    IconButton(
                        onClick = { scope.launch { drawerState.close() } },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(16.dp)
                    ) {
                        Icon(TablerIcons.X, contentDescription = "Close", tint = Color.White, modifier = Modifier.size(24.dp))
                    }
                }

                // --- Menu Content ---
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(vertical = 16.dp)
                        .verticalScroll(androidx.compose.foundation.rememberScrollState())
                ) {
                    val allMenuItems = listOf(
                        Triple("Wall", TablerIcons.Users, "wall_tab"),
                        Triple("Dashboard", TablerIcons.Home, "dashboard_tab"),
                        // Triple("Conversations", TablerIcons.MessageCircle, "conversations_list"),
                        Triple("Funding Calls", TablerIcons.Coin, "funding_calls_list"),
                        Triple("My Ideas", TablerIcons.Bulb, "saved_ideas"),
                        Triple("Events", TablerIcons.CalendarEvent, "events_list"),
                        Triple("Profile", TablerIcons.User, "profile_screen"),
                        Triple("My Network", TablerIcons.Share, "connection_requests"),
                        Triple("Settings", TablerIcons.Settings, "settings_screen")
                    )

                    val menuItems = if (userRole.equals("startup", ignoreCase = true)) {
                        allMenuItems
                    } else {
                        allMenuItems.filter { it.third != "saved_ideas" }
                    }

                    menuItems.forEach { (label, icon, route) ->
                         NavigationDrawerItem(
                            label = { Text(label, fontWeight = FontWeight.Medium) },
                            icon = { Icon(icon, contentDescription = null, tint = AtmiyaPrimary, modifier = Modifier.size(24.dp)) },
                            selected = false,
                            onClick = {
                                scope.launch { drawerState.close() }
                                onNavigate(route)
                            },
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = NavigationDrawerItemDefaults.colors(
                                unselectedContainerColor = Color.Transparent,
                                unselectedIconColor = AtmiyaPrimary,
                                unselectedTextColor = Color.Black
                            )
                        )
                    }

                    Spacer(modifier = Modifier.weight(1f))
                    Divider(modifier = Modifier.padding(vertical = 16.dp))
                    
                    NavigationDrawerItem(
                        label = { Text("Log Out", fontWeight = FontWeight.Bold, color = Color.Red) },
                        icon = { Icon(TablerIcons.Logout, contentDescription = null, tint = Color.Red, modifier = Modifier.size(24.dp)) },
                        selected = false,
                        onClick = {
                            scope.launch { 
                                drawerState.close()
                                onLogout()
                            }
                        },
                        modifier = Modifier.padding(horizontal = 12.dp),
                        colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = Color.Red.copy(alpha = 0.1f))
                    )
                    
                     Spacer(modifier = Modifier.height(24.dp))
                }
            }
        },
        content = content
    )
}
