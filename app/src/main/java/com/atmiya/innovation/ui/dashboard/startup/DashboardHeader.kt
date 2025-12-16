package com.atmiya.innovation.ui.dashboard.startup

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import compose.icons.TablerIcons
import compose.icons.tablericons.Bell
import compose.icons.tablericons.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.atmiya.innovation.ui.components.UserAvatar
import com.atmiya.innovation.ui.theme.AtmiyaPrimary

@Composable
fun DashboardTopSection(
    userName: String,
    userPhotoUrl: String?,
    onNavigate: (String) -> Unit
) {
    Column {
        // 1. Curved Header Section
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
        ) {
            // The Curve Background
            Canvas(modifier = Modifier.fillMaxSize()) {
                val path = Path().apply {
                    moveTo(0f, 0f)
                    lineTo(size.width, 0f)
                    lineTo(size.width, size.height - 60.dp.toPx()) 
                    quadraticBezierTo(
                        size.width / 2, size.height, 
                        0f, size.height - 60.dp.toPx() 
                    )
                    close()
                }
                drawPath(path = path, color = AtmiyaPrimary)
                
                // Subtle Pattern Overlay
                drawCircle(
                    color = Color.White.copy(alpha = 0.05f),
                    radius = 120.dp.toPx(),
                    center = androidx.compose.ui.geometry.Offset(x = size.width * 0.9f, y = size.height * 0.2f)
                )
                drawCircle(
                    color = Color.White.copy(alpha = 0.03f),
                    radius = 180.dp.toPx(),
                    center = androidx.compose.ui.geometry.Offset(x = 0f, y = size.height * 0.5f)
                )
                drawCircle(
                    color = Color.White.copy(alpha = 0.04f),
                    radius = 80.dp.toPx(),
                    center = androidx.compose.ui.geometry.Offset(x = size.width * 0.25f, y = 0f)
                )
            }
            
            // Header Content
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .padding(top = 20.dp) // Status bar padding
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Welcome,",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                        Text(
                            text = userName,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1
                        )
                    }
                    
                    // Profile / Notification
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { /* TODO: Notifications */ }) {
                            Icon(
                                TablerIcons.Bell,
                                contentDescription = "Notifications",
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        UserAvatar(
                            model = userPhotoUrl,
                            name = userName,
                            modifier = Modifier
                                .clickable { onNavigate("profile_screen") },
                            size = 44.dp
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Text(
                    text = "Netfund provides access to\nfunding, mentors & investors",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
        
        // 2. Search Bar (Overlapping)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = (-50).dp) // Overlap upwards more significantly
                .padding(horizontal = 24.dp)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(28.dp),
                color = Color.White,
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable { /* TODO: Search */ }
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        TablerIcons.Search,
                        contentDescription = "Search",
                        tint = Color.Gray,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Search for network & funding...",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.Gray
                    )
                }
            }
        }
    }
}
