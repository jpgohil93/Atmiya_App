package com.atmiya.innovation.ui.dashboard.startup

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange // Replaced CalendarToday
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.atmiya.innovation.data.AIFEvent
import com.atmiya.innovation.ui.theme.AtmiyaAccent
import com.atmiya.innovation.ui.theme.AtmiyaPrimary
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun AIFEventsSection(
    eventsState: StartupDashboardViewModel.UiState<List<AIFEvent>>,
    debugInfo: String = "",
    onEventClick: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp)
    ) {


        when (eventsState) {
            is StartupDashboardViewModel.UiState.Loading -> {
                EventsShimmer()
            }
            is StartupDashboardViewModel.UiState.Success -> {
                val events = eventsState.data
                if (events.isEmpty()) {
                    EmptyEventsState(debugInfo)
                } else {
                    Column(
                        modifier = Modifier.padding(horizontal = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        events.forEach { event ->
                            EventCard(event = event, onClick = { onEventClick(event.id) })
                        }
                    }
                }
            }
            is StartupDashboardViewModel.UiState.Error -> {
                ErrorEventsState(message = eventsState.message + "\nDebug: " + debugInfo)
            }
        }
    }
}

@Composable
fun EventCard(
    event: AIFEvent,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        border = BorderStroke(1.5.dp, Color.White), // More visible border
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Column {
            // Banner Image
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
            ) {
                AsyncImage(
                    model = event.bannerUrl ?: "https://via.placeholder.com/280x140/6366F1/FFFFFF?text=${event.title}",
                    contentDescription = event.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                
                // Status Badge
                // Use dynamic status instead of persisted status
                val displayStatus = event.dynamicStatus
                if (displayStatus.isNotEmpty()) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp),
                        shape = RoundedCornerShape(8.dp),
                        color = when (displayStatus.lowercase()) {
                            "upcoming" -> AtmiyaPrimary
                            "ongoing" -> AtmiyaAccent
                            "completed" -> Color.Gray
                            else -> AtmiyaPrimary
                        }
                    ) {
                        Text(
                            text = displayStatus.uppercase(),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            
            // Content
            Column(
                modifier = Modifier
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.85f), // More transparent
                                Color(0xFFE3F2FD).copy(alpha = 0.9f) // Slight blue tint for glass feel
                            )
                        )
                    )
                    .padding(20.dp)
            ) {
                // Title
                Text(
                    text = event.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black, // Ensure high contrast
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                
                // Date
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = null,
                        tint = Color.DarkGray,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = formatEventDateRange(event.startDate, event.endDate),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.DarkGray
                    )
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                // Venue
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = Color.DarkGray,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = event.venue,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.DarkGray,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // View Details Button
                Button(
                    onClick = onClick,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AtmiyaPrimary
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "View Details",
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
fun EventsShimmer() {
    Column(
        modifier = Modifier.padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        repeat(3) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(320.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.LightGray.copy(alpha = 0.3f))
            )
        }
    }
}

@Composable
fun EmptyEventsState(debugInfo: String = "") {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "No upcoming events",
            style = MaterialTheme.typography.bodyLarge,
            color = Color.Gray
        )
        if (debugInfo.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = debugInfo,
                style = MaterialTheme.typography.bodySmall,
                color = Color.Red.copy(alpha = 0.7f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            

        }
    }
}

@Composable
fun ErrorEventsState(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Unable to load events",
            style = MaterialTheme.typography.bodyLarge,
            color = Color.Gray
        )
    }
}

fun formatEventDateRange(startDate: Timestamp?, endDate: Timestamp?): String {
    if (startDate == null) return "Date TBD"
    
    val dateFormat = SimpleDateFormat("dd MMM", Locale.getDefault())
    val yearFormat = SimpleDateFormat("yyyy", Locale.getDefault())
    
    val startStr = dateFormat.format(startDate.toDate())
    val yearStr = yearFormat.format(startDate.toDate())
    
    return if (endDate != null && endDate != startDate) {
        val endStr = dateFormat.format(endDate.toDate())
        "$startStr - $endStr, $yearStr"
    } else {
        "$startStr, $yearStr"
    }
}
