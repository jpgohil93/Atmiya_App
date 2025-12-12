package com.atmiya.innovation.ui.dashboard.news

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.atmiya.innovation.data.NewsArticle
import com.atmiya.innovation.ui.theme.AtmiyaPrimary
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DashboardNewsSection(
    onViewAllClick: () -> Unit,
    onNewsClick: (String) -> Unit
) {
    val viewModel: NewsViewModel = viewModel()
    val newsArticles by viewModel.news.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    // Show top 5 items in the carousel
    val displayArticles = newsArticles.take(5)
    val pagerState = rememberPagerState(pageCount = { displayArticles.size })

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Startup News",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "View All",
                style = MaterialTheme.typography.bodyMedium,
                color = AtmiyaPrimary,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clickable { onViewAllClick() }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (isLoading) {
             Box(modifier = Modifier.fillMaxWidth().height(220.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = AtmiyaPrimary)
            }
        } else if (displayArticles.isEmpty()) {
             Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                Text("No news available", color = Color.Gray)
            }
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                HorizontalPager(
                    state = pagerState,
                    contentPadding = PaddingValues(horizontal = 24.dp),
                    pageSpacing = 16.dp, 
                    modifier = Modifier.fillMaxWidth()
                ) { page ->
                    NewsCard(
                        article = displayArticles[page], 
                        onClick = { onNewsClick(displayArticles[page].url) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp) // Taller card for immersive look
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Indicators
                Row(
                    Modifier
                        .wrapContentHeight()
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    repeat(pagerState.pageCount) { iteration ->
                        val color = if (pagerState.currentPage == iteration) AtmiyaPrimary else Color.LightGray.copy(alpha = 0.5f)
                        Box(
                            modifier = Modifier
                                .padding(3.dp)
                                .clip(CircleShape)
                                .background(color)
                                .size(if (pagerState.currentPage == iteration) 8.dp else 6.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun NewsCard(article: NewsArticle, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier
            .clickable { onClick() },
        shape = RoundedCornerShape(24.dp), // More rounded
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Full Bleed Image
            AsyncImage(
                model = article.imageUrl ?: "https://via.placeholder.com/400x220?text=News",
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            // Gradient Overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        androidx.compose.ui.graphics.Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.9f)),
                            startY = 200f // Adjust gradient start
                        )
                    )
            )

            // Content Overlay
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(20.dp)
            ) {
                // Source Badge
                Surface(
                    color = AtmiyaPrimary.copy(alpha = 0.9f),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Text(
                        text = article.sourceName.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                // Title
                Text(
                    text = article.title,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                // Date
                Text(
                    text = formatTime(article.publishedAt), 
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }
        }
    }
}

fun formatTime(isoString: String): String {
    // Basic helper to display date. 
    return try {
        isoString.substring(0, 10)
    } catch (e: Exception) {
        ""
    }
}
