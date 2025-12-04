package com.atmiya.innovation.ui.dashboard.startup

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.atmiya.innovation.data.FundingCall
import com.atmiya.innovation.repository.FirestoreRepository
import com.atmiya.innovation.ui.theme.AtmiyaPrimary
import com.atmiya.innovation.ui.theme.AtmiyaSecondary
import com.atmiya.innovation.ui.theme.AtmiyaAccent
import com.google.firebase.Timestamp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

@Composable
fun FundingCallsSection(
    recommendedCalls: List<FundingCall>,
    allCalls: List<FundingCall>,
    isLoading: Boolean,
    onCallClick: (String) -> Unit,
    onViewAllClick: () -> Unit,
    onCreateTestCall: () -> Unit // DEBUG
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp)
    ) {
        // DEBUG: Create Test Call Button if empty
        if (!isLoading && allCalls.isEmpty() && recommendedCalls.isEmpty()) {
            Button(
                onClick = onCreateTestCall,
                modifier = Modifier.padding(horizontal = 20.dp).fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = AtmiyaSecondary)
            ) {
                Text("DEBUG: Create Test Funding Call")
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
        // --- Recommended Section ---
        if (recommendedCalls.isNotEmpty() || isLoading) {
            SectionHeader(
                title = "Recommended for You",
                isAnimated = true,
                onViewAll = null
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            if (isLoading) {
                FundingCallShimmer()
            } else {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(recommendedCalls) { call ->
                        FundingCallCard(
                            call = call,
                            isRecommended = true,
                            onClick = { onCallClick(call.id) }
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
        }

        // --- All Funding Calls Section ---
        SectionHeader(
            title = "All Funding Calls",
            isAnimated = false,
            onViewAll = onViewAllClick
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        if (isLoading && recommendedCalls.isEmpty()) {
            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                repeat(3) {
                    FundingCallShimmer()
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        } else {
            Column(
                modifier = Modifier.padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                allCalls.take(5).forEach { call ->
                    FundingCallCard(
                        call = call,
                        isRecommended = false,
                        onClick = { onCallClick(call.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun SectionHeader(
    title: String,
    isAnimated: Boolean,
    onViewAll: (() -> Unit)?
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val infiniteTransition = rememberInfiniteTransition(label = "header_pulse")
        val alpha by if (isAnimated) {
            infiniteTransition.animateFloat(
                initialValue = 0.7f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1000, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "alpha"
            )
        } else {
            remember { mutableFloatStateOf(1f) }
        }

        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = if (isAnimated) alpha else 1f)
        )
        
        if (onViewAll != null) {
            TextButton(onClick = onViewAll) {
                Text("View All", color = AtmiyaAccent)
            }
        }
    }
}

@Composable
fun FundingCallCard(
    call: FundingCall,
    isRecommended: Boolean,
    onClick: () -> Unit
) {
    // Animation States
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "scale"
    )
    
    // Fetch Investor Details
    val repository = remember { FirestoreRepository() }
    var investorProfilePhoto by remember { mutableStateOf<String?>(null) }
    var investorLocation by remember { mutableStateOf<String?>(null) }
    
    LaunchedEffect(call.investorId) {
        if (call.investorId.isNotEmpty()) {
            try {
                val user = repository.getUser(call.investorId)
                investorProfilePhoto = user?.profilePhotoUrl
                investorLocation = user?.city
            } catch (e: Exception) {
                // Ignore
            }
        }
    }
    
    // Gradient Background (Red Accent)
    val gradientColors = if (isRecommended) {
        listOf(
            AtmiyaPrimary.copy(alpha = 0.1f),
            AtmiyaAccent.copy(alpha = 0.05f) // Red accent
        )
    } else {
        listOf(
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = if (isRecommended) 340.dp else 600.dp)
            .scale(scale)
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            modifier = Modifier
                .background(Brush.linearGradient(gradientColors))
                .padding(16.dp)
        ) {
            Column {
                // Header: Investor Info + Location
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Investor Avatar
                        if (investorProfilePhoto != null) {
                            AsyncImage(
                                model = investorProfilePhoto,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(Color.White),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(AtmiyaAccent.copy(alpha = 0.1f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = call.investorName.firstOrNull()?.uppercase() ?: "I",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = AtmiyaAccent,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = call.investorName,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    color = AtmiyaAccent.copy(alpha = 0.1f),
                                    shape = RoundedCornerShape(4.dp),
                                    border = androidx.compose.foundation.BorderStroke(0.5.dp, AtmiyaAccent.copy(alpha = 0.3f))
                                ) {
                                    Text(
                                        text = "Investor",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = AtmiyaAccent,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                    )
                                }
                            }
                            if (!investorLocation.isNullOrEmpty()) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.LocationOn,
                                        contentDescription = null,
                                        modifier = Modifier.size(12.dp),
                                        tint = Color.Gray
                                    )
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text(
                                        text = investorLocation!!,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.Gray
                                    )
                                }
                            }
                        }
                    }
                    
                    // Posted Date
                    if (call.createdAt != null) {
                        val dateFormat = SimpleDateFormat("dd MMM", Locale.getDefault())
                        Text(
                            text = "Posted ${dateFormat.format(call.createdAt.toDate())}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Gray
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Title
                Text(
                    text = call.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Key Stats Row (Funding & Timer)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Funding Amount
                    Column {
                        Text(
                            text = "Total Funding",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Gray
                        )
                        val formattedAmount = try {
                            val amount = call.maxTicketAmount.toLongOrNull() ?: 0L
                            java.text.NumberFormat.getCurrencyInstance(java.util.Locale("en", "IN")).format(amount)
                        } catch (e: Exception) {
                            "₹${call.maxTicketAmount}"
                        }
                        Text(
                            text = formattedAmount,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = AtmiyaAccent // Red
                        )
                    }
                    
                    // Timer
                    if (call.applicationDeadline != null) {
                        CardTimer(deadline = call.applicationDeadline)
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Footer: Sectors + Arrow
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Sector Chips
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        call.sectors.take(2).forEach { sector ->
                            Surface(
                                color = MaterialTheme.colorScheme.surface,
                                shape = RoundedCornerShape(8.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Text(
                                    text = sector,
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = "View Details",
                        tint = AtmiyaAccent, // Red
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun CardTimer(deadline: Timestamp) {
    var timeLeft by remember { mutableStateOf("") }
    
    LaunchedEffect(deadline) {
        while (true) {
            val diff = deadline.toDate().time - System.currentTimeMillis()
            if (diff <= 0) {
                timeLeft = "Closed"
                break
            }
            val days = TimeUnit.MILLISECONDS.toDays(diff)
            val hours = TimeUnit.MILLISECONDS.toHours(diff) % 24
            val minutes = TimeUnit.MILLISECONDS.toMinutes(diff) % 60
            timeLeft = String.format("%dd %02dh %02dm", days, hours, minutes)
            delay(60000) // Update every minute for list view to save resources
        }
    }

    Surface(
        color = AtmiyaAccent.copy(alpha = 0.1f),
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, AtmiyaAccent.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Timer,
                contentDescription = null,
                modifier = Modifier.size(12.dp),
                tint = AtmiyaAccent
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = timeLeft,
                style = MaterialTheme.typography.labelSmall,
                color = AtmiyaAccent,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun DeadlineBadge(deadline: Timestamp?) {
    if (deadline == null) return
    
    val daysLeft = remember(deadline) {
        val diff = deadline.toDate().time - System.currentTimeMillis()
        TimeUnit.MILLISECONDS.toDays(diff)
    }
    
    val (color, text) = when {
        daysLeft < 0 -> Color.Gray to "Closed"
        daysLeft < 3 -> Color.Red to "$daysLeft days left"
        daysLeft < 7 -> Color(0xFFFFA000) to "$daysLeft days left" // Amber
        else -> Color(0xFF4CAF50) to "$daysLeft days left" // Green
    }
    
    // Pulse animation for urgent deadlines
    val infiniteTransition = rememberInfiniteTransition(label = "urgency_pulse")
    val alpha by if (daysLeft in 0..2) {
        infiniteTransition.animateFloat(
            initialValue = 0.6f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(800, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "alpha"
        )
    } else {
        remember { mutableFloatStateOf(1f) }
    }

    Surface(
        color = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Schedule,
                contentDescription = null,
                modifier = Modifier.size(12.dp),
                tint = color.copy(alpha = alpha)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                color = color.copy(alpha = alpha),
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun FundingCallShimmer() {
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim = infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1200,
                easing = LinearEasing
            )
        ),
        label = "translate"
    )

    val brush = Brush.linearGradient(
        colors = listOf(
            Color.LightGray.copy(alpha = 0.6f),
            Color.LightGray.copy(alpha = 0.2f),
            Color.LightGray.copy(alpha = 0.6f)
        ),
        start = androidx.compose.ui.geometry.Offset.Zero,
        end = androidx.compose.ui.geometry.Offset(x = translateAnim.value, y = translateAnim.value)
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(brush)
        )
    }
}

@Composable
fun FundingCallsSummaryCard(
    count: Int,
    onClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.02f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .scale(if (count > 0) scale else 1f)
            .clickable { onClick() },
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            AtmiyaPrimary,
                            AtmiyaAccent // Red Gradient
                        )
                    )
                )
        ) {
            // Background Pattern/Icon
            Icon(
                imageVector = Icons.Default.AttachMoney,
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .size(140.dp)
                    .offset(x = 30.dp, y = 20.dp)
                    .graphicsLayer { alpha = 0.15f },
                tint = Color.White
            )

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Icon Circle
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.MonetizationOn,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.width(20.dp))

                Column {
                    Text(
                        text = "Active Funding Calls",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (count > 0) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.White)
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "$count New",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = AtmiyaAccent
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text(
                            text = if (count > 0) "Opportunities waiting" else "Check back soon",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.weight(1f))
                
                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}
