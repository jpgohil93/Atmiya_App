package com.atmiya.innovation.ui.dashboard.home

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.Home // Replaced School
import androidx.compose.material.icons.filled.PlayArrow // Replaced VideoLibrary
import androidx.compose.material.icons.filled.Email // Replaced QuestionAnswer
import androidx.compose.material.icons.filled.List // Replaced Forum
import androidx.compose.runtime.*
import com.atmiya.innovation.repository.FirestoreRepository
import com.atmiya.innovation.ui.components.BentoCardType
import com.atmiya.innovation.ui.components.BentoGrid
import com.atmiya.innovation.ui.components.BentoItem
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

@Composable
fun MentorHome(
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

    val items = listOf(
        BentoItem(
            type = BentoCardType.HERO,
            title = "Welcome, $userName",
            subtitle = "Guide the next generation",
            icon = Icons.Default.Home,
            span = 2,
            onClick = { onNavigate("network") }
        ),
        BentoItem(
            type = BentoCardType.FEATURE,
            title = "My Videos",
            subtitle = "Manage your content",
            icon = Icons.Default.PlayArrow,
            onClick = { onNavigate("mentor_videos") }
        ),
        BentoItem(
            type = BentoCardType.FEATURE,
            title = "Requests",
            subtitle = "Startup inquiries",
            icon = Icons.Default.Email,
            badge = "Pending", // Placeholder
            onClick = { onNavigate("network") }
        ),
        BentoItem(
            type = BentoCardType.FEATURE,
            title = "Community Wall",
            subtitle = "Engage with startups",
            icon = Icons.Default.List,
            span = 2,
            onClick = { onNavigate("wall") }
        ),
        BentoItem(
            type = BentoCardType.UTILITY,
            title = "Profile",
            icon = Icons.Default.Person,
            onClick = { onNavigate("profile") }
        )
    )

    BentoGrid(items = items)
}
