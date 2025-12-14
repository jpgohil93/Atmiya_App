package com.atmiya.innovation.ui.dashboard.startup

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.atmiya.innovation.ui.components.NetworkCard
import com.atmiya.innovation.ui.components.InfoRow
import com.atmiya.innovation.ui.components.PillBadge
import com.atmiya.innovation.ui.components.SoftCard
import com.atmiya.innovation.ui.theme.AtmiyaPrimary
import com.atmiya.innovation.ui.theme.AtmiyaSecondary
import com.atmiya.innovation.ui.theme.AtmiyaAccent
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.Timestamp
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import compose.icons.TablerIcons
import compose.icons.tablericons.*

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
                        // For recommended horizontal list, we might want a slightly more compact version
                        // But for now, let's use the new card style but ensure width is constrained
                        Box(modifier = Modifier.width(320.dp)) {
                             FundingCallCard(
                                call = call,
                                isRecommended = true,
                                onClick = { onCallClick(call.id) }
                            )
                        }
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
    // Fetch Investor Details
    val repository = remember { FirestoreRepository() }
    val auth = remember { com.google.firebase.auth.FirebaseAuth.getInstance() }
    val currentUser = auth.currentUser
    val isOwner = currentUser?.uid == call.investorId
    
    var investorProfilePhoto by remember { mutableStateOf<String?>(null) }
    
    LaunchedEffect(call.investorId) {
        if (call.investorId.isNotEmpty()) {
            try {
                val user = repository.getUser(call.investorId)
                investorProfilePhoto = user?.profilePhotoUrl
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    NetworkCard(
        imageModel = investorProfilePhoto ?: "",
        name = call.investorName,
        roleOrTitle = call.title, // e.g. "Seed Round for Fintech"
        badges = {
            if (call.sectors.isNotEmpty()) {
                call.sectors.take(3).forEach { sector ->
                    PillBadge(
                        text = sector,
                        backgroundColor = Color(0xFFF3E5F5), // Light Purple
                        contentColor = Color(0xFF7B1FA2)
                    )
                }
            }
            if (call.stages.isNotEmpty()) {
                PillBadge(
                     text = call.stages.first(),
                     backgroundColor = Color(0xFFE3F2FD), // Light Blue
                     contentColor = Color(0xFF1976D2)
                )
            }
        },
        infoContent = {
            // Sectors Row
            val sectorsText = if (call.sectors.isNotEmpty()) call.sectors.take(3).joinToString(", ") else "General"
            InfoRow(label = "Sectors", value = sectorsText)
            
            // Typical Check (Ticket Size)
            val ticketStr = try {
                 val min = call.minTicketAmount.toLongOrNull() ?: 0L
                 "Ticket: > ${formatAmountSimple(min)}"
            } catch (e: Exception) {
                "Ticket: ${call.minTicketAmount}"
            }
             
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Typical Check", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                PillBadge(text = ticketStr)
            }

            // Investment Type (Equity)
            val equityStr = if (call.minEquity != null) "Equity ${call.minEquity}% - ${call.maxEquity}%" else "Equity"
            InfoRow(label = "Investment Type", value = equityStr)
            
            // Posted Time
            val timeAgo = remember(call.createdAt) {
                call.createdAt?.toDate()?.let { date ->
                    val diff = System.currentTimeMillis() - date.time
                    val days = TimeUnit.MILLISECONDS.toDays(diff)
                    val hours = TimeUnit.MILLISECONDS.toHours(diff)
                    when {
                        hours < 1 -> "Just now"
                        hours < 24 -> "${hours}h ago"
                        days < 7 -> "${days}d ago"
                        else -> java.text.SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(date)
                    }
                } ?: ""
            }
            if (timeAgo.isNotEmpty()) {
                InfoRow(label = "Posted", value = timeAgo)
            }
        },
        primaryButtonText = if (isOwner) "Review Opportunity" else "View Opportunity",
        onPrimaryClick = onClick,
        secondaryButtonText = if (isOwner) null else "Connect Now", // Or "Apply"
        onSecondaryClick = if (isOwner) { {} } else { onClick } // Navigate to detail for applying
    )
}

fun formatAmountSimple(amount: Long): String {
    return when {
         amount >= 10000000 -> "${String.format("%.0f", amount / 10000000.0)}Cr"
        amount >= 100000 -> "${String.format("%.0f", amount / 100000.0)}L"
        else -> java.text.NumberFormat.getInstance().format(amount)
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
        colors = CardDefaults.cardColors(containerColor = Color.White) // Shimmer on white card
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
    SoftCard(
        modifier = Modifier.fillMaxWidth().height(120.dp),
        onClick = onClick,
        backgroundColor = AtmiyaPrimary // Use Primary or Gradient
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
             // ... pattern ...
             Icon(
                imageVector = TablerIcons.InfoCircle,
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .size(140.dp)
                    .offset(x = 30.dp, y = 20.dp)
                    .graphicsLayer { alpha = 0.15f },
                tint = Color.White
            )

            Row(
                modifier = Modifier.fillMaxSize(), // Padding is in SoftCard
                verticalAlignment = Alignment.CenterVertically
            ) {
                 Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = TablerIcons.Star,
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
                     Text(
                        text = if (count > 0) "$count Opportunities" else "Check back soon",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }
             }
        }
    }
}
