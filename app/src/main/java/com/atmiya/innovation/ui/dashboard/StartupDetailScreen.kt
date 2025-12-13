package com.atmiya.innovation.ui.dashboard
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

    LaunchedEffect(startupId) {
        try {
            // Concurrent fetch
            val s = repository.getStartup(startupId)
            val u = repository.getUser(startupId)
            startup = s
            user = u
        } catch (e: Exception) {
            // Handle error
        } finally {
            isLoading = false
        }
    }

    SoftScaffold(
        topBar = {
            TopAppBar(
                title = { Text("Startup Profile", fontWeight = FontWeight.Bold, fontSize = 20.sp) }, // Smaller, cleaner title
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent, // Transparent to blend with background
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
                    
                    // --- Header Section (Hero Image) ---
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(400.dp) // Covers roughly half screen
                    ) {
                        val heroImage = if (!s.logoUrl.isNullOrBlank()) s.logoUrl else u.profilePhotoUrl
                        
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
                                            0.7f to Color.Transparent, // Keep majority of image clear
                                            1.0f to MaterialTheme.colorScheme.background // Fade to match scaffold background
                                        )
                                    )
                                )
                        )
                    }

                    // Content below image
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp), // Removed negative offset to let gradient do the work seamlessly
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
                                color = Color(0xFF4B5563), // Dark Gray
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
                        // Location: City, Region
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

                    // --- Highlights Cards (Quick Stats) ---
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
                            Divider(color = Color.LightGray.copy(alpha=0.3f))
                            Spacer(modifier = Modifier.height(24.dp))

                            // Grid of Details
                            Text("Details", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = AtmiyaPrimary)
                            Spacer(modifier = Modifier.height(16.dp))

                            DetailRow("Startup Name", s.startupName, TablerIcons.Home)
                            DetailRow("Founder Name(s)", u.name, TablerIcons.User)
                            DetailRow("City", u.city, TablerIcons.MapPin)
                            DetailRow("Region", u.region, TablerIcons.World)
                            DetailRow("Sector", s.sector, TablerIcons.Hash)
                            DetailRow("Stage", s.stage, TablerIcons.ChartBar)
                            
                            val fundingVal = if(s.fundingAsk.isNotBlank()) "${com.atmiya.innovation.utils.CurrencyUtils.formatIndianRupee(s.fundingAsk)}" else "None / N/A"
                            DetailRow("Funding Requirement", fundingVal, TablerIcons.CurrencyRupee)
                            
                            val supportNeeded = if (s.fundingAsk.isNotBlank()) "Funding, Mentorship" else "Mentorship, Networking"
                            DetailRow("Type of Support Needed", supportNeeded, TablerIcons.InfoCircle)
                            
                            DetailRow("Website URL", s.website, TablerIcons.World)
                            DetailRow("Social Media", s.socialLinks, TablerIcons.Link)
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(100.dp)) // Padding for FAB
                }

                // --- Floating CTA ---
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
                             Text("Interested?", style = MaterialTheme.typography.labelLarge, color = Color.Gray)
                             Text("Connect with them", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                         }
                         Button(
                             onClick = { /* Handle Connect Logic */ },
                             shape = RoundedCornerShape(50),
                             colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF111827)),
                             modifier = Modifier.height(50.dp)
                         ) {
                             Text("Connect Now", fontSize = 16.sp)
                         }
                     }
                }
            }
        }
    }
}

// Local components removed in favor of shared com.atmiya.innovation.ui.components.DetailComponents
