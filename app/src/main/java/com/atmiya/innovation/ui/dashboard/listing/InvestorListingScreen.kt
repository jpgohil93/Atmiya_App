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
    
    var key by remember { mutableStateOf(0) }
    val investorsFlow = remember(key) { repository.getInvestorsFlow() }
    val investors by investorsFlow.collectAsState(initial = emptyList())
    
    var isRefreshing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

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
                // DEBUG SECTION
                // Debug info removed as per request

                items(investors) { investor ->
                    InvestorCard(user = investor, onClick = { onInvestorClick(investor.uid) })
                }
            }
        }
    }
}

@Composable
fun InvestorCard(user: Investor, onClick: () -> Unit) {
    NetworkCard(
        imageModel = user.profilePhotoUrl ?: "",
        name = user.name,
        roleOrTitle = user.firmName.ifBlank { "Independent Investor" },
        badges = {
             // Show first sector as a subtle badge too if space permits, or just rely on details
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
        secondaryButtonText = "Connect",
        onSecondaryClick = { /* Handle Connect */ onClick() }
    )
}
