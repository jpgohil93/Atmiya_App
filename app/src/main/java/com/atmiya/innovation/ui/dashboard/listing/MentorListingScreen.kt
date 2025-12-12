package com.atmiya.innovation.ui.dashboard.listing

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import compose.icons.TablerIcons
import compose.icons.tablericons.ArrowLeft
import compose.icons.tablericons.Star
import compose.icons.tablericons.PlayerPlay
import compose.icons.tablericons.Check // Added
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.atmiya.innovation.data.Mentor
import com.atmiya.innovation.repository.FirestoreRepository
import com.atmiya.innovation.ui.theme.AtmiyaPrimary
import com.atmiya.innovation.ui.components.SoftScaffold // Assuming SoftScaffold is preferred, else just Scaffold
import com.atmiya.innovation.ui.components.CommonTopBar // Or just standard top bar with back navigation
import com.atmiya.innovation.ui.components.NetworkCard
import com.atmiya.innovation.ui.components.InfoRow
import com.atmiya.innovation.ui.components.PillBadge
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MentorListingScreen(
    onBack: () -> Unit,
    onMentorClick: (String) -> Unit
) {
    val repository = remember { FirestoreRepository() }

    // We can restart the flow by changing a key
    var key by remember { mutableStateOf(0) }

    // Using key to restart flow on refresh
    val mentorsFlow = remember(key) { repository.getMentorsFlow() }
    val mentors by mentorsFlow.collectAsState(initial = emptyList())

    var isRefreshing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        // Optional: Any initial setup
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mentors", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(TablerIcons.ArrowLeft, contentDescription = "Back", modifier = Modifier.size(28.dp))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        // If we want a spinner, we'd need a more complex flow wrapper.
        // For now, let's just show the list. Valid empty list is fine.
         SwipeRefresh(
            state = rememberSwipeRefreshState(isRefreshing),
            onRefresh = {
                scope.launch {
                    isRefreshing = true
                    key++ // Restart flow
                    kotlinx.coroutines.delay(1000) // Fake delay to show spinner
                    isRefreshing = false
                }
            },
            modifier = Modifier.fillMaxSize().padding(padding)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // DEBUG SECTION
                // Debug info removed as per request
                
                items(mentors) { mentor ->
                    MentorCard(user = mentor, onClick = { onMentorClick(mentor.uid) })
                }
            }
        }
    }
}

@Composable
fun MentorCard(user: Mentor, onClick: () -> Unit) {
    NetworkCard(
        imageModel = user.profilePhotoUrl ?: "",
        name = user.name,
        roleOrTitle = "${user.title}${if(user.experienceYears.isNotBlank()) ", ${user.experienceYears} Yrs Exp." else ""}",
        badges = {
             if (user.expertiseAreas.isNotEmpty()) {
                 PillBadge(
                    text = "${user.expertiseAreas.first()}",
                    backgroundColor = AtmiyaPrimary.copy(alpha = 0.1f),
                    contentColor = AtmiyaPrimary
                )
            }
        },
        infoContent = {
            if (user.expertiseAreas.isNotEmpty()) {
                InfoRow(
                    label = "Sectors",
                    value = user.expertiseAreas.take(3).joinToString(", ")
                )
            }
            if (user.topicsToTeach.isNotEmpty()) {
                InfoRow(
                    label = "Mentoring",
                    value = user.topicsToTeach.take(2).joinToString(", ")
                )
            }
        },
        primaryButtonText = "View Profile",
        onPrimaryClick = onClick,
        secondaryButtonText = "Book Session",
        onSecondaryClick = { /* Handle Book Session */ onClick() }
    )
}

// StatBadge removed as it is no longer used in this new design (replaced by NetworkCard structure)
