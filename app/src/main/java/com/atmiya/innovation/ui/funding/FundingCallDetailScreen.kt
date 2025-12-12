package com.atmiya.innovation.ui.funding

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import compose.icons.TablerIcons
import compose.icons.tablericons.ArrowLeft
import compose.icons.tablericons.Pencil
import compose.icons.tablericons.InfoCircle
import compose.icons.tablericons.CalendarEvent
import compose.icons.tablericons.ChartPie
import compose.icons.tablericons.MapPin
import compose.icons.tablericons.FileText
import compose.icons.tablericons.ArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.atmiya.innovation.data.FundingApplication
import com.atmiya.innovation.data.FundingCall
import com.atmiya.innovation.data.User
import com.atmiya.innovation.repository.FirestoreRepository
import com.atmiya.innovation.ui.dashboard.startup.DeadlineBadge
import com.atmiya.innovation.ui.theme.AtmiyaPrimary
import com.atmiya.innovation.ui.theme.AtmiyaSecondary
import com.atmiya.innovation.ui.theme.AtmiyaAccent
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.UUID
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FundingCallDetailScreen(
    callId: String,
    onBack: () -> Unit,
    onApply: (String) -> Unit = {},
    onEdit: (String) -> Unit = {}
) {
    val repository = remember { FirestoreRepository() }
    val auth = FirebaseAuth.getInstance()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    
    var call by remember { mutableStateOf<FundingCall?>(null) }
    var investorProfile by remember { mutableStateOf<User?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var currentUser by remember { mutableStateOf<User?>(null) }
    
    // Animation State
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(callId) {
        try {
            val user = auth.currentUser
            if (user != null) {
                currentUser = repository.getUser(user.uid)
            }

            val snapshot = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("fundingCalls").document(callId).get().await()
            val fetchedCall = snapshot.toObject(FundingCall::class.java)
            call = fetchedCall
            
            if (fetchedCall != null && fetchedCall.investorId.isNotEmpty()) {
                try {
                    investorProfile = repository.getUser(fetchedCall.investorId)
                } catch (e: Exception) {
                    android.util.Log.e("FundingDetail", "Error fetching investor", e)
                }
            }
            
            isVisible = true
        } catch (e: Exception) {
            Toast.makeText(context, "Error loading call", Toast.LENGTH_SHORT).show()
        } finally {
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        colors = IconButtonDefaults.iconButtonColors(containerColor = Color.White.copy(alpha = 0.9f))
                    ) {
                        Icon(TablerIcons.ArrowLeft, contentDescription = "Back", modifier = Modifier.size(24.dp))
                    }
                },
                actions = {
                    // Show Edit Button if current user is the creator (investor)
                    if (call != null && currentUser != null && call!!.investorId == currentUser!!.uid) {
                        IconButton(
                            onClick = { 
                                onEdit(callId)
                            },
                             colors = IconButtonDefaults.iconButtonColors(containerColor = Color.White.copy(alpha = 0.9f))
                        ) {
                           Icon(TablerIcons.Pencil, contentDescription = "Edit", modifier = Modifier.size(24.dp))
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        bottomBar = {
            if (call != null) {
                if (currentUser?.role == "startup") {
                    BottomAppBar(
                        containerColor = Color.White,
                        tonalElevation = 8.dp
                    ) {
                        Button(
                            onClick = {
                                scope.launch {
                                    val user = auth.currentUser
                                    if (user != null) {
                                        val application = FundingApplication(
                                            id = UUID.randomUUID().toString(),
                                            callId = call!!.id,
                                            startupId = user.uid,
                                            startupName = currentUser?.name ?: "Unknown Startup",
                                            status = "applied",
                                            appliedAt = Timestamp.now()
                                        )
                                        repository.applyToFundingCall(application)
                                        Toast.makeText(context, "Application Sent Successfully!", Toast.LENGTH_LONG).show()
                                        onBack()
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                                .height(56.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AtmiyaPrimary
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text("Apply for Funding", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                } else if (currentUser?.role == "investor" && call!!.investorId == currentUser!!.uid) {
                   // Creator View 
                }
            }
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
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = innerPadding.calculateBottomPadding())
                    .background(Color.White)
            ) {
                // --- Premium Header ---
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(350.dp) // Taller header
                ) {
                    // Background
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color(0xFFF5F5F7), // Light Gray
                                        Color(0xFFE0E0E0)  // Slightly Darker
                                    )
                                )
                            )
                    ) {
                         // Subtle Pattern / Logo Overlay
                         Icon(
                            imageVector = TablerIcons.InfoCircle, // Replaced MonetizationOn
                            contentDescription = null,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .offset(x = 40.dp, y = (-20).dp)
                                .size(240.dp)
                                .alpha(0.03f)
                                .graphicsLayer(rotationZ = -15f),
                            tint = Color.Black
                        )
                    }

                    // Content Overlay
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(horizontal = 24.dp, vertical = 40.dp) // Adjusted padding
                    ) {
                        
                        // New Investor Badge
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = Color.White,
                            modifier = Modifier.padding(bottom = 16.dp),
                            shadowElevation = 2.dp
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (investorProfile?.profilePhotoUrl?.isNotEmpty() == true) {
                                    AsyncImage(
                                        model = investorProfile!!.profilePhotoUrl,
                                        contentDescription = "Investor Profile",
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(AtmiyaPrimary),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = c.investorName.firstOrNull()?.uppercase() ?: "I",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = Color.White
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = c.investorName, // Investor Name
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = AtmiyaPrimary
                                )
                            }
                        }

                        // Title
                        Text(
                            text = c.title,
                            style = MaterialTheme.typography.displaySmall, // Larger
                            fontWeight = FontWeight.Bold,
                            color = AtmiyaPrimary,
                            lineHeight = 40.sp
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Meta Row (Date & Location)
                        val dateFormat = SimpleDateFormat("dd MMM, yyyy", Locale.getDefault())
                        val deadlineStr = c.applicationDeadline?.toDate()?.let { dateFormat.format(it) } ?: "N/A"
                        
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(TablerIcons.CalendarEvent, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Deadline: $deadlineStr",
                                color = Color.Gray,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            
                            if (!c.locationPreference.isNullOrEmpty()) {
                                Spacer(modifier = Modifier.width(16.dp))
                                Icon(TablerIcons.MapPin, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp)) // Replace with Location icon if available
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = c.locationPreference,
                                    color = Color.Gray,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                // --- Content Body ---
                Column(
                    modifier = Modifier
                        .offset(y = (-24).dp)
                        .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                        .background(Color.White)
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(32.dp) // More breathing room
                ) {
                    AnimatedVisibility(
                        visible = isVisible,
                        enter = slideInVertically(
                            initialOffsetY = { 100 },
                            animationSpec = spring(stiffness = Spring.StiffnessLow)
                        )
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(32.dp)) {
                            
                            // 1. Stats Grid (Ticket & Equity)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                DetailCard(
                                    icon = TablerIcons.InfoCircle,
                                    title = "Ticket Size",
                                    value = "₹${c.minTicketAmount} - ₹${c.maxTicketAmount}",
                                    color = AtmiyaPrimary,
                                    modifier = Modifier.weight(1f)
                                )
                                DetailCard(
                                    icon = TablerIcons.ChartPie, // Replaced PieChart
                                    title = "Equity",
                                    value = "${c.minEquity}% - ${c.maxEquity}%",
                                    color = AtmiyaSecondary,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            
                            // 2. Countdown Timer (If active)
                            if (c.applicationDeadline != null) {
                                CountdownTimer(deadline = c.applicationDeadline)
                            }
                            HorizontalDivider(color = Color(0xFFF0F0F0)) // Divider -> HorizontalDivider in M3

                            // 3. Description
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                SectionTitle("About the Opportunity")
                                Text(
                                    text = c.description,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = Color(0xFF424242), // Softer Black
                                    lineHeight = 28.sp // Better readability
                                )
                            }

                            // 4. Tags (Sectors & Stages)
                            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        SectionTitle("Sectors")
                                        FlowRow(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalArrangement = Arrangement.spacedBy(8.dp),
                                            modifier = Modifier.padding(top = 8.dp)
                                        ) {
                                            if (c.sectors.isNotEmpty()) {
                                                c.sectors.forEach { sector ->
                                                    Chip(text = sector, color = AtmiyaPrimary)
                                                }
                                            } else {
                                                Text("All Sectors", color = Color.Gray, style = MaterialTheme.typography.bodyMedium)
                                            }
                                        }
                                    }
                                }
                                
                                Column {
                                    SectionTitle("Startup Stage")
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.padding(top = 8.dp)
                                    ) {
                                        if (c.stages.isNotEmpty()) {
                                            c.stages.forEach { stage ->
                                                Chip(text = stage, color = AtmiyaSecondary)
                                            }
                                        } else {
                                             Text("Any Stage", color = Color.Gray, style = MaterialTheme.typography.bodyMedium)
                                        }
                                    }
                                }
                            }

                            // 5. Attachments
                            if (c.attachments.isNotEmpty()) {
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    SectionTitle("Documents")
                                    c.attachments.forEach { attachment ->
                                        AttachmentCard(
                                            name = attachment["name"] ?: "Document",
                                            url = attachment["url"] ?: "",
                                            context = context
                                        )
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(60.dp)) // Bottom padding
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CountdownTimer(deadline: Timestamp) {
    var timeLeft by remember { mutableStateOf("") }
    
    LaunchedEffect(deadline) {
        while (true) {
            val diff = deadline.toDate().time - System.currentTimeMillis()
            if (diff <= 0) {
                timeLeft = "Expired"
                break
            }
            val days = TimeUnit.MILLISECONDS.toDays(diff)
            val hours = TimeUnit.MILLISECONDS.toHours(diff) % 24
            val minutes = TimeUnit.MILLISECONDS.toMinutes(diff) % 60
            val seconds = TimeUnit.MILLISECONDS.toSeconds(diff) % 60
            timeLeft = String.format("%02dd : %02dh : %02dm : %02ds", days, hours, minutes, seconds)
            delay(1000)
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF0F0).copy(alpha = 0.5f)), // Subtle Red Tint
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, AtmiyaSecondary.copy(alpha = 0.1f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(AtmiyaSecondary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                 Icon(TablerIcons.CalendarEvent, contentDescription = null, tint = AtmiyaSecondary)
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column {
                Text(
                    text = "Application Closes In",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.Gray
                )
                Text(
                    text = timeLeft,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = AtmiyaSecondary,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}

@Composable
fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp),
        fontWeight = FontWeight.Bold,
        color = AtmiyaPrimary
    )
}

@Composable
fun DetailCard(
    icon: ImageVector,
    title: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.height(130.dp), // Taller for better proportion 
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF9F9F9)),
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(0.dp, Color.Transparent), // Clean styling
        elevation = CardDefaults.cardElevation(0.dp) // Flat
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.Start
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .border(1.dp, color.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.weight(1f))
            Column {
                Text(title, style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                     value, 
                     style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp), 
                     fontWeight = FontWeight.Bold, 
                     color = AtmiyaPrimary,
                     maxLines = 1,
                     overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun Chip(text: String, color: Color) {
    Surface(
        color = color.copy(alpha = 0.05f), // Very subtle tint
        shape = RoundedCornerShape(50),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.2f))
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = color,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
}

@Composable
fun AttachmentCard(name: String, url: String, context: android.content.Context) {
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
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEEEEEE)), // Clean white card
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFF5F5F7)),
                contentAlignment = Alignment.Center
            ) {
                Icon(TablerIcons.FileText, contentDescription = null, tint = Color.Gray) // PDF Icon Ideally
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                 Text(name, fontWeight = FontWeight.SemiBold, color = AtmiyaPrimary, maxLines = 1)
                 Text("Tap to view", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            }
            
            Icon(TablerIcons.ArrowRight, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp))
        }
    }
}

// Helper for alpha
fun Modifier.alpha(alpha: Float) = this.then(Modifier.graphicsLayer(alpha = alpha))
