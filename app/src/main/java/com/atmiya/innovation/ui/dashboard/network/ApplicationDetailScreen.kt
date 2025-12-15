package com.atmiya.innovation.ui.dashboard.network

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.atmiya.innovation.data.FundingApplication
import com.atmiya.innovation.repository.FirestoreRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApplicationDetailScreen(
    callId: String,
    applicationId: String,
    onBack: () -> Unit,
    onViewStartupProfile: (String) -> Unit // startupId
) {
    val repository = remember { FirestoreRepository() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    
    // Fetch Application
    var application by remember { mutableStateOf<FundingApplication?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    
    LaunchedEffect(callId, applicationId) {
        application = repository.getFundingApplication(callId, applicationId)
        isLoading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Application Details") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (isLoading) {
            Box(Modifier.padding(padding).fillMaxSize()) { CircularProgressIndicator(Modifier.align(Alignment.Center)) }
        } else if (application == null) {
            Box(Modifier.padding(padding).fillMaxSize()) { Text("Application not found", Modifier.align(Alignment.Center)) }
        } else {
            val app = application!!
            Column(
                modifier = Modifier
                    .padding(padding)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Text(app.startupName, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Status: ${app.status.replaceFirstChar { it.uppercase() }}", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                        Divider()
                        DetailRow("Funding Ask", app.fundingAsk)
                        DetailRow("City", app.city)
                        DetailRow("State", app.state)
                        DetailRow("Sector", app.startupSector)
                        DetailRow("Stage", app.startupStage)
                        DetailRow("Email", app.startupEmail)
                        DetailRow("Phone", com.atmiya.innovation.utils.StringUtils.formatPhoneNumber(app.startupPhone))
                    }
                }
                
                if (app.additionalNote.isNotBlank()) {
                    Text("Additional Note", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(app.additionalNote, style = MaterialTheme.typography.bodyMedium)
                }

                if (!app.pitchDeckUrl.isNullOrBlank()) {
                     Button(
                        onClick = { 
                            // Handle open URL 
                            // Intent logic would go here
                        }, 
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val fileName = com.atmiya.innovation.utils.StorageUtils.getFileNameFromUrl(app.pitchDeckUrl!!)
                        Text("View $fileName")
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Actions
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { onViewStartupProfile(app.startupId) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("View Profile")
                    }
                    
                    Button(
                        onClick = {
                             scope.launch {
                                 repository.updateFundingApplicationStatus(app.id, "shortlisted")
                                 // Refresh local state manually or rely on re-fetch if we observe flow
                                 application = app.copy(status = "shortlisted")
                             }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = app.status != "shortlisted"
                    ) {
                        Text("Shortlist")
                    }
                }
            }
        }
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    if (value.isNotBlank()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
        }
    }
}
