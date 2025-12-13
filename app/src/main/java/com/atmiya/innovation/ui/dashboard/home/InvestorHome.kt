package com.atmiya.innovation.ui.dashboard.home

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.Star // Replaced TrendingUp
import androidx.compose.material.icons.filled.Info // Replaced Campaign
import androidx.compose.material.icons.filled.List // Replaced Assignment/Forum
import androidx.compose.material.icons.filled.Settings // Replaced Tune
import androidx.compose.runtime.*
import com.atmiya.innovation.repository.FirestoreRepository
import com.atmiya.innovation.ui.components.BentoCardType
import com.atmiya.innovation.ui.components.BentoGrid
import com.atmiya.innovation.ui.components.BentoItem
import com.google.firebase.auth.FirebaseAuth
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.launch

@Composable
fun InvestorHome(
    onNavigate: (String) -> Unit
) {
    val repository = remember { FirestoreRepository() }
    val auth = FirebaseAuth.getInstance()
    var userName by remember { mutableStateOf("Investor") }

    LaunchedEffect(Unit) {
        val user = auth.currentUser
        if (user != null) {
            val userProfile = repository.getUser(user.uid)
            userName = userProfile?.name ?: "Investor"
        }
    }

    val items = listOf(
        BentoItem(
            type = BentoCardType.HERO,
            title = "Welcome, $userName",
            subtitle = "Discover the next big thing",
            icon = Icons.Default.Star,
            span = 2,
            onClick = { onNavigate("funding") }
        ),
        BentoItem(
            type = BentoCardType.FEATURE,
            title = "Startups Directory",
            subtitle = "Explore all registered startups",
            icon = Icons.Default.Search,
            span = 2,
            onClick = { onNavigate("startups_list") }
        ),
        BentoItem(
            type = BentoCardType.FEATURE,
            title = "My Funding Calls",
            subtitle = "Manage your calls",
            icon = Icons.Default.Info,
            onClick = { onNavigate("funding") }
        ),
        BentoItem(
            type = BentoCardType.FEATURE,
            title = "Applications",
            subtitle = "Review startups",
            icon = Icons.Default.List,
            badge = "New", // Placeholder until logic implemented
            onClick = { onNavigate("funding") }
        ),
        BentoItem(
            type = BentoCardType.FEATURE,
            title = "Community Wall",
            subtitle = "See what's happening",
            icon = Icons.Default.List,
            span = 2,
            onClick = { onNavigate("wall") }
        ),
        BentoItem(
            type = BentoCardType.UTILITY,
            title = "Preferences",
            icon = Icons.Default.Settings,
            onClick = { onNavigate("profile") }
        ),
        BentoItem(
            type = BentoCardType.UTILITY,
            title = "Profile",
            icon = Icons.Default.Person,
            onClick = { onNavigate("profile") }
        )
    )

    // Viral GIF Logic
    var viralGifUrl by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        try {
            val gsReference = com.google.firebase.storage.FirebaseStorage.getInstance()
                .getReferenceFromUrl("gs://atmiya-eacdf.firebasestorage.app/Date 04122025 Viral (1080 x 300 px).gif")
            val uri = gsReference.downloadUrl.await()
            viralGifUrl = uri.toString()
        } catch (e: Exception) {
            // Ignore
        }
    }

    BentoGrid(
        items = items,
        footer = {
            if (viralGifUrl != null) {
                com.atmiya.innovation.ui.components.ViralGifBanner(
                     gifUrl = viralGifUrl!!,
                     modifier = Modifier.fillMaxWidth().wrapContentHeight()
                )
            }
        }
    )
}
