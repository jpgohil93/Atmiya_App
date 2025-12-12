package com.atmiya.innovation.ui.funding

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
// import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Info // Added explicit import
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.atmiya.innovation.ui.theme.AtmiyaPrimary
import android.content.Intent
import android.net.Uri

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FundingDetailScreen(
    callId: String,
    onBack: () -> Unit,
    onApply: () -> Unit,
    viewModel: FundingViewModel = viewModel()
) {
    val selectedCall by viewModel.selectedCall.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val context = LocalContext.current
    val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
    val currentUser = auth.currentUser
    val firestoreRepository = remember { com.atmiya.innovation.repository.FirestoreRepository() }
    
    // State to check if user has already applied (if startup)
    var hasApplied by remember { mutableStateOf(false) }
    var userRole by remember { mutableStateOf("") }
    
    // State for Investor view: Applications list
    var applications by remember { mutableStateOf<List<com.atmiya.innovation.data.FundingApplication>>(emptyList()) }

    LaunchedEffect(callId) {
        viewModel.selectCall(callId)
        if (currentUser != null) {
            val user = firestoreRepository.getUser(currentUser.uid)
            userRole = user?.role ?: ""
            
            if (userRole == "startup") {
                hasApplied = firestoreRepository.hasApplied(callId, currentUser.uid)
            }
        }
    }
    
    // Fetch applications if user is the owner investor
    LaunchedEffect(selectedCall, currentUser) {
        if (selectedCall != null && currentUser != null && selectedCall!!.investorId == currentUser.uid) {
            firestoreRepository.getApplicationsForCall(callId).collect { apps ->
                applications = apps
            }
        }
    }

    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = { Text("Funding Details") },
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
        if (isLoading || selectedCall == null) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = AtmiyaPrimary)
            }
        } else {
            val call = selectedCall!!
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                Text(
                    text = call.title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = AtmiyaPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Posted by ${call.investorName}",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.Gray
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                FundingDetailSectionHeader("Description")
                Text(text = call.description, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                
                Spacer(modifier = Modifier.height(24.dp))
                
                FundingDetailSectionHeader("Details")
                FundingDetailRow("Ticket Size", "${call.minTicketAmount} - ${call.maxTicketAmount}")
                FundingDetailRow("Sectors", call.sectors.joinToString(", "))
                FundingDetailRow("Stages", call.stages.joinToString(", "))
                if (call.locationPreference != null) {
                    FundingDetailRow("Location", call.locationPreference)
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                if (call.attachments.isNotEmpty()) {
                    FundingDetailSectionHeader("Attachments")
                    call.attachments.forEach { attachment ->
                        OutlinedButton(
                            onClick = {
                                val url = attachment["url"]
                                if (url != null) {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                    context.startActivity(intent)
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = AtmiyaPrimary)
                        ) {
                            Icon(Icons.Default.Info, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(attachment["name"] ?: "Document")
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                
                // Logic for Buttons / Sections based on Role
                if (userRole == "startup") {
                    if (hasApplied) {
                        Button(
                            onClick = { },
                            enabled = false,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(disabledContainerColor = Color.Gray)
                        ) {
                            Text("Application Submitted ✅")
                        }
                    } else {
                        Button(
                            onClick = onApply,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = AtmiyaPrimary)
                        ) {
                            Text("Apply Now")
                        }
                    }
                } else if (currentUser != null && call.investorId == currentUser.uid) {
                    // Investor View: Show Applications
                    FundingDetailSectionHeader("Received Applications (${applications.size})")
                    if (applications.isEmpty()) {
                        Text("No applications yet.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        applications.forEach { app ->
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(app.startupName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                    Text("Ask: ${app.fundingAsk}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                                    Text("Stage: ${app.startupStage} | Sector: ${app.startupSector}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    if (app.pitchDeckUrl != null) {
                                        OutlinedButton(
                                            onClick = {
                                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(app.pitchDeckUrl))
                                                context.startActivity(intent)
                                            },
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text("View Pitch Deck")
                                        }
                                    }
                                    if (app.additionalNote.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text("Note: ${app.additionalNote}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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

@Composable
private fun FundingDetailSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

@Composable
private fun FundingDetailRow(label: String, value: String) {
    Row(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(
            text = "$label: ",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
