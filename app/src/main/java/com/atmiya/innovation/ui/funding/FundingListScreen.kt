package com.atmiya.innovation.ui.funding

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.atmiya.innovation.data.FundingCall
import com.atmiya.innovation.ui.theme.AtmiyaPrimary
import com.google.firebase.auth.FirebaseAuth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FundingListScreen(
    role: String,
    onCallClick: (String) -> Unit,
    onCreateCall: () -> Unit = {},
    onViewApps: (String) -> Unit = {},
    viewModel: FundingViewModel = viewModel()
) {
    val fundingCalls by viewModel.fundingCalls.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val filterType by viewModel.filterType.collectAsState()
    
    val auth = FirebaseAuth.getInstance()
    
    Scaffold(
        floatingActionButton = {
            if (role == "investor") {
                FloatingActionButton(
                    onClick = onCreateCall,
                    containerColor = AtmiyaPrimary,
                    contentColor = Color.White
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Create Call")
                }
            }
        }
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            // Header
            SmallTopAppBar(
                title = { Text("Funding Calls", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.smallTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = AtmiyaPrimary
                )
            )

            // Filter Tabs (Simplified)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = filterType == "all",
                    onClick = { viewModel.setFilter("all") },
                    label = { Text("All Active") }
                )
                
                if (role == "investor") {
                    FilterChip(
                        selected = filterType == "my_calls",
                        onClick = { viewModel.setFilter("my_calls") },
                        label = { Text("My Calls") }
                    )
                }
            }

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = AtmiyaPrimary)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (fundingCalls.isEmpty()) {
                        item {
                            Text(
                                "No funding calls found.",
                                modifier = Modifier.fillMaxWidth().padding(32.dp),
                                style = MaterialTheme.typography.bodyLarge,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    } else {
                        items(fundingCalls) { call ->
                            val isOwner = role == "investor" && call.investorId == auth.currentUser?.uid
                            FundingCallCard(
                                call = call, 
                                onClick = { onCallClick(call.id) },
                                showApplications = isOwner,
                                onViewApplications = { onViewApps(call.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FundingCallCard(
    call: FundingCall, 
    onClick: () -> Unit,
    showApplications: Boolean = false,
    onViewApplications: () -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = call.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = AtmiyaPrimary,
                    modifier = Modifier.weight(1f)
                )
                if (!call.isActive) {
                    Badge(containerColor = Color.Gray) { Text("Closed") }
                }
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "By ${call.investorName}",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = call.description,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                call.sectors.take(2).forEach { sector ->
                    SuggestionChip(
                        onClick = {},
                        label = { Text(sector, style = MaterialTheme.typography.bodySmall) },
                        enabled = false
                    )
                }
                if (call.sectors.size > 2) {
                    Text(
                        "+${call.sectors.size - 2}",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.align(Alignment.CenterVertically)
                    )
                }
            }

            if (showApplications) {
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = onViewApplications,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("View Applications")
                }
            }
        }
    }
}
