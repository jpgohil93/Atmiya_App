package com.atmiya.innovation.ui.funding

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
    onApply: (String) -> Unit = {} // Added for future use if needed, currently handling apply internally
) {
    val repository = remember { FirestoreRepository() }
    val auth = FirebaseAuth.getInstance()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var call by remember { mutableStateOf<FundingCall?>(null) }
    var investorProfile by remember { mutableStateOf<User?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var userRole by remember { mutableStateOf("") }
    
    // Animation State
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(callId) {
        try {
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
            
            // Check user role
            val user = auth.currentUser
            if (user != null) {
                val u = repository.getUser(user.uid)
                userRole = u?.role ?: ""
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
                        colors = IconButtonDefaults.iconButtonColors(containerColor = Color.White.copy(alpha = 0.8f))
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        bottomBar = {
            if (call != null && userRole == "startup") {
                BottomAppBar(
                    containerColor = Color.White,
                    tonalElevation = 8.dp
                ) {
                    Button(
                        onClick = {
                            scope.launch {
                                val user = auth.currentUser
                                if (user != null) {
                                    val userProfile = repository.getUser(user.uid)
                                    val application = FundingApplication(
                                        id = UUID.randomUUID().toString(),
                                        callId = call!!.id,
                                        startupId = user.uid,
                                        startupName = userProfile?.name ?: "Unknown Startup",
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
                            .height(50.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Black
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Apply for Funding", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
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
            ) {
                // --- Premium Header ---
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(320.dp) // Increased height for more details
                ) {
                    // Gradient Background
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        AtmiyaAccent, // Red
                                        Color.Black
                                    )
                                )
                            )
                    )
                    
                    // Pattern Overlay
                    Icon(
                        imageVector = Icons.Default.AttachMoney,
                        contentDescription = null,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(200.dp)
                            .offset(x = 50.dp, y = 50.dp)
                            .alpha(0.1f),
                        tint = Color.White
                    )

                    // Content
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(24.dp)
                    ) {
                        // Investor Profile Section
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (investorProfile?.profilePhotoUrl?.isNotEmpty() == true) {
                                AsyncImage(
                                    model = investorProfile!!.profilePhotoUrl,
                                    contentDescription = "Investor Profile",
                                    modifier = Modifier
                                        .size(60.dp)
                                        .clip(CircleShape)
                                        .background(Color.White),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(60.dp)
                                        .clip(CircleShape)
                                        .background(Color.White),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = c.investorName.firstOrNull()?.uppercase() ?: "I",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 24.sp,
                                        color = AtmiyaPrimary
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.width(16.dp))
                            
                            Column {
                                Text(
                                    text = c.investorName,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleMedium
                                )
                                if (investorProfile?.city?.isNotEmpty() == true) {
                                    Text(
                                        text = investorProfile!!.city,
                                        color = Color.White.copy(alpha = 0.8f),
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(20.dp))
                        
                        Text(
                            text = c.title,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Dates Row
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CalendarToday, contentDescription = null, tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                            val startDate = c.createdAt?.toDate()?.let { dateFormat.format(it) } ?: "N/A"
                            val endDate = c.applicationDeadline?.toDate()?.let { dateFormat.format(it) } ?: "N/A"
                            Text(
                                text = "$startDate - $endDate",
                                color = Color.White.copy(alpha = 0.9f),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // --- Content Body ---
                Column(
                    modifier = Modifier
                        .offset(y = (-20).dp)
                        .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    AnimatedVisibility(
                        visible = isVisible,
                        enter = slideInVertically(
                            initialOffsetY = { 100 },
                            animationSpec = spring(stiffness = Spring.StiffnessLow)
                        )
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
                            
                            // Countdown Timer
                            if (c.applicationDeadline != null) {
                                CountdownTimer(deadline = c.applicationDeadline)
                            }

                            // Key Stats Grid
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                DetailCard(
                                    icon = Icons.Default.MonetizationOn,
                                    title = "Ticket Size",
                                    value = "₹${c.minTicketAmount} - ₹${c.maxTicketAmount}",
                                    color = AtmiyaPrimary,
                                    modifier = Modifier.weight(1f)
                                )
                                DetailCard(
                                    icon = Icons.Default.PieChart,
                                    title = "Equity Ask",
                                    value = "${c.minEquity}% - ${c.maxEquity}%",
                                    color = Color(0xFFB71C1C), // Dark Red
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            // Description
                            Column {
                                SectionTitle("About the Opportunity")
                                Text(
                                    text = c.description,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    lineHeight = 24.sp
                                )
                            }

                            // Sectors & Stages
                            Column {
                                SectionTitle("Target Sectors")
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    c.sectors.forEach { sector ->
                                        Chip(text = sector, color = AtmiyaPrimary)
                                    }
                                }
                            }
                            
                            Column {
                                SectionTitle("Startup Stage")
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    c.stages.forEach { stage ->
                                        Chip(text = stage, color = AtmiyaSecondary)
                                    }
                                }
                            }

                            // Attachments
                            if (c.attachments.isNotEmpty()) {
                                Column {
                                    SectionTitle("Documents")
                                    c.attachments.forEach { attachment ->
                                        AttachmentCard(
                                            name = attachment["name"] ?: "Document",
                                            url = attachment["url"] ?: "",
                                            context = context
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(40.dp))
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
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, AtmiyaSecondary.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(Icons.Default.Timer, contentDescription = null, tint = AtmiyaSecondary)
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "Application Deadline",
                    style = MaterialTheme.typography.labelSmall,
                    color = AtmiyaSecondary
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
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(bottom = 8.dp)
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
        modifier = modifier.height(100.dp), // Fixed height for consistency
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f)),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
            }
            Column {
                Text(title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = color)
            }
        }
    }
}

@Composable
fun Chip(text: String, color: Color) {
    Surface(
        color = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.2f))
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = color,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
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
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Description, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.width(16.dp))
            Text(name, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
            Spacer(modifier = Modifier.weight(1f))
            Icon(Icons.Default.ArrowForward, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
        }
    }
}

// Helper for alpha
fun Modifier.alpha(alpha: Float) = this.then(Modifier.graphicsLayer(alpha = alpha))
