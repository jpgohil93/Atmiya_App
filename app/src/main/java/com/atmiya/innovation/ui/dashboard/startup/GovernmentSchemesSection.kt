package com.atmiya.innovation.ui.dashboard.startup

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.atmiya.innovation.ui.theme.AtmiyaPrimary
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.HorizontalPagerIndicator
import com.google.accompanist.pager.rememberPagerState

@OptIn(ExperimentalPagerApi::class)
@Composable
fun GovernmentSchemesSection() {
    val schemes = remember {
        listOf(
            Scheme(
                title = "Startup India Seed Fund",
                subtitle = "Financial assistance for proof of concept & prototype dev.",
                action = "Apply Now",
                color = Color(0xFF1A237E), // Dark Blue
                iconUrl = "https://www.startupindia.gov.in/content/dam/invest-india/newhomepage/Logo1.png" 
            ),
            Scheme(
                title = "Atal Innovation Mission",
                subtitle = "Promoting a culture of innovation and entrepreneurship.",
                action = "View Details",
                color = Color(0xFF004D40), // Dark Teal
                iconUrl = "https://aim.gov.in/images/aim-logo.png"
            ),
            Scheme(
                title = "Credit Guarantee Scheme",
                subtitle = "Collateral-free credit for startups.",
                action = "Check Eligibility",
                color = Color(0xFFBF360C), // Dark Orange
                iconUrl = "https://www.startupindia.gov.in/content/dam/invest-india/newhomepage/Logo1.png"
            )
        )
    }
    
    val pagerState = rememberPagerState()
    
    Column {
        HorizontalPager(
            count = schemes.size,
            state = pagerState,
            contentPadding = PaddingValues(horizontal = 20.dp),
            itemSpacing = 16.dp,
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
        ) { page ->
            SchemeBanner(scheme = schemes[page])
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Indicators
        HorizontalPagerIndicator(
            pagerState = pagerState,
            modifier = Modifier.align(Alignment.CenterHorizontally),
            activeColor = AtmiyaPrimary,
            inactiveColor = Color.Gray.copy(alpha = 0.3f),
            indicatorWidth = 8.dp,
            indicatorHeight = 8.dp
        )
    }
}

data class Scheme(
    val title: String,
    val subtitle: String,
    val action: String,
    val color: Color,
    val iconUrl: String
)

@Composable
fun SchemeBanner(scheme: Scheme) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = scheme.color)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon / Logo Area
            Surface(
                modifier = Modifier.size(80.dp),
                shape = CircleShape,
                color = Color.White
            ) {
                Box(contentAlignment = Alignment.Center) {
                    // Placeholder for logo if URL fails or just generic icon
                    Icon(
                        imageVector = Icons.Default.AccountBalance,
                        contentDescription = null,
                        tint = scheme.color,
                        modifier = Modifier.size(40.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Text Content
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = scheme.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = scheme.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.9f),
                    maxLines = 2,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(12.dp))
                
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color.White,
                    modifier = Modifier.clickable { /* TODO */ }
                ) {
                    Text(
                        text = scheme.action,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = scheme.color,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}
