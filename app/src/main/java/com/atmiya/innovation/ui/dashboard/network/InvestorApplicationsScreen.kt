package com.atmiya.innovation.ui.dashboard.network

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.atmiya.innovation.data.FundingApplication
import com.atmiya.innovation.repository.FirestoreRepository
import com.google.firebase.auth.FirebaseAuth
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.background
import com.atmiya.innovation.ui.theme.AtmiyaPrimary
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvestorApplicationsScreen(
    onBack: () -> Unit,
    onNavigateToDetail: (String) -> Unit // startupId
) {
    val repository = remember { FirestoreRepository() }
    val auth = FirebaseAuth.getInstance()
    val currentUser = auth.currentUser
    
    var errorText by remember { mutableStateOf<String?>(null) } // Error State

    // 1. Fetch My Calls
    val myCalls by remember(currentUser) { 
        if (currentUser != null) repository.getFundingCallsForInvestorFlow(currentUser.uid) 
        else kotlinx.coroutines.flow.flowOf(emptyList()) 
    }.collectAsState(initial = emptyList())

    // 2. Fetch Applications for those calls
    // Note: This reactively updates when `myCalls` changes
    val callIds = remember(myCalls) { myCalls.map { it.id } }
    val applications by remember(callIds) {
        if (callIds.isNotEmpty()) {
             // Pass onError callback
             repository.getApplicationsForCallsFlow(callIds, onError = { msg ->
                 errorText = msg
             })
        }
        else kotlinx.coroutines.flow.flowOf(emptyList())
    }.collectAsState(initial = emptyList())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Received Applications") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp)) {
            
            // ERROR DISPLAY
            if (errorText != null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Error Loading Applications",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = errorText ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Hint: If this is an Index Error, look at Logcat for the link.",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }

            if (myCalls.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("You haven't posted any funding calls yet.")
                }
            } else if (applications.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No applications received yet.")
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(applications) { app ->
                        val relatedCall = myCalls.find { it.id == app.callId }
                        ApplicationItem(
                            application = app,
                            callTitle = relatedCall?.title ?: "Unknown Call",
                            onClick = { onNavigateToDetail(app.startupId) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ApplicationItem(
    application: FundingApplication,
    callTitle: String,
    onClick: () -> Unit
) {
    // Mimic ModernStartupCard style exactly from StartupListingScreen
    // Using FundingApplication data but fetching User data for enrichment
    val repository = remember { FirestoreRepository() }
    var founderName by remember { mutableStateOf("") }
    var startupProfile by remember { mutableStateOf<com.atmiya.innovation.data.User?>(null) }
    var startupDoc by remember { mutableStateOf<com.atmiya.innovation.data.Startup?>(null) }
    
    LaunchedEffect(application.startupId) {
        val user = repository.getUser(application.startupId)
        val startup = repository.getStartup(application.startupId)
        if (user != null) {
            founderName = user.name
            startupProfile = user
        }
        startupDoc = startup
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp), 
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp), // Zero elevation removes tint, pure white
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF3F4F6)) // Add subtle border for definition
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Logo Logic: Use User Profile Photo (source of truth) or Startup Logo or Initial
                val logoUrl = startupProfile?.profilePhotoUrl ?: startupDoc?.logoUrl
                
                if (!logoUrl.isNullOrBlank()) {
                    coil.compose.AsyncImage(
                        model = logoUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFF3F4F6)),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                } else {
                    androidx.compose.foundation.layout.Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE0E7FF)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = application.startupName.take(1).uppercase(),
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    // Title: Real Owner Name (Founder Name)
                    val displayTitle = if (founderName.isNotBlank()) founderName else application.startupName
                    
                    Text(
                        text = displayTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    // Subtitle: Startup Name
                    if (founderName.isNotBlank()) {
                         Text(
                            text = application.startupName,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF1F2937)
                        )
                    }
                    
                    // Tags: Sector • Stage (fetch from Startup Doc)
                    val sector = startupDoc?.sector ?: ""
                    val stage = startupDoc?.stage ?: ""
                    
                    if (sector.isNotBlank() || stage.isNotBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                             if (sector.isNotBlank()) {
                                 // Highlight Sector
                                 Surface(
                                     color = AtmiyaPrimary.copy(alpha = 0.1f),
                                     shape = RoundedCornerShape(4.dp)
                                 ) {
                                     Text(
                                         text = sector,
                                         style = MaterialTheme.typography.labelSmall,
                                         color = AtmiyaPrimary,
                                         fontWeight = FontWeight.Bold,
                                         modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                     )
                                 }
                                 
                                 if (stage.isNotBlank()) {
                                     Text(" • ", color = Color.Gray, modifier = Modifier.padding(horizontal = 4.dp))
                                 }
                             }
                             if (stage.isNotBlank()) {
                                 Text(
                                     text = stage,
                                     style = MaterialTheme.typography.bodySmall,
                                     color = Color.Gray
                                 )
                             }
                        }
                    }
                    
                    // Applied On
                    val appliedDate = remember(application.appliedAt) {
                        application.appliedAt?.toDate()?.let { date ->
                             java.text.SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(date)
                        } ?: ""
                    }
                    if (appliedDate.isNotEmpty()) {
                        Text(
                            text = "Applied: $appliedDate",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Gray,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
                
                // Bookmark Icon (Placeholder)
                IconButton(onClick = { /* TODO */ }) {
                     // Using default icon to avoid import issues if Tabler not previously imported here
                     // But StartupListing uses Tabler. Let's try to use default or just skip if risky.
                     // User said "same ui". Sticky header had TablerIcons.Bookmark.
                     // Let's us Icon(Icons.Default.Star) as placeholder if Tabler not imported, OR import Tabler.
                     // Converting to safe default for now to avoid compilation error if Tabler not in file imports
                     // Verified imports: file doesn't have Tabler. I will use Default.StarBorder or similar.
                     // Actually let's just use a gray tint Icon
                     // Or better, add the import in a separate call? No, assume available or use safely.
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            
            // Description (from Startup Doc)
            val description = startupDoc?.description ?: ""
            if (description.isNotBlank()) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF4B5563),
                    maxLines = 2,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Footer
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                 // Status Tag from Application
                 // Use Funding Ask if status is just "Applied" to match Listing? 
                 // User wants "same ui". Listing shows "Raising".
                 // But here status is important. Let's show Status.
                 StatusChip(status = application.status)
                
                Button(
                    onClick = onClick,
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF111827),
                        contentColor = Color.White
                    ),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    Text("View Profile", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

@Composable
fun StatusChip(status: String) {
    val (color, text) = when(status.lowercase()) {
        "shortlisted" -> MaterialTheme.colorScheme.primary to "Shortlisted"
        "rejected" -> MaterialTheme.colorScheme.error to "Rejected"
        else -> MaterialTheme.colorScheme.secondary to "Applied"
    }
    
    Surface(
        color = color.copy(alpha = 0.1f),
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text = text,
            color = color,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}
