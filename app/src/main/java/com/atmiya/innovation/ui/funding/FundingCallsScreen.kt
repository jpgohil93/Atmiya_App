package com.atmiya.innovation.ui.funding

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
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
import com.atmiya.innovation.ui.theme.AtmiyaPrimary
import com.atmiya.innovation.ui.theme.AtmiyaSecondary
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.Timestamp
import kotlinx.coroutines.launch
import java.util.UUID

@Composable
fun FundingCallsScreen(role: String, onNavigate: (String) -> Unit) {
    val repository = remember { FirestoreRepository() }
    val auth = FirebaseAuth.getInstance()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // State for calls
    val calls by repository.getFundingCalls().collectAsState(initial = emptyList())
    var searchQuery by remember { mutableStateOf("") }
    
    val filteredCalls = calls.filter { 
        it.title.contains(searchQuery, ignoreCase = true) || 
        it.investorName.contains(searchQuery, ignoreCase = true) ||
        (it.sectors ?: emptyList()).any { sector -> sector.contains(searchQuery, ignoreCase = true) }
    }

    Scaffold(
        floatingActionButton = {
            if (role == "investor") {
                FloatingActionButton(
                    onClick = { onNavigate("create_funding_call") },
                    containerColor = AtmiyaPrimary,
                    contentColor = Color.White
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Create Call")
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = "Funding Opportunities",
                    style = MaterialTheme.typography.headlineMedium,
                    color = AtmiyaPrimary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Search Opportunities") },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    shape = RoundedCornerShape(12.dp)
                )
            }
            if (filteredCalls.isEmpty()) {
                item {
                    Text("No active funding calls found.", color = Color.Gray)
                }
            } else {
                items(filteredCalls) { call ->
                    FundingCallCard(call, role, repository, auth, scope, context, onNavigate)
                }
            }
        }
    }
}

@Composable
fun FundingCallCard(
    call: FundingCall, 
    role: String,
    repository: FirestoreRepository,
    auth: FirebaseAuth,
    scope: kotlinx.coroutines.CoroutineScope,
    context: android.content.Context,
    onNavigate: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onNavigate("funding_call/${call.id}") },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(text = call.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(text = "by ${call.investorName}", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(text = call.description, style = MaterialTheme.typography.bodyMedium, maxLines = 3, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row {
                val stages = call.stages ?: emptyList()
                stages.take(3).forEach { stage ->
                    SuggestionChip(onClick = {}, label = { Text(stage) })
                    Spacer(modifier = Modifier.width(4.dp))
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row {
                val sectors = call.sectors ?: emptyList()
                sectors.take(3).forEach { sector ->
                    SuggestionChip(onClick = {}, label = { Text(sector) })
                    Spacer(modifier = Modifier.width(4.dp))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (role == "startup") {
                Button(
                    onClick = {
                        scope.launch {
                            val user = auth.currentUser
                            if (user != null) {
                                val userProfile = repository.getUser(user.uid)
                                val application = FundingApplication(
                                    id = UUID.randomUUID().toString(),
                                    callId = call.id,
                                    startupId = user.uid,
                                    startupName = userProfile?.name ?: "Unknown Startup",
                                    status = "applied",
                                    appliedAt = Timestamp.now()
                                )
                                repository.applyToFundingCall(application)
                                Toast.makeText(context, "Application Sent!", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = AtmiyaSecondary)
                ) {
                    Text("Apply Now")
                }
            } else if (role == "investor") {
                OutlinedButton(
                    onClick = { /* TODO: View Applications */ },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("View Applications")
                }
            }
        }
    }
}
