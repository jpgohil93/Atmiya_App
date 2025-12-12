package com.atmiya.innovation.ui.generator

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.atmiya.innovation.data.StartupIdea
import com.atmiya.innovation.repository.FirestoreRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import compose.icons.TablerIcons
import compose.icons.tablericons.FileText
import androidx.activity.compose.BackHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.background

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedIdeasScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val auth = FirebaseAuth.getInstance()
    val userId = auth.currentUser?.uid
    val firestoreRepo = remember { FirestoreRepository() }
    
    var ideas by remember { mutableStateOf<List<StartupIdea>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    
    LaunchedEffect(userId) {
        if (userId != null) {
            firestoreRepo.getSavedStartupIdeas(userId).collect {
                ideas = it
                isLoading = false
            }
        } else {
            isLoading = false
        }
    }
    
    var selectedIdea by remember { mutableStateOf<StartupIdea?>(null) }
    
    // Back handler to close detail view if open
    androidx.activity.compose.BackHandler(enabled = selectedIdea != null) {
        selectedIdea = null
    }

    if (selectedIdea != null) {
         // Full Screen Detail View as a nested Scaffold
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { 
                        Text(
                            text = "Idea Details",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        ) 
                    },
                    navigationIcon = {
                        IconButton(onClick = { selectedIdea = null }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
            }
        ) { detailPadding ->
            Box(
                modifier = Modifier
                    .padding(detailPadding)
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
            ) {
                 // Reusing the IdeaCard structure but inside a scrollable column for full screen
                 LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                 ) {
                     item {
                         IdeaCard(
                             idea = selectedIdea!!,
                             onSave = null,
                             showActions = false
                         )
                     }
                 }
            }
        }
    } else {
        // List View
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text("My Ideas") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            }
        ) { padding ->
            Box(modifier = Modifier.padding(padding).fillMaxSize()) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                } else if (ideas.isEmpty()) {
                    Text(
                        text = "No saved ideas yet.",
                        modifier = Modifier.align(Alignment.Center),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(ideas) { idea ->
                            SavedIdeaCard(
                                idea = idea, 
                                onClick = { selectedIdea = idea }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SavedIdeaCard(
    idea: StartupIdea,
    onClick: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        onClick = onClick
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = idea.name.ifBlank { "Untitled Idea" },
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = idea.oneLineSummary,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Tap to view details",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
