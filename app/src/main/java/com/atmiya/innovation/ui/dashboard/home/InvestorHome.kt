package com.atmiya.innovation.ui.dashboard.home

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import com.atmiya.innovation.repository.FirestoreRepository
import com.atmiya.innovation.ui.components.BentoCardType
import com.atmiya.innovation.ui.components.BentoGrid
import com.atmiya.innovation.ui.components.BentoItem
import com.google.firebase.auth.FirebaseAuth
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
            icon = Icons.Default.TrendingUp,
            span = 2,
            onClick = { onNavigate("funding") }
        ),
        BentoItem(
            type = BentoCardType.FEATURE,
            title = "My Funding Calls",
            subtitle = "Manage your calls",
            icon = Icons.Default.Campaign,
            onClick = { onNavigate("funding") }
        ),
        BentoItem(
            type = BentoCardType.FEATURE,
            title = "Applications",
            subtitle = "Review startups",
            icon = Icons.Default.Assignment,
            badge = "New", // Placeholder until logic implemented
            onClick = { onNavigate("funding") }
        ),
        BentoItem(
            type = BentoCardType.FEATURE,
            title = "Community Wall",
            subtitle = "See what's happening",
            icon = Icons.Default.Forum,
            span = 2,
            onClick = { onNavigate("wall") }
        ),
        BentoItem(
            type = BentoCardType.UTILITY,
            title = "Preferences",
            icon = Icons.Default.Tune,
            onClick = { onNavigate("profile") }
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
