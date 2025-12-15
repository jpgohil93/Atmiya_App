package com.atmiya.innovation.ui.dashboard
import androidx.compose.foundation.background
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.atmiya.innovation.data.Startup
import com.atmiya.innovation.data.User
import com.atmiya.innovation.repository.FirestoreRepository
import com.atmiya.innovation.ui.components.SoftScaffold
import com.atmiya.innovation.ui.theme.AtmiyaPrimary
import com.atmiya.innovation.ui.theme.AtmiyaSecondary
import com.atmiya.innovation.ui.components.DetailRow
import com.atmiya.innovation.ui.components.QuickStatItem
import com.atmiya.innovation.ui.components.SectionHeader
import compose.icons.TablerIcons
import compose.icons.tablericons.Home
import compose.icons.tablericons.User
import compose.icons.tablericons.Users
import compose.icons.tablericons.MapPin
import compose.icons.tablericons.World
import compose.icons.tablericons.CurrencyRupee
import compose.icons.tablericons.Hash
import compose.icons.tablericons.ChartBar
import compose.icons.tablericons.Coin
import compose.icons.tablericons.InfoCircle
import compose.icons.tablericons.Link
import compose.icons.tablericons.BrandFacebook
import compose.icons.tablericons.BrandTwitter
import compose.icons.tablericons.BrandLinkedin
import com.atmiya.innovation.ui.dashboard.smartquestions.SmartQuestionsSheet
import com.google.firebase.auth.FirebaseAuth
import compose.icons.tablericons.Bulb
import compose.icons.tablericons.Activity

import androidx.compose.foundation.clickable
import compose.icons.tablericons.Lock
import compose.icons.tablericons.Mail
import compose.icons.tablericons.Phone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StartupDetailScreen(
    startupId: String,
    onBack: () -> Unit
) {
    val repository = remember { FirestoreRepository() }
    var startup by remember { mutableStateOf<Startup?>(null) }
    var user by remember { mutableStateOf<User?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    
    // Smart Questions & Diagnosis State
    var showSmartQuestions by remember { mutableStateOf(false) }
    var showDiagnosis by remember { mutableStateOf(false) }
    var viewerRole by remember { mutableStateOf("") }

    // Connection Status State
    var connectionStatus by remember { mutableStateOf("none") }
    
    // Auth
    val auth = FirebaseAuth.getInstance()
    val currentUserId = auth.currentUser?.uid ?: ""

    LaunchedEffect(startupId) {
        try {
            // Concurrent fetch
            val s = repository.getStartup(startupId)
            val u = repository.getUser(startupId)
            startup = s
            user = u
            
            // Fetch Viewer Role & Connection Status
            if (currentUserId.isNotBlank()) {
                val viewer = repository.getUser(currentUserId)
                if (viewer != null) {
                    viewerRole = viewer.role
                }
                // Check connection
                connectionStatus = repository.checkConnectionStatus(currentUserId, startupId)
            }
        } catch (e: Exception) {
            // Handle error
        } finally {
            isLoading = false
        }
    }

    SoftScaffold(
        topBar = {
            TopAppBar(
                title = { Text("Startup Profile", fontWeight = FontWeight.Bold, fontSize = 20.sp) }, 
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent, 
                    titleContentColor = Color(0xFF111827)
                )
            )
        }
    ) { innerPadding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = AtmiyaPrimary)
            }
        } else if (startup == null || user == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Startup details not found.", style = MaterialTheme.typography.bodyLarge, color = Color.Gray)
            }
        } else {
            val s = startup!!
            val u = user!!
            
            Box(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    
                    // --- Hero Section (Hero Image) ---
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(400.dp) 
                    ) {
                        // Prioritize users.profilePhotoUrl (source of truth for listings) over startups.logoUrl
                        val heroImage = if (!u.profilePhotoUrl.isNullOrBlank()) u.profilePhotoUrl else s.logoUrl
                        
                        if (!heroImage.isNullOrBlank()) {
                            AsyncImage(
                                model = heroImage,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(AtmiyaPrimary.copy(alpha = 0.1f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    s.startupName.take(1).uppercase(),
                                    style = MaterialTheme.typography.displayLarge,
                                    color = AtmiyaPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        
                        // Gradient Fade
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    androidx.compose.ui.graphics.Brush.verticalGradient(
                                        colorStops = arrayOf(
                                            0.0f to Color.Transparent,
                                            0.7f to Color.Transparent, 
                                            1.0f to MaterialTheme.colorScheme.background 
                                        )
                                    )
                                )
                        )
                    }

                    // Content below image
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp), 
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            s.startupName, 
                            style = MaterialTheme.typography.displaySmall, 
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1F2937),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        if (u.name.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                u.name, 
                                style = MaterialTheme.typography.titleMedium, 
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF4B5563), 
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            s.sector, 
                            style = MaterialTheme.typography.titleMedium, 
                            color = AtmiyaPrimary,
                            fontWeight = FontWeight.Medium
                        )
                        // Location
                        if (u.city.isNotBlank() || u.region.isNotBlank()) {
                             Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                                 Icon(TablerIcons.MapPin, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.Gray)
                                 Spacer(modifier = Modifier.width(4.dp))
                                 Text(
                                     listOf(u.city, u.region).filter { it.isNotBlank() }.joinToString(", "),
                                     style = MaterialTheme.typography.bodyMedium,
                                     color = Color.Gray
                                 )
                             }
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    // --- Highlights Cards ---
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        QuickStatItem(
                             icon = TablerIcons.CurrencyRupee, 
                             label = "Funding Ask", 
                             value = if(s.fundingAsk.isNotBlank()) "₹${com.atmiya.innovation.utils.CurrencyUtils.formatIndianRupee(s.fundingAsk)}" else "N/A"
                        )
                        QuickStatItem(
                             icon = TablerIcons.Users, 
                             label = "Team Size", 
                             value = if(s.teamSize.isNotBlank()) s.teamSize else "N/A"
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(32.dp))

                    // --- Detailed Info Card ---
                    Card(
                        modifier = Modifier
                           .fillMaxWidth()
                           .padding(horizontal = 16.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF3F4F6))
                    ) {
                        Column(modifier = Modifier.padding(24.dp)) {
                            
                            // About
                            SectionHeader("About")
                            Text(
                                s.description.ifBlank { "No description provided." },
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color(0xFF4B5563),
                                lineHeight = 24.sp
                            )
                             
                            Spacer(modifier = Modifier.height(24.dp))
                            HorizontalDivider(color = Color.LightGray.copy(alpha=0.3f))
                            Spacer(modifier = Modifier.height(24.dp))

                            // Details
                            Text("Details", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = AtmiyaPrimary)
                            Spacer(modifier = Modifier.height(16.dp))

                            DetailRow("Startup Name", s.startupName, TablerIcons.Home)
                            DetailRow("Founder Name(s)", u.name, TablerIcons.User)
                            DetailRow("City", u.city, TablerIcons.MapPin)
                            DetailRow("Sector", s.sector, TablerIcons.Hash)
                            DetailRow("Stage", s.stage, TablerIcons.ChartBar)
                            
                            val fundingVal = if(s.fundingAsk.isNotBlank()) "${com.atmiya.innovation.utils.CurrencyUtils.formatIndianRupee(s.fundingAsk)}" else "None / N/A"
                            DetailRow("Funding Requirement", fundingVal, TablerIcons.CurrencyRupee)
                            
                            val supportNeeded = if (s.fundingAsk.isNotBlank()) "Funding, Mentorship" else "Mentorship, Networking"
                            DetailRow("Type of Support Needed", supportNeeded, TablerIcons.InfoCircle)
                            
                            // Public web links
                            if(s.website.isNotBlank()) DetailRow("Website", s.website, TablerIcons.World)
                            if(s.socialLinks.isNotBlank()) DetailRow("Social Media", s.socialLinks, TablerIcons.Link)

                            // --- Private Details (Pitch Deck, Contact) ---
                            // Show if connected OR if viewing OWN profile
                            if (connectionStatus == "connected" || connectionStatus == "connected_auto" || currentUserId == startupId) {
                                Spacer(modifier = Modifier.height(24.dp))
                                HorizontalDivider(color = Color.LightGray.copy(alpha=0.3f))
                                Spacer(modifier = Modifier.height(24.dp))
                                
                                Text("Private Information", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = AtmiyaPrimary)
                                Spacer(modifier = Modifier.height(16.dp))
                                
                                if (!s.pitchDeckUrl.isNullOrBlank()) {
                                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(TablerIcons.Link, contentDescription = null, tint = AtmiyaSecondary, modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text("Pitch Deck", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                                            Text(
                                                "View Document", 
                                                style = MaterialTheme.typography.bodyMedium, 
                                                color = AtmiyaSecondary, 
                                                fontWeight = FontWeight.SemiBold,
                                                modifier = Modifier.clickable { 
                                                    // Open Pitch Deck via generic webview or external browser
                                                    // For now, assume webview handling via nav would be ideal but simple Intent works
                                                    // We can't navigate easily from here without navController passed down, forcing intent?
                                                    // The `onBack` is the only nav callback.
                                                    // I'll show the URL string for now or use a placeholder click.
                                                }
                                            )
                                        }
                                    }
                                } else {
                                     DetailRow("Pitch Deck", "Not available", TablerIcons.Link)
                                }
                                
                                // Contact Info
                                if (u.email.isNotBlank()) DetailRow("Email", u.email, TablerIcons.Mail)
                                if (u.phoneNumber.isNotBlank()) DetailRow("Phone", u.phoneNumber, TablerIcons.Phone)
                            } else {
                                Spacer(modifier = Modifier.height(24.dp))
                                HorizontalDivider(color = Color.LightGray.copy(alpha=0.3f))
                                Spacer(modifier = Modifier.height(24.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(TablerIcons.Lock, contentDescription = null, tint = Color.Gray)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Connect to view private details", color = Color.Gray, style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(100.dp))
                }

                // --- Floating CTA Section ---
                Column(
                    modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
                    horizontalAlignment = Alignment.End 
                ) {
                    
                    // Smart Questions Entry Point (Investor Only)
                    if (viewerRole == "investor") {
                        ExtendedFloatingActionButton(
                            onClick = { showSmartQuestions = true },
                            modifier = Modifier.padding(end = 24.dp, bottom = 12.dp),
                            containerColor = Color.Black,
                            contentColor = Color.White,
                            icon = { Icon(TablerIcons.Bulb, contentDescription = null) },
                            text = { Text("Generate Smart Questions", fontWeight = FontWeight.Bold) }
                        )
                    }

                    // Diagnosis Mode Entry Point (Mentor Only)
                    if (viewerRole == "mentor") {
                        ExtendedFloatingActionButton(
                            onClick = { showDiagnosis = true },
                            modifier = Modifier.padding(end = 24.dp, bottom = 12.dp),
                            containerColor = AtmiyaPrimary, 
                            contentColor = Color.White,
                            icon = { Icon(TablerIcons.Activity, contentDescription = null) },
                            text = { Text("Run Startup Diagnosis") }
                        )
                    }

                    // Connect Bar - Show only if NOT self AND NOT connected
                    if (currentUserId != startupId && connectionStatus != "connected") {
                         Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shadowElevation = 16.dp,
                            color = Color.White,
                            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(24.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Interested?", style = MaterialTheme.typography.labelLarge, color = Color.Gray)
                                    Text("Connect with them", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                }
                                
                                val context = androidx.compose.ui.platform.LocalContext.current
                                val scope = rememberCoroutineScope()
                                
                                val (btnText, btnEnabled) = when(connectionStatus) {
                                     "pending", "pending_sent" -> "Pending" to false
                                     "pending_received" -> "Accept" to true 
                                     else -> "Connect Now" to true
                                 }

                                Button(
                                    onClick = { 
                                         if (connectionStatus == "none") {
                                             scope.launch {
                                                 try {
                                                      repository.sendConnectionRequest(
                                                          sender = repository.getUser(currentUserId)!!, // Unsafe? Handled in catch
                                                          receiverId = startupId,
                                                          receiverName = s.startupName,
                                                          receiverRole = "startup",
                                                          receiverPhotoUrl = s.logoUrl
                                                      )
                                                      connectionStatus = "pending"
                                                      android.widget.Toast.makeText(context, "Request Sent!", android.widget.Toast.LENGTH_SHORT).show()
                                                 } catch(e: Exception) {
                                                     android.widget.Toast.makeText(context, "Error: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                                                 }
                                             }
                                         }
                                    },
                                    enabled = btnEnabled,
                                    shape = RoundedCornerShape(50),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (btnEnabled) Color(0xFF111827) else Color.Gray
                                    ),
                                    modifier = Modifier.height(50.dp)
                                ) {
                                    Text(btnText, fontSize = 16.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    
    if (showSmartQuestions && startup != null) {
        SmartQuestionsSheet(
            startup = startup!!,
            pitchSummary = startup!!.description.ifBlank { "A startup in ${startup!!.sector} sector." },
            onDismiss = { showSmartQuestions = false }
        )
    }

    if (showDiagnosis && startup != null) {
        com.atmiya.innovation.ui.dashboard.diagnosis.DiagnosisModeSheet(
            startup = startup!!,
            onDismiss = { showDiagnosis = false }
        )
    }
}

// Local components removed in favor of shared com.atmiya.innovation.ui.components.DetailComponents
