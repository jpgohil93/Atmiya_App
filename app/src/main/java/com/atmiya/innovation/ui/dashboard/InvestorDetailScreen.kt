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
import com.atmiya.innovation.data.Investor
import com.atmiya.innovation.repository.FirestoreRepository
import com.atmiya.innovation.ui.components.DetailRow
import com.atmiya.innovation.ui.components.QuickStatItem
import com.atmiya.innovation.ui.components.SectionHeader
import com.atmiya.innovation.ui.components.SoftScaffold
import com.atmiya.innovation.ui.components.UserAvatar
import com.atmiya.innovation.ui.theme.AtmiyaPrimary
import com.atmiya.innovation.ui.theme.AtmiyaSecondary
import com.atmiya.innovation.utils.CurrencyUtils
import compose.icons.TablerIcons
import compose.icons.tablericons.Building
import compose.icons.tablericons.ChartPie
import compose.icons.tablericons.Coin
import compose.icons.tablericons.CurrencyRupee
import compose.icons.tablericons.InfoCircle
import compose.icons.tablericons.Target
import compose.icons.tablericons.Mail
import compose.icons.tablericons.Phone
import compose.icons.tablericons.World
import compose.icons.tablericons.BrandLinkedin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvestorDetailScreen(
    investorId: String,
    onBack: () -> Unit
) {
    val repository = remember { FirestoreRepository() }
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current
    
    // Auth & Status
    val auth = remember { com.google.firebase.auth.FirebaseAuth.getInstance() }
    val currentUserId = auth.currentUser?.uid ?: ""
    var currentUser by remember { mutableStateOf<com.atmiya.innovation.data.User?>(null) }
    var connectionStatus by remember { mutableStateOf("none") } // "none", "pending", "connected"

    var investor by remember { mutableStateOf<Investor?>(null) }
    var targetUser by remember { mutableStateOf<com.atmiya.innovation.data.User?>(null) } // To get email/phone
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(investorId, currentUserId) {
        try {
            isLoading = true
            var inv = repository.getInvestor(investorId)
            targetUser = repository.getUser(investorId) // Fetch user for contact details
            
            // Fallback if Investor is null OR if it has no name (empty document)
            val needsFallback = (inv == null || inv.name.isBlank())
            
            if (needsFallback && targetUser != null) {
                val u = targetUser!!
                // Use user data as base
                inv = inv?.copy(
                    name = u.name,
                    profilePhotoUrl = u.profilePhotoUrl ?: inv.profilePhotoUrl,
                    city = u.city
                ) ?: Investor(
                    uid = u.uid,
                    name = u.name,
                    profilePhotoUrl = u.profilePhotoUrl,
                    city = u.city,
                    firmName = "",
                    sectorsOfInterest = emptyList(),
                    preferredStages = emptyList(),
                    ticketSizeMin = "",
                    investmentType = "",
                    bio = "",
                    website = "",
                    isDeleted = false
                )
            }
            investor = inv
            
            if (currentUserId.isNotBlank()) {
                currentUser = repository.getUser(currentUserId)
                connectionStatus = repository.checkConnectionStatus(currentUserId, investorId)
            }
            android.util.Log.d("InvestorDetail", "Loaded investor: ${investor?.name}, ID: $investorId")
        } catch (e: Exception) {
             android.util.Log.e("InvestorDetail", "Error loading investor", e)
        } finally {
            isLoading = false
        }
    }

    SoftScaffold(
        topBar = {
            TopAppBar(
                title = { Text("Investor Profile", fontWeight = FontWeight.Bold, fontSize = 20.sp) },
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
        } else if (investor == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Investor details not found.", style = MaterialTheme.typography.bodyLarge, color = Color.Gray)
                    // Spacer(modifier = Modifier.height(8.dp))
                    // Text("DEBUG: ID = $investorId", style = MaterialTheme.typography.bodySmall, color = Color.Red)
                }
            }
        } else {
            val i = investor!!
            
            Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
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
                        // Prioritize users.profilePhotoUrl (source of truth) over investors.profilePhotoUrl
                        val heroPhotoUrl = targetUser?.profilePhotoUrl ?: i.profilePhotoUrl
                        UserAvatar(
                            model = heroPhotoUrl,
                            name = if (i.name.isNotBlank()) i.name else "?",
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
                            i.name.ifBlank { "Unknown Investor" }, 
                            style = MaterialTheme.typography.displaySmall, 
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                         Text(
                            i.firmName.ifBlank { "Independent Investor" },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                             textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(32.dp))

                    // --- Quick Stats ---
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        // Ticket Size - Min
                        val ticketStr = if (i.ticketSizeMin.isNotBlank()) "₹${CurrencyUtils.formatIndianRupee(i.ticketSizeMin)}" else "N/A"
                        QuickStatItem(
                             icon = TablerIcons.CurrencyRupee, 
                             label = "Start Ticket", 
                             value = ticketStr
                        )
                        QuickStatItem(
                             icon = TablerIcons.Target, 
                             label = "Focus Areas", 
                             value = "${i.sectorsOfInterest.size}"
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
                                i.bio.ifBlank { "No biography provided." },
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                lineHeight = 24.sp
                            )
                            
                            Spacer(modifier = Modifier.height(24.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            Spacer(modifier = Modifier.height(24.dp))
                            
                            // Detailed Info
                            Text("Investment Details", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            DetailRow("Firm Name", i.firmName.ifBlank { "Independent" }, TablerIcons.Building)
                            
                            if (i.sectorsOfInterest.isNotEmpty()) {
                                DetailRow("Focus Sectors", i.sectorsOfInterest.joinToString(", "), TablerIcons.ChartPie)
                            }
                            
                            val ticketRange = if(i.ticketSizeMin.isNotBlank()) {
                                "₹${CurrencyUtils.formatIndianRupee(i.ticketSizeMin)} (Min)"
                            } else "N/A"
                            DetailRow("Ticket Size", ticketRange, TablerIcons.Coin)
                            
                             if (i.preferredStages.isNotEmpty()) {
                                DetailRow("Stage", i.preferredStages.joinToString(", "), TablerIcons.InfoCircle)
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
                                if (i.website.isNotBlank()) DetailRow("Website", i.website, TablerIcons.World)
                                // LinkedIn not in Investor model, checking User model isn't standard for linkedin but maybe i.website covers it? 
                                // Model says "website: String // LinkedIn or Website". So we use i.website.
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(100.dp)) // Padding for FAB
                }
                
                // --- Floating CTA ---
                // Hide if connected
                if (connectionStatus != "connected") {
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
                                 Text("Interested in funding?", style = MaterialTheme.typography.labelLarge, color = Color.Gray)
                                 Text("Connect with Investor", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                             }
                             
                             Spacer(modifier = Modifier.width(16.dp))

                             val (btnText, btnEnabled) = when(connectionStatus) {
                                 "pending", "pending_sent" -> "Pending" to false
                                 "pending_received" -> "Accept" to true 
                                 else -> "Connect Now" to true
                             }

                             Button(
                                 onClick = { 
                                     if (connectionStatus == "none" && currentUser != null) {
                                         scope.launch {
                                             try {
                                                 repository.sendConnectionRequest(
                                                     sender = currentUser!!,
                                                     receiverId = investorId,
                                                     receiverName = i.name,
                                                     receiverRole = "investor",
                                                     receiverPhotoUrl = i.profilePhotoUrl
                                                 )
                                                 // Refresh status after sending
                                                 connectionStatus = repository.checkConnectionStatus(currentUserId, investorId)
                                                 android.widget.Toast.makeText(context, "Request sent!", android.widget.Toast.LENGTH_SHORT).show()
                                                 
                                              } catch (e: Exception) {
                                                  android.util.Log.e("Connection", "Error", e)
                                                  android.widget.Toast.makeText(context, "Error: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                                              }
                                         }
                                     }
                                 },
                                 enabled = btnEnabled && currentUser != null,
                                 // Removed fillMaxWidth, let it wrap content or set fixed width if needed
                                 // But since it's in a Row with weight(1f) text, it should be fine.
                                 colors = ButtonDefaults.buttonColors(
                                     containerColor = if (btnText == "Connect Now") AtmiyaSecondary else Color.Gray,
                                     contentColor = Color.White
                                 ),
                                 shape = RoundedCornerShape(12.dp)
                             ) {
                                 Text(btnText, fontWeight = FontWeight.Bold)
                             }
                         }
                    }
                } else {
                    // Connected State - No Button, maybe just a small floating text or nothing?
                    // User requested "no other button displayed". So we render nothing.
                }
            }
        }
    }
}


// Local components removed to use shared ones from StartupDetailScreen.kt (or move to common file if strictly needed)
