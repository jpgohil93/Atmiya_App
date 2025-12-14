package com.atmiya.innovation.ui.dashboard.home

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.Star // Replaced TrendingUp
import androidx.compose.material.icons.filled.Info // Replaced Campaign
import androidx.compose.material.icons.filled.List // Replaced Assignment/Forum
import androidx.compose.material.icons.filled.Settings // Replaced Tune
import androidx.compose.runtime.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import com.atmiya.innovation.repository.FirestoreRepository
import com.google.firebase.auth.FirebaseAuth
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.launch
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.text.font.FontWeight

@Composable
fun InvestorHome(
    isTabVisible: Boolean = true,
    onNavigate: (String) -> Unit
) {
    val repository = remember { FirestoreRepository() }
    val auth = FirebaseAuth.getInstance()
    var userName by remember { mutableStateOf("Investor") }
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        val user = auth.currentUser
        if (user != null) {
            val userProfile = repository.getUser(user.uid)
            userName = userProfile?.name ?: "Investor"
        }
    }

    // Video Slider State
    val viewModel = remember { com.atmiya.innovation.ui.dashboard.startup.StartupDashboardViewModel() }
    val videosState by viewModel.featuredVideos.collectAsState()

    // Viral GIF Logic
    var viralGifUrl by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        try {
            val gsReference = com.google.firebase.storage.FirebaseStorage.getInstance()
                .getReferenceFromUrl("gs://atmiya-eacdf.firebasestorage.app/Date 04122025 Viral (1080 x 300 px).gif")
            val downloadUrl = gsReference.downloadUrl.await()
            viralGifUrl = downloadUrl.toString()
        } catch (e: Exception) {
            // Ignore
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp)
    ) {
        // 1. Hero Video
        item {
            com.atmiya.innovation.ui.dashboard.SharedDashboardHeader(
                videosState = videosState,
                isVisible = isTabVisible,
                onVideoClick = { videoId ->
                    onNavigate("video_detail/$videoId")
                }
            )
        }

        // 2. Accelerate Your Growth (Custom Layout)
        item {
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
                 // Registered Startups
                 com.atmiya.innovation.ui.dashboard.DashboardCard(
                    title = "Registered Startups",
                    subtitle = "Explore innovative ventures",
                    imageResId = com.atmiya.innovation.R.drawable.ic_startups,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { onNavigate("startups_list") }
                )

                // Row 1: My Funding Calls + Applications
                Row(
                     horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                     // Partner Card (My Funding Calls)
                     com.atmiya.innovation.ui.dashboard.DashboardCard(
                        title = "My Funding Calls",
                        subtitle = "Manage your calls",
                        modifier = Modifier.weight(1f),
                        imageResId = com.atmiya.innovation.R.drawable.ic_my_funding_calls,
                        onClick = { onNavigate("funding") }
                    )
                    
                    // Applications (Moved here)
                    com.atmiya.innovation.ui.dashboard.DashboardCard(
                        title = "Applications",
                        subtitle = "Review startups",
                        modifier = Modifier.weight(1f),
                        imageResId = com.atmiya.innovation.R.drawable.ic_applications,
                        onClick = { onNavigate("funding") }
                    )
                }

                // Row 2: Events + Smart Questions
                Row(
                     horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Events (Moved here)
                    com.atmiya.innovation.ui.dashboard.DashboardCard(
                        title = "Events",
                        subtitle = "Upcoming events",
                        imageResId = com.atmiya.innovation.R.drawable.ic_events,
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigate("events_list") }
                    )

                    // Smart Questions (Updated)
                    com.atmiya.innovation.ui.dashboard.DashboardCard(
                        title = "Smart Questions",
                        subtitle = "AI Due Diligence", 
                        modifier = Modifier.weight(1f),
                        imageResId = com.atmiya.innovation.R.drawable.ic_smart_questions,
                        onClick = {
                            android.widget.Toast.makeText(
                                context,
                                "Select a startup to Ask Anything", 
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                            onNavigate("startups_list") 
                        }
                    )
                }
            }
        }
        
        // 4. News Section
        item {
             com.atmiya.innovation.ui.dashboard.news.DashboardNewsSection(
                 onViewAllClick = { onNavigate("news_list") },
                 onNewsClick = { url -> 
                     // Handle generic news click, maybe open webview or browser
                 }
             )
        }
        
        // 5. Viral Banner
        if (viralGifUrl != null) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
                // Reuse ViralGifBanner logic or import it
                // Assuming ViralGifBanner is in components package
                 com.atmiya.innovation.ui.components.ViralGifBanner(
                     gifUrl = viralGifUrl!!,
                     modifier = Modifier.fillMaxWidth().wrapContentHeight()
                )
            }
        }
        
        item {
             Spacer(modifier = Modifier.height(30.dp))
        }
    }
}
