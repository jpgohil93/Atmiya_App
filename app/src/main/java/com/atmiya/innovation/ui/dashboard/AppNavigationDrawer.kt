package com.atmiya.innovation.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.atmiya.innovation.data.User
import com.atmiya.innovation.ui.theme.AtmiyaPrimary
import kotlinx.coroutines.launch

@Composable
fun AppNavigationDrawer(
    drawerState: DrawerState,
    user: User?,
    onNavigateToDashboard: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onLogout: () -> Unit,
    content: @Composable () -> Unit
) {
    val scope = rememberCoroutineScope()
    val userName = user?.name ?: "User"
    val userPhotoUrl = user?.profilePhotoUrl ?: ""
    val userRole = user?.role ?: ""

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = false,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = Color.White,
                drawerContentColor = Color.Black,
                modifier = Modifier.width(300.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp)
                ) {
                    // --- Header ---
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Avatar
                            if (userPhotoUrl.isNotBlank()) {
                                AsyncImage(
                                    model = userPhotoUrl,
                                    contentDescription = "Profile",
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(Color.LightGray),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(AtmiyaPrimary.copy(alpha = 0.1f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = userName.take(1).uppercase(),
                                        style = MaterialTheme.typography.titleMedium,
                                        color = AtmiyaPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            // Name & Role
                            Column {
                                Text(
                                    text = userName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (userRole.isNotBlank()) userRole.replaceFirstChar { it.uppercase() } else "Member",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.Gray
                                )
                            }
                        }
                        // Close Button
                        IconButton(
                            onClick = { scope.launch { drawerState.close() } },
                            modifier = Modifier
                                .size(32.dp)
                                .background(Color(0xFFF5F5F5), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = Color.Black,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    // --- Main Menu Items ---
                    val menuItems = listOf(
                        Triple("Conversations", Icons.Default.Email, { /* TODO */ }),
                        Triple("Wall", Icons.Default.Home, {
                            scope.launch { drawerState.close() }
                            Unit
                        }),
                        Triple("Dashboard", Icons.Default.Dashboard, {
                            scope.launch { drawerState.close() }
                            onNavigateToDashboard()
                        }),
                        Triple("Funding Calls", Icons.Default.AttachMoney, { /* TODO */ }),
                        Triple("Mentorship", Icons.Default.School, { /* TODO */ }),
                        Triple("Events", Icons.Default.Event, { /* TODO */ })
                    )

                    menuItems.forEach { (label, icon, onClick) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(onClick = onClick)
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = label,
                                tint = Color.Black,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    // --- Secondary Menu ---
                    val secondaryItems = listOf(
                        "Profile" to {
                            scope.launch { drawerState.close() }
                            onNavigateToProfile()
                        },
                        "Settings" to {
                            scope.launch { drawerState.close() }
                            onNavigateToSettings()
                        },
                        "Help" to { /* TODO */ }
                    )

                    secondaryItems.forEach { (label, onClick) ->
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(onClick = onClick)
                                .padding(vertical = 8.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // --- Logout Button ---
                    Button(
                        onClick = {
                            scope.launch {
                                drawerState.close()
                                onLogout()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Black),
                        shape = RoundedCornerShape(25.dp)
                    ) {
                        Text("Log Out", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        content = content
    )
}
