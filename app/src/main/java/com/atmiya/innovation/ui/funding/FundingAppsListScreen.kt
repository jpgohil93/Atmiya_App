package com.atmiya.innovation.ui.funding

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.atmiya.innovation.data.FundingApplication
import com.atmiya.innovation.repository.FirestoreRepository
import com.atmiya.innovation.ui.theme.AtmiyaPrimary
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FundingAppsListScreen(
    callId: String,
    onBack: () -> Unit,
    onAppClick: (String) -> Unit // applicationId
) {
    val repository = remember { FirestoreRepository() }
    val scope = rememberCoroutineScope()
    
    var applications by remember { mutableStateOf<List<FundingApplication>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(callId) {
        try {
            repository.getApplicationsForCall(callId).collect {
                applications = it
                isLoading = false
            }
        } catch (e: Exception) {
            // Handle error
        }
    }

    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = { Text("Applications", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.smallTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = AtmiyaPrimary
                )
            )
        }
    ) { padding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = AtmiyaPrimary)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (applications.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                            Text("No applications yet.", color = Color.Gray)
                        }
                    }
                } else {
                    items(applications) { app ->
                        ApplicationCard(app = app, onClick = { onAppClick(app.id) })
                    }
                }
            }
        }
    }
}

@Composable
fun ApplicationCard(app: FundingApplication, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = app.startupName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                StatusBadge(status = app.status)
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${app.startupSector} • ${app.startupStage}",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Ask: ₹${app.fundingAsk}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = AtmiyaPrimary
            )
        }
    }
}

@Composable
fun StatusBadge(status: String) {
    val (color, text) = when (status) {
        "accepted" -> Color(0xFF4CAF50) to "Accepted"
        "rejected" -> Color(0xFFF44336) to "Rejected"
        "ignored" -> Color.Gray to "Ignored"
        else -> Color(0xFFFF9800) to "Applied"
    }
    
    Surface(
        color = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(16.dp)
    ) {
        Text(
            text = text,
            color = color,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}
