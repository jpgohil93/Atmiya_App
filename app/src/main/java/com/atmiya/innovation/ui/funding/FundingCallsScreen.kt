package com.atmiya.innovation.ui.funding

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.atmiya.innovation.ui.theme.AtmiyaPrimary
import com.atmiya.innovation.ui.theme.AtmiyaSecondary

data class FundingCall(
    val id: String,
    val title: String,
    val investorName: String,
    val description: String,
    val sectors: List<String>,
    val stage: String,
    val status: String = "Open"
)

@Composable
fun FundingCallsScreen(role: String) {
    val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
    val auth = com.google.firebase.auth.FirebaseAuth.getInstance()

    // State for calls
    var calls by remember { mutableStateOf(listOf<FundingCall>()) }
    var searchQuery by remember { mutableStateOf("") }

    // Listen for real-time updates
    LaunchedEffect(Unit) {
        db.collection("funding_calls")
            .whereEqualTo("status", "Open")
            .addSnapshotListener { snapshot, e ->
                if (e != null) return@addSnapshotListener
                
                if (snapshot != null) {
                    val newCalls = snapshot.documents.map { doc ->
                        FundingCall(
                            id = doc.id,
                            title = doc.getString("title") ?: "",
                            investorName = doc.getString("investorName") ?: "Unknown Firm",
                            description = doc.getString("description") ?: "",
                            sectors = (doc.get("sectors") as? List<String>) ?: emptyList(),
                            stage = doc.getString("stage") ?: "",
                            status = doc.getString("status") ?: "Open"
                        )
                    }
                    calls = newCalls
                }
            }
    }
    
    val filteredCalls = calls.filter { 
        it.title.contains(searchQuery, ignoreCase = true) || 
        it.investorName.contains(searchQuery, ignoreCase = true) ||
        it.sectors.any { sector -> sector.contains(searchQuery, ignoreCase = true) }
    }

    var showCreateDialog by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
           // ...
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
                    FundingCallCard(call, role)
                }
            }
        }
        // ...
    }

        if (showCreateDialog) {
            CreateFundingCallDialog(
                onDismiss = { showCreateDialog = false },
                onCreate = { newCall ->
                    val user = auth.currentUser
                    if (user != null) {
                        val callData = hashMapOf(
                            "investorId" to user.uid,
                            "investorName" to "My Firm", // TODO: Fetch from profile
                            "title" to newCall.title,
                            "description" to newCall.description,
                            "sectors" to newCall.sectors, // Currently hardcoded to "General" in dialog
                            "stage" to newCall.stage,
                            "status" to "Open",
                            "timestamp" to com.google.firebase.Timestamp.now()
                        )
                        db.collection("funding_calls").add(callData)
                    }
                    showCreateDialog = false
                }
            )
        }
    }


@Composable
fun FundingCallCard(call: FundingCall, role: String) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
    val auth = com.google.firebase.auth.FirebaseAuth.getInstance()

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(text = call.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(text = "by ${call.investorName}", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(text = call.description, style = MaterialTheme.typography.bodyMedium)
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row {
                SuggestionChip(onClick = {}, label = { Text(call.stage) })
                Spacer(modifier = Modifier.width(8.dp))
                call.sectors.forEach { sector ->
                    SuggestionChip(onClick = {}, label = { Text(sector) })
                    Spacer(modifier = Modifier.width(4.dp))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (role == "startup") {
                Button(
                    onClick = {
                        val user = auth.currentUser
                        if (user != null) {
                            val application = hashMapOf(
                                "callId" to call.id,
                                "startupId" to user.uid,
                                "status" to "Pending",
                                "timestamp" to com.google.firebase.Timestamp.now()
                            )
                            db.collection("applications").add(application)
                                .addOnSuccessListener {
                                    android.widget.Toast.makeText(context, "Application Sent!", android.widget.Toast.LENGTH_SHORT).show()
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

@Composable
fun CreateFundingCallDialog(onDismiss: () -> Unit, onCreate: (FundingCall) -> Unit) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var stage by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Funding Call") },
        text = {
            Column {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title") })
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Description") })
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = stage, onValueChange = { stage = it }, label = { Text("Target Stage") })
            }
        },
        confirmButton = {
            Button(onClick = {
                onCreate(FundingCall("new", title, "My Firm", description, listOf("General"), stage))
            }) {
                Text("Post")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
