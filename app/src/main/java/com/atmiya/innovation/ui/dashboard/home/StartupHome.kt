package com.atmiya.innovation.ui.dashboard.home

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.Star // Replaced MonetizationOn
import androidx.compose.material.icons.filled.Home // Replaced School
import androidx.compose.material.icons.filled.List // Replaced Forum/Description
import androidx.compose.material.icons.filled.CheckCircle // Replaced VerifiedUser
import androidx.compose.material.icons.filled.DateRange // Replaced CalendarToday
import androidx.compose.material.icons.filled.LocationOn // Replaced Map
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.atmiya.innovation.data.Startup
import com.atmiya.innovation.repository.FirestoreRepository
import com.atmiya.innovation.ui.components.*
import com.atmiya.innovation.ui.theme.AtmiyaPrimary
import com.atmiya.innovation.ui.theme.AtmiyaSecondary
import com.atmiya.innovation.utils.QRCodeGenerator
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

@Composable
fun StartupHome(
    onNavigate: (String) -> Unit
) {
    val repository = remember { FirestoreRepository() }
    val auth = FirebaseAuth.getInstance()
    var userName by remember { mutableStateOf("Startup") }
    var participantId by remember { mutableStateOf<String?>(null) }
    var track by remember { mutableStateOf("") }
    var fundingCount by remember { mutableIntStateOf(0) }
    
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        val user = auth.currentUser
        if (user != null) {
            try {
                val userProfile = repository.getUser(user.uid)
                userName = userProfile?.name ?: "Startup"
                participantId = userProfile?.participantId
                
                val startupProfile = repository.getStartup(user.uid)
                track = startupProfile?.track ?: (if (startupProfile?.startupType == "edp") "EDP" else "ACC")
            } catch (e: Exception) {
                android.util.Log.e("StartupHome", "Error fetching user", e)
            }
        }
        
        try {
            val calls = repository.getFundingCalls(limit = 10)
            fundingCount = calls.size
        } catch (e: Exception) {
            fundingCount = 0
        }
    }

    val items = listOf(
        BentoItem(BentoCardType.FEATURE, "Funding Calls", "Apply for investment", Icons.Default.Star, badge = if (fundingCount > 0) "$fundingCount New" else null, onClick = { onNavigate("funding") }),
        BentoItem(BentoCardType.FEATURE, "Mentorship", "Connect with experts", Icons.Default.Home, onClick = { onNavigate("Network") }),
        BentoItem(BentoCardType.FEATURE, "Community Wall", "Share & Connect", Icons.Default.List, span = 2, onClick = { onNavigate("wall") }),
        BentoItem(BentoCardType.UTILITY, "Verification", "Show QR", Icons.Default.CheckCircle, onClick = { onNavigate("startup_verification") }),
        BentoItem(BentoCardType.UTILITY, "My Pitch Deck", icon = Icons.Default.List, onClick = { /* Open PDF */ }),
        BentoItem(BentoCardType.UTILITY, "Settings", icon = Icons.Default.Settings, onClick = { onNavigate("profile") })
    )

    BentoGrid(
        items = items,
        header = {
            EventCompanionSection(
                userName = userName,
                participantId = participantId ?: "Pending",
                track = track,
                onOpenMap = {
                    val gmmIntentUri = Uri.parse("geo:0,0?q=Atmiya+University,+Kalawad+Road,+Rajkot")
                    val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                    mapIntent.setPackage("com.google.android.apps.maps")
                    if (mapIntent.resolveActivity(context.packageManager) != null) {
                        context.startActivity(mapIntent)
                    } else {
                        // Fallback to browser
                        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/search/?api=1&query=Atmiya+University,+Kalawad+Road,+Rajkot"))
                        context.startActivity(browserIntent)
                    }
                }
            )
        }
    )
}

@Composable
fun EventCompanionSection(
    userName: String,
    participantId: String,
    track: String,
    onOpenMap: () -> Unit
) {
    var showFullPass by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // 1. Digital ID Card
        SoftCard(
            modifier = Modifier.fillMaxWidth().height(180.dp),
            backgroundColor = AtmiyaPrimary,
            contentColor = Color.White,
            onClick = { showFullPass = true }
        ) {
            Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("DELEGATE PASS", style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.7f))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(userName, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text("$track Track", style = MaterialTheme.typography.titleMedium, color = AtmiyaSecondary)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(participantId, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, color = Color.White.copy(alpha = 0.9f))
                }
                
                // QR Code
                Box(
                    modifier = Modifier.size(120.dp).clip(RoundedCornerShape(12.dp)).background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    val qrBitmap = remember(participantId) { QRCodeGenerator.generateQRCode(participantId) }
                    if (qrBitmap != null) {
                        Image(bitmap = qrBitmap, contentDescription = "QR Code", modifier = Modifier.size(100.dp))
                    }
                }
            }
        }

        // 2. Schedule Card
        SoftCard(modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.DateRange, contentDescription = null, tint = AtmiyaPrimary)
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text("Event Schedule", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Dec 17 - 21 • 5 Days", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            // Mini Timeline
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                listOf("17", "18", "19", "20", "21").forEach { day ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(day, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text("Dec", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    }
                }
            }
        }

        // 3. Venue Map
        SoftCard(modifier = Modifier.fillMaxWidth(), onClick = onOpenMap) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.LocationOn, contentDescription = null, tint = AtmiyaSecondary)
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text("Venue: Atmiya University", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Kalawad Road, Rajkot", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                }
                Spacer(modifier = Modifier.weight(1f))
                Icon(Icons.Default.ArrowForward, contentDescription = null, tint = Color.Gray)
            }
        }
    }

    if (showFullPass) {
        Dialog(onDismissRequest = { showFullPass = false }) {
            SoftCard(modifier = Modifier.fillMaxWidth().padding(16.dp), radius = 24.dp) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(16.dp)) {
                    Text("OFFICIAL DELEGATE", style = MaterialTheme.typography.labelLarge, color = AtmiyaPrimary, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    val qrBitmap = remember(participantId) { QRCodeGenerator.generateQRCode(participantId) }
                    if (qrBitmap != null) {
                        Image(bitmap = qrBitmap, contentDescription = "QR Code", modifier = Modifier.size(200.dp))
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(userName, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = Color.Black)
                    Text(track, style = MaterialTheme.typography.headlineSmall, color = AtmiyaSecondary)
                    Text(participantId, style = MaterialTheme.typography.titleMedium, color = Color.Gray)
                    
                    Spacer(modifier = Modifier.height(32.dp))
                    Text("Dec 17-21, 2025", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                    Text("Atmiya University, Rajkot", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}
