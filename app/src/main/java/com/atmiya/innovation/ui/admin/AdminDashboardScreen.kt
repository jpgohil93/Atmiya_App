package com.atmiya.innovation.ui.admin

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info // Replaced QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.atmiya.innovation.ui.components.SoftCard
import com.atmiya.innovation.ui.theme.AtmiyaPrimary
import com.atmiya.innovation.ui.theme.AtmiyaSecondary

@Composable
fun AdminDashboardScreen(
    onNavigate: (String) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Users", "Funding", "Import", "Verify")

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = AtmiyaPrimary,
            contentColor = Color.White,
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
                    unselectedContentColor = Color.White.copy(alpha = 0.7f)
                )
            }
        }

        when (selectedTab) {
            0 -> UserListScreen(onUserClick = { userId -> onNavigate("user_detail/$userId") })
            1 -> com.atmiya.innovation.ui.funding.FundingListScreen(
                role = "admin",
                onCallClick = { callId -> onNavigate("funding_call/$callId") },
                onCreateCall = { onNavigate("create_funding_call") },
                onViewApps = { callId -> onNavigate("funding_apps_list/$callId") }
            )
            2 -> CsvImportScreen()
            3 -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    SoftCard(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { onNavigate("admin_verification") }
                    ) {
                        Column(
                            modifier = Modifier.padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = AtmiyaPrimary
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "Verify Startups",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Scan QR Code or Search by Phone",
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color.Gray
                            )
                        }
                    }
                }
            }
        }
    }
}
