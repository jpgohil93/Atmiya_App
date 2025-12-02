package com.atmiya.innovation.ui.funding

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.atmiya.innovation.data.FundingApplication
import com.atmiya.innovation.data.FundingCall
import com.atmiya.innovation.repository.FirestoreRepository
import com.atmiya.innovation.ui.components.SoftButton
import com.atmiya.innovation.ui.components.SoftScaffold
import com.atmiya.innovation.ui.theme.AtmiyaPrimary
import com.atmiya.innovation.ui.theme.AtmiyaSecondary
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FundingCallDetailScreen(
    callId: String,
    onBack: () -> Unit
) {
    val repository = remember { FirestoreRepository() }
    val auth = FirebaseAuth.getInstance()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var call by remember { mutableStateOf<FundingCall?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(callId) {
        // We need a getFundingCall(id) method in repository. 
        // For now, we can filter from getAll or add a specific method.
        // Adding specific method is better but for speed I'll fetch all and find.
        // Actually, let's add getFundingCall to repository or just use getDocument logic here if needed.
        // But repository is cleaner. I'll assume I can add it or just fetch from collection directly here for MVP speed.
        try {
            val snapshot = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("fundingCalls").document(callId).get().await()
            call = snapshot.toObject(FundingCall::class.java)
        } catch (e: Exception) {
            Toast.makeText(context, "Error loading call", Toast.LENGTH_SHORT).show()
        } finally {
            isLoading = false
        }
    }

    SoftScaffold(
        topBar = {
            TopAppBar(
                title = { Text("Funding Opportunity", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = AtmiyaPrimary
                )
            )
        }
    ) { innerPadding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = AtmiyaPrimary)
            }
        } else if (call == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Funding Call not found.")
            }
        } else {
            val c = call!!
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(c.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = AtmiyaPrimary)
                Text("by ${c.investorName}", style = MaterialTheme.typography.titleMedium, color = Color.Gray)
                
                Divider()
                
                Section("Description", c.description)
                
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    InfoCard("Min Ticket", "₹${c.minTicketAmount}", Modifier.weight(1f))
                    InfoCard("Max Ticket", "₹${c.maxTicketAmount}", Modifier.weight(1f))
                }
                
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    InfoCard("Min Equity", "${c.minEquity}%", Modifier.weight(1f))
                    InfoCard("Max Equity", "${c.maxEquity}%", Modifier.weight(1f))
                }

                if (c.sectors.isNotEmpty()) {
                    Text("Sectors", fontWeight = FontWeight.Bold)
                    Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                        c.sectors.forEach { 
                            SuggestionChip(onClick = {}, label = { Text(it) })
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                    }
                }

                if (c.stages.isNotEmpty()) {
                    Text("Stages", fontWeight = FontWeight.Bold)
                    Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                        c.stages.forEach { 
                            SuggestionChip(onClick = {}, label = { Text(it) })
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                    }
                }

                if (c.attachments.isNotEmpty()) {
                    Text("Attachments", fontWeight = FontWeight.Bold)
                    c.attachments.forEach { attachment ->
                        val name = attachment["name"] as? String ?: "Document"
                        val url = attachment["url"] as? String ?: ""
                        
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (url.isNotEmpty()) {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                        context.startActivity(intent)
                                    }
                                },
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Description, contentDescription = null, tint = AtmiyaSecondary)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(name, fontWeight = FontWeight.Medium)
                                Spacer(modifier = Modifier.weight(1f))
                                Icon(Icons.Default.AttachFile, contentDescription = null, tint = Color.Gray)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Apply Button (Only for Startups)
                // We need to check role. Since we don't have role passed, we fetch user.
                var userRole by remember { mutableStateOf("") }
                LaunchedEffect(Unit) {
                    val user = auth.currentUser
                    if (user != null) {
                        val u = repository.getUser(user.uid)
                        userRole = u?.role ?: ""
                    }
                }

                if (userRole == "startup") {
                    SoftButton(
                        onClick = {
                            scope.launch {
                                val user = auth.currentUser
                                if (user != null) {
                                    val userProfile = repository.getUser(user.uid)
                                    val application = FundingApplication(
                                        id = UUID.randomUUID().toString(),
                                        callId = c.id,
                                        startupId = user.uid,
                                        startupName = userProfile?.name ?: "Unknown Startup",
                                        status = "applied",
                                        appliedAt = Timestamp.now()
                                    )
                                    repository.applyToFundingCall(application)
                                    Toast.makeText(context, "Application Sent Successfully!", Toast.LENGTH_LONG).show()
                                    onBack()
                                }
                            }
                        },
                        text = "Apply Now",
                        modifier = Modifier.fillMaxWidth().height(50.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun Section(title: String, content: String) {
    Column {
        Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(4.dp))
        Text(content, style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun InfoCard(title: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = AtmiyaPrimary.copy(alpha = 0.05f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(title, style = MaterialTheme.typography.labelMedium, color = Color.Gray)
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = AtmiyaPrimary)
        }
    }
}
