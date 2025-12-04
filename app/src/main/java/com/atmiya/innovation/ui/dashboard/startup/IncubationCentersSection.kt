package com.atmiya.innovation.ui.dashboard.startup

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.atmiya.innovation.ui.theme.AtmiyaPrimary

@Composable
fun IncubationCentersSection() {
    val centers = remember {
        listOf(
            IncubationCenter(
                name = "Atmiya Incubation Center",
                location = "Rajkot, Gujarat",
                services = "Mentorship, Lab Access, Funding Support",
                imageUrl = "https://example.com/aic.jpg", // Placeholder
                color = Color(0xFFE8EAF6) // Light Indigo
            ),
            IncubationCenter(
                name = "i-Hub Gujarat",
                location = "Ahmedabad, Gujarat",
                services = "Networking, Policy Support, Grants",
                imageUrl = "https://example.com/ihub.jpg",
                color = Color(0xFFE0F2F1) // Light Teal
            ),
            IncubationCenter(
                name = "Startup Oasis",
                location = "Jaipur, Rajasthan",
                services = "Accelerator Programs, Co-working",
                imageUrl = "https://example.com/oasis.jpg",
                color = Color(0xFFFFF3E0) // Light Orange
            )
        )
    }
    
    LazyRow(
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(centers) { center ->
            IncubationCenterCard(center)
        }
    }
}

data class IncubationCenter(
    val name: String,
    val location: String,
    val services: String,
    val imageUrl: String,
    val color: Color
)

@Composable
fun IncubationCenterCard(center: IncubationCenter) {
    Card(
        modifier = Modifier
            .width(280.dp)
            .height(160.dp)
            .clickable { /* TODO */ },
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            // Left Color Strip
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(12.dp)
                    .background(center.color)
            )
            
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = center.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = center.location,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                }
                
                // Services Tags
                Column {
                    Text(
                        text = "Provides:",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = center.services,
                        style = MaterialTheme.typography.bodySmall,
                        color = AtmiyaPrimary,
                        maxLines = 2,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
