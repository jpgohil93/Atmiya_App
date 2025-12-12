package com.atmiya.innovation.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import compose.icons.TablerIcons
import compose.icons.tablericons.ArrowRight
import compose.icons.tablericons.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.atmiya.innovation.ui.theme.AtmiyaPrimary
import com.atmiya.innovation.ui.theme.AtmiyaSecondary

@Composable
fun CommonListingCard(
    imageModel: Any?,
    title: String,
    subtitle: String,
    tags: List<String>,
    metricValue: String,
    metricLabel: String,
    footerValue: String,
    footerIcon: ImageVector = TablerIcons.Star,
    connectionStatus: String = "self", // "none", "pending_sent", "pending_received", "connected", "self"
    onConnectAction: () -> Unit = {},
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row: Image + Details + Metric
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Profile Image with Black Ring
                Surface(
                    shape = CircleShape,
                    border = BorderStroke(2.dp, Color.Black), // Black Ring as requested
                    color = Color.Transparent,
                    modifier = Modifier.size(64.dp)
                ) {
                    AsyncImage(
                        model = imageModel,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(2.dp) // Spacing between ring and image
                            .clip(CircleShape)
                            .background(Color.Gray),
                        contentScale = ContentScale.Crop
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Middle: Title, Subtitle, Tags
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E3A8A) // Dark Blue/Navy for Name
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray,
                        maxLines = 1
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Tags
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        tags.take(2).forEachIndexed { index, tag ->
                            Surface(
                                color = if (index == 0) Color(0xFF00897B) else Color(0xFFF57C00), // Teal / Orange
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = tag,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                }
                
                // Right: Metric (Gold)
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = metricValue,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFC0CA33) // Gold/Yellowish
                    )
                    Text(
                        text = metricLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Black
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(12.dp))
            
            // Footer
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = footerIcon,
                        contentDescription = null,
                        tint = Color(0xFFC0CA33), // Gold Star
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = footerValue,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                }

                // Connection Action Button
                if (connectionStatus != "self") {
                    when (connectionStatus) {
                        "connected" -> {
                            Button(
                                onClick = onConnectAction,
                                colors = ButtonDefaults.buttonColors(containerColor = AtmiyaSecondary),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Text("Message", fontSize = 12.sp)
                            }
                        }
                        "pending_sent" -> {
                             OutlinedButton(
                                onClick = { /* No-op or Cancel */ },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                                modifier = Modifier.height(32.dp),
                                enabled = false
                            ) {
                                Text("Pending", fontSize = 12.sp)
                            }
                        }
                        "pending_received" -> {
                             Button(
                                onClick = onClick, // Navigate to detail/connections to accept
                                colors = ButtonDefaults.buttonColors(containerColor = AtmiyaPrimary),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Text("Respond", fontSize = 12.sp)
                            }
                        }
                        else -> { // "none" or null
                             OutlinedButton(
                                onClick = onConnectAction,
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                                modifier = Modifier.height(32.dp),
                                border = BorderStroke(1.dp, AtmiyaSecondary),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = AtmiyaSecondary)
                            ) {
                                Text("Connect", fontSize = 12.sp)
                            }
                        }
                    }
                } else {
                     Icon(
                        imageVector = TablerIcons.ArrowRight,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
