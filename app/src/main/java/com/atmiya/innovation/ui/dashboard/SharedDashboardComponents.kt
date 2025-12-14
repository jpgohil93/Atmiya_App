package com.atmiya.innovation.ui.dashboard

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.atmiya.innovation.R
import com.atmiya.innovation.ui.dashboard.startup.HeroVideoSlider

@Composable
fun SharedDashboardHeader(
    videosState: Any?,
    isVisible: Boolean,
    onVideoClick: (String) -> Unit
) {
    Column(modifier = Modifier.padding(top = 0.dp)) {
        HeroVideoSlider(
            videosState = videosState,
            isVisible = isVisible,
            onVideoClick = onVideoClick
        )
    }
}

@Composable
fun SharedGrowthSection(
    onNavigate: (String) -> Unit,
    partnerCard: @Composable RowScope.() -> Unit
) {
    Column {
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Accelerate Your Growth",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
        )
        
        Column(
            modifier = Modifier.padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Registered Startups Card
            DashboardCard(
                title = "Registered Startups",
                subtitle = "Explore innovative ventures",
                imageResId = R.drawable.ic_startups,
                modifier = Modifier.fillMaxWidth(),
                onClick = { onNavigate("startups_list") }
            )

            // Row with Partner Card and Events Card
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                partnerCard()
                
                DashboardCard(
                    title = "Events",
                    subtitle = "Upcoming events",
                    imageResId = R.drawable.ic_events,
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigate("events_list") }
                )
            }
        }
    }
}

@Composable
fun DashboardCard(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    imageResId: Int? = null,
    onClick: () -> Unit,
    content: @Composable BoxScope.() -> Unit = {}
) {
    Surface(
        modifier = modifier
            .height(180.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(24.dp),
        color = Color.White,
        shadowElevation = 2.dp,
        tonalElevation = 2.dp
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Image area (takes top ~65% of card)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.65f)
                    .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            ) {
                if (imageResId != null) {
                    Image(
                        painter = painterResource(id = imageResId),
                        contentDescription = title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize()) {
                        content()
                    }
                }
            }
            
            // Text area with solid white background (takes bottom ~35%)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.35f)
                    .background(Color.White)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        letterSpacing = 0.5.sp
                    ),
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF263238), // Dark Blue Grey for premium look
                    fontSize = 17.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF78909C), // Softer Blue Grey
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
