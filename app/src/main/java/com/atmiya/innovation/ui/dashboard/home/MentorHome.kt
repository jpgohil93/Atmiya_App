package com.atmiya.innovation.ui.dashboard.home

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.Home // Replaced School
import androidx.compose.material.icons.filled.PlayArrow // Replaced VideoLibrary
import androidx.compose.material.icons.filled.Email // Replaced QuestionAnswer
import androidx.compose.material.icons.filled.List // Replaced Forum
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

@Composable
fun MentorHome(
    isTabVisible: Boolean = true,
    onNavigate: (String) -> Unit
) {
    val repository = remember { FirestoreRepository() }
    val auth = FirebaseAuth.getInstance()
    var userName by remember { mutableStateOf("Mentor") }

    LaunchedEffect(Unit) {
        val user = auth.currentUser
        if (user != null) {
            val userProfile = repository.getUser(user.uid)
            userName = userProfile?.name ?: "Mentor"
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

        // 2. Accelerate Your Growth (Startups + Partner + Events)
        item {
            com.atmiya.innovation.ui.dashboard.SharedGrowthSection(
                onNavigate = onNavigate,
                partnerCard = {
                    com.atmiya.innovation.ui.dashboard.DashboardCard(
                        title = "My Videos",
                        subtitle = "Manage your content",
                        modifier = Modifier.weight(1f),
                        imageResId = com.atmiya.innovation.R.drawable.ic_my_videos,
                        onClick = { onNavigate("mentor_videos") }
                    )
                }
            )
        }

        // 3. Mentor Specific Cards (Requests)
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                 com.atmiya.innovation.ui.dashboard.DashboardCard(
                    title = "Startup Diagnosis",
                    subtitle = "AI Analysis",
                    modifier = Modifier.weight(1f),
                    imageResId = com.atmiya.innovation.R.drawable.ic_diagnosis_3d, 
                    onClick = { onNavigate("startups_list") } 
                )

                com.atmiya.innovation.ui.dashboard.DashboardCard(
                    title = "Investors",
                    subtitle = "Connect with investors",
                    modifier = Modifier.weight(1f),
                    imageResId = com.atmiya.innovation.R.drawable.ic_investors,
                    onClick = { onNavigate("investors_list") }
                )
            }
        }
        
        // 4. News Section
        item {
             com.atmiya.innovation.ui.dashboard.news.DashboardNewsSection(
                 onViewAllClick = { onNavigate("news_list") },
                 onNewsClick = { url -> 
                     val encodedUrl = java.net.URLEncoder.encode(url, java.nio.charset.StandardCharsets.UTF_8.toString())
                     onNavigate("news_detail/$encodedUrl")
                 }
             )
        }

        // 5. Viral Banner
        if (viralGifUrl != null) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
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
