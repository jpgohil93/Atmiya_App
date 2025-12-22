package com.atmiya.innovation.ui.dashboard

import kotlinx.coroutines.launch
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.atmiya.innovation.data.Mentor
import com.atmiya.innovation.repository.FirestoreRepository
import com.atmiya.innovation.ui.components.DetailRow
import com.atmiya.innovation.ui.components.QuickStatItem
import com.atmiya.innovation.ui.components.SectionHeader
import com.atmiya.innovation.ui.components.SoftScaffold
import com.atmiya.innovation.ui.components.UserAvatar
import com.atmiya.innovation.ui.theme.AtmiyaPrimary
import compose.icons.TablerIcons
import compose.icons.tablericons.Briefcase
import compose.icons.tablericons.Certificate
import compose.icons.tablericons.Clock
import compose.icons.tablericons.MapPin
import compose.icons.tablericons.School
import compose.icons.tablericons.User
import compose.icons.tablericons.Mail
import compose.icons.tablericons.Phone
import compose.icons.tablericons.World
import compose.icons.tablericons.BrandLinkedin
import compose.icons.tablericons.Lock

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MentorDetailScreen(
    mentorId: String,
    onBack: () -> Unit
) {
    val repository = remember { FirestoreRepository() }
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current
    
    // Auth & Status
    val auth = remember { com.google.firebase.auth.FirebaseAuth.getInstance() }
    val currentUserId = auth.currentUser?.uid ?: ""
    var currentUser by remember { mutableStateOf<com.atmiya.innovation.data.User?>(null) }
    
    // Connection Status State (Real-time)
    val connectionStatus by remember(currentUserId, mentorId) {
        if (currentUserId.isNotBlank()) repository.getConnectionStatusFlow(currentUserId, mentorId)
        else kotlinx.coroutines.flow.flowOf("none")
    }.collectAsState(initial = "none") 

    var mentor by remember { mutableStateOf<Mentor?>(null) }
    var targetUser by remember { mutableStateOf<com.atmiya.innovation.data.User?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(mentorId, currentUserId) {
        try {
            isLoading = true
            mentor = repository.getMentor(mentorId)
            targetUser = repository.getUser(mentorId)
            
            if (currentUserId.isNotBlank()) {
                currentUser = repository.getUser(currentUserId)
                // connectionStatus handled by Flow
            }
        } catch (e: Exception) {
            // Log error
        } finally {
            isLoading = false
        }
    }

    SoftScaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mentor Profile", fontWeight = FontWeight.Bold, fontSize = 20.sp) },
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
        } else if (mentor == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Mentor details not found.", style = MaterialTheme.typography.bodyLarge, color = Color.Gray)
            }
        } else {
            val m = mentor!!
            
            Box(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    
                    // --- Hero Section ---
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(400.dp)
                    ) {
                        // Prioritize users.profilePhotoUrl (source of truth) over mentors.profilePhotoUrl
                        val heroPhotoUrl = targetUser?.profilePhotoUrl ?: m.profilePhotoUrl
                        UserAvatar(
                            model = heroPhotoUrl,
                            name = m.name,
                            modifier = Modifier.fillMaxSize(),
                            size = null,
                            shape = androidx.compose.ui.graphics.RectangleShape,
                            fontSize = MaterialTheme.typography.displayLarge.fontSize
                        )
                        
                        // Gradient Overlay
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    androidx.compose.ui.graphics.Brush.verticalGradient(
                                        colorStops = arrayOf(
                                            0.0f to Color.Transparent,
                                            0.6f to Color.Transparent, 
                                            1.0f to MaterialTheme.colorScheme.background
                                        )
                                    )
                                )
                        )
                    }

                    // --- Header Info ---
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            m.name, 
                            style = MaterialTheme.typography.displaySmall, 
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            m.title, 
                            style = MaterialTheme.typography.titleMedium, 
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary, 
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        if (m.organization.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                             Text(
                                m.organization,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    // --- Quick Stats ---
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        QuickStatItem(
                             icon = TablerIcons.Clock, 
                             label = "Experience", 
                             value = "${m.experienceYears} Years"
                        )
                         // Could add another stat if available, e.g. "Mentees" or "Sessions"
                         // For now, let's show 'Expertise' count as a proxy for breadth
                        QuickStatItem(
                             icon = TablerIcons.School, 
                             label = "Areas", 
                             value = "${m.expertiseAreas.size}"
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(32.dp))

                    // --- Details Card ---
                    Card(
                        modifier = Modifier
                           .fillMaxWidth()
                           .padding(horizontal = 16.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(24.dp)) {
                            
                            // Bio
                            SectionHeader("About")
                            Text(
                                m.bio.ifBlank { "No biography provided." },
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                lineHeight = 24.sp
                            )
                             
                            Spacer(modifier = Modifier.height(24.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            Spacer(modifier = Modifier.height(24.dp))

                            // Details
                            Text("Professional Details", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(16.dp))

                            DetailRow("Current Title", m.title, TablerIcons.Briefcase)
                            DetailRow("Organization", m.organization, TablerIcons.MapPin) // Using MapPin as placeholder for Org location/entity
                            DetailRow("Experience", "${m.experienceYears} Years", TablerIcons.Clock)
                            
                            if (m.expertiseAreas.isNotEmpty()) {
                                DetailRow("Expertise", m.expertiseAreas.joinToString(", "), TablerIcons.Certificate)
                            }
                            
                            // Contact Info (Revealed if connected)
                            if (connectionStatus == "connected" || connectionStatus == "connected_auto") { 
                                Spacer(modifier = Modifier.height(24.dp))
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                Spacer(modifier = Modifier.height(24.dp))

                                Text("Contact Information", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.height(16.dp))
                                
                                targetUser?.let { user ->
                                    if (user.email.isNotBlank()) DetailRow("Email", user.email, TablerIcons.Mail)
                                    if (user.phoneNumber.isNotBlank()) DetailRow("Phone", com.atmiya.innovation.utils.StringUtils.formatPhoneNumber(user.phoneNumber), TablerIcons.Phone)
                                }
                            } else {
                                Spacer(modifier = Modifier.height(24.dp))
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                Spacer(modifier = Modifier.height(24.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(TablerIcons.Lock, contentDescription = null, tint = Color.Gray)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Connect to view private details", color = Color.Gray, style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(100.dp)) // Padding for FAB
                }

                // --- Floating CTA ---
                if (connectionStatus != "connected" && connectionStatus != "connected_auto") {
                    Surface(
                        modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
                        shadowElevation = 16.dp,
                        color = Color.White,
                        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                    ) {
                         Row(
                             modifier = Modifier.padding(24.dp),
                             verticalAlignment = Alignment.CenterVertically
                         ) {
                             Column(modifier = Modifier.weight(1f)) {
                                 Text("Looking for guidance?", style = MaterialTheme.typography.labelLarge, color = Color.Gray)
                                 Text("Request Mentorship", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                             }

                              Spacer(modifier = Modifier.width(16.dp))
                              
                              val (btnText, btnEnabled, isWithdraw) = when(connectionStatus) {
                                  "pending_sent" -> Triple("Withdraw", true, true)
                                  "pending_received" -> Triple("Accept", true, false)
                                  "declined" -> Triple("Declined (Wait 24h)", false, false)
                                  else -> Triple("Connect Now", true, false)
                              }

                             Button(
                                 onClick = { 
                                     if (isWithdraw) {
                                          scope.launch {
                                              val sortedIds = listOf(currentUserId, mentorId).sorted()
                                              val requestId = "${sortedIds[0]}_${sortedIds[1]}"
                                              try {
                                                  repository.cancelConnectionRequest(requestId)
                                                  android.widget.Toast.makeText(context, "Request Withdrawn", android.widget.Toast.LENGTH_SHORT).show()
                                              } catch(e: Exception) {
                                                  android.widget.Toast.makeText(context, "Error: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                                              }
                                          }
                                     } else if (connectionStatus == "none" && currentUser != null) {
                                         scope.launch {
                                             try {
                                                 repository.sendConnectionRequest(
                                                     sender = currentUser!!,
                                                     receiverId = mentorId,
                                                     receiverName = m.name,
                                                     receiverRole = "mentor",
                                                     receiverPhotoUrl = m.profilePhotoUrl
                                                 )
                                                 // Status update handled by Flow
                                                 android.widget.Toast.makeText(context, "Request sent!", android.widget.Toast.LENGTH_SHORT).show()
                                             } catch(e: Exception) {
                                                 android.widget.Toast.makeText(context, "Error: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                                             }
                                         }
                                     } else if (connectionStatus == "pending_received") {
                                        android.widget.Toast.makeText(context, "Check Requests tab to accept.", android.widget.Toast.LENGTH_LONG).show()
                                     }
                                 },
                                 enabled = btnEnabled,
                                 shape = RoundedCornerShape(50),
                                 colors = ButtonDefaults.buttonColors(
                                     containerColor = if (isWithdraw) Color.Transparent else if (btnEnabled) AtmiyaPrimary else Color.Gray,
                                     contentColor = if (isWithdraw) Color.Red else Color.White
                                 ),
                                 border = if (isWithdraw) androidx.compose.foundation.BorderStroke(1.dp, Color.Red) else null,
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

// Local components removed to use shared ones from StartupDetailScreen.kt
