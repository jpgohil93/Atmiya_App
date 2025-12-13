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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvestorDetailScreen(
    investorId: String,
    onBack: () -> Unit
) {
    val repository = remember { FirestoreRepository() }
    var investor by remember { mutableStateOf<Investor?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(investorId) {
        try {
            investor = repository.getInvestor(investorId)
        } catch (e: Exception) {
             // Log error
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
                Text("Investor details not found.", style = MaterialTheme.typography.bodyLarge, color = Color.Gray)
            }
        } else {
            val i = investor!!
            
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
                        if (!i.profilePhotoUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = i.profilePhotoUrl,
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
                                    i.name.take(1).uppercase(),
                                    style = MaterialTheme.typography.displayLarge,
                                    color = AtmiyaPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        
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
                            i.name, 
                            style = MaterialTheme.typography.displaySmall, 
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1F2937),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                         Text(
                            i.firmName.ifBlank { "Independent Investor" },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = AtmiyaPrimary,
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
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF3F4F6))
                    ) {
                        Column(modifier = Modifier.padding(24.dp)) {
                            
                            // Bio
                            SectionHeader("About")
                             Text(
                                i.bio.ifBlank { "No biography provided." },
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color(0xFF4B5563),
                                lineHeight = 24.sp
                            )
                            
                            Spacer(modifier = Modifier.height(24.dp))
                            HorizontalDivider(color = Color.LightGray.copy(alpha=0.3f))
                            Spacer(modifier = Modifier.height(24.dp))
                            
                            // Detailed Info
                            Text("Investment Details", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = AtmiyaPrimary)
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

                            // Email/Phone intentionally excluded
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
                             Text("Interested in funding?", style = MaterialTheme.typography.labelLarge, color = Color.Gray)
                             Text("Connect with Investor", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                         }
                         Button(
                             onClick = { /* Connect Logic */ },
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


// Local components removed to use shared ones from StartupDetailScreen.kt (or move to common file if strictly needed)
