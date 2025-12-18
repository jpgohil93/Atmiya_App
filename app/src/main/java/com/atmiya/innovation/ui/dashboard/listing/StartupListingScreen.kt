package com.atmiya.innovation.ui.dashboard.listing

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.atmiya.innovation.data.Startup
import com.atmiya.innovation.data.AppConstants
import com.atmiya.innovation.repository.FirestoreRepository
import com.atmiya.innovation.ui.theme.AtmiyaPrimary
import com.atmiya.innovation.ui.theme.AtmiyaSecondary
import compose.icons.TablerIcons
import compose.icons.tablericons.ArrowLeft
import compose.icons.tablericons.Bookmark
import compose.icons.tablericons.Filter
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StartupListingScreen(
    onBack: () -> Unit,
    onStartupClick: (String) -> Unit,
    onChatClick: (String, String) -> Unit = { _, _ -> }
) {
    val repository = remember { FirestoreRepository() }
    val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
    val scope = rememberCoroutineScope()
    val currentUser = auth.currentUser

    // -- State --
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("All") }
    val filterOptions = remember { listOf("All") + AppConstants.SECTOR_OPTIONS }

    val startupsFlow = remember { repository.getStartupsFlow() }
    val allStartups by startupsFlow.collectAsState(initial = emptyList())
    
    // User Role for Investor View
    var currentUserRole by remember { mutableStateOf("") }
    LaunchedEffect(currentUser) {
        if (currentUser != null) {
            val u = repository.getUser(currentUser.uid)
            currentUserRole = u?.role ?: ""
        }
    }
    
    // Filter startups based on search and selection
    val filteredStartups = remember(allStartups, searchQuery, selectedFilter) {
        allStartups.filter { startup ->
            val matchesSearch = startup.startupName.contains(searchQuery, ignoreCase = true) || 
                                startup.description.contains(searchQuery, ignoreCase = true)
            val matchesFilter = if (selectedFilter == "All") true else startup.sector.contains(selectedFilter, ignoreCase = true)
            matchesSearch && matchesFilter
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow, // Soft gray background
        topBar = {
            Column(modifier = Modifier.background(MaterialTheme.colorScheme.surface)) {
                TopAppBar(
                    title = { 
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Explore Startups", fontWeight = FontWeight.Bold, fontSize = 24.sp)
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(TablerIcons.ArrowLeft, contentDescription = "Back", modifier = Modifier.size(24.dp))
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
                )
                
                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search startups, sectors...", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedBorderColor = Color.Transparent,
                        focusedBorderColor = MaterialTheme.colorScheme.primary
                    ),
                    singleLine = true
                )

                // Filter Chips
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filterOptions) { filter ->
                        FilterChip(
                            selected = selectedFilter == filter,
                            onClick = { selectedFilter = filter },
                            label = { Text(filter) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = selectedFilter == filter,
                                borderColor = if (selectedFilter == filter) MaterialTheme.colorScheme.primary else Color.Transparent
                            )
                        )
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    "${filteredStartups.size} Startups Found", 
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }

            items(filteredStartups) { startup ->
                ModernStartupCard(
                    startup = startup,
                    onClick = { onStartupClick(startup.uid) },
                    isInvestor = currentUserRole == "investor"
                )
            }
        }
    }
}

@Composable
fun ModernStartupCard(
    startup: Startup,
    onClick: () -> Unit,
    isInvestor: Boolean = false
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .shadow(elevation = 2.dp, shape = RoundedCornerShape(16.dp), spotColor = Color(0x1A000000)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Logo
                if (!startup.logoUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = startup.logoUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    // Generate a consistent color based on startup name
                    val colors = listOf(
                        Color(0xFFEF5350), Color(0xFFEC407A), Color(0xFFAB47BC),
                        Color(0xFF7E57C2), Color(0xFF5C6BC0), Color(0xFF42A5F5),
                        Color(0xFF29B6F6), Color(0xFF26C6DA), Color(0xFF26A69A),
                        Color(0xFF66BB6A), Color(0xFF9CCC65), Color(0xFFFFCA28),
                        Color(0xFFFF7043), Color(0xFF8D6E63), Color(0xFF78909C)
                    )
                    val colorIndex = kotlin.math.abs(startup.startupName.hashCode()) % colors.size
                    val backgroundColor = colors[colorIndex]

                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(backgroundColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            startup.startupName.take(1).uppercase(),
                            style = MaterialTheme.typography.headlineSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Title & Subtitle
                val repository = remember { FirestoreRepository() }
                var founderName by remember { mutableStateOf("") }
                
                LaunchedEffect(startup.uid) {
                    val user = repository.getUser(startup.uid)
                    if (user != null) {
                        founderName = user.name
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    val titleText = if (isInvestor && founderName.isNotBlank()) founderName else startup.startupName
                    Text(
                        text = titleText,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    if (isInvestor) {
                        if (founderName.isNotBlank()) {
                             Text(
                                text = startup.startupName,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    } else {
                        if (founderName.isNotBlank()) {
                             Text(
                                text = founderName,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                         if (startup.sector.isNotBlank()) {
                             Text(
                                 text = startup.sector,
                                 style = MaterialTheme.typography.bodySmall,
                                 color = MaterialTheme.colorScheme.onSurfaceVariant
                             )
                             if (startup.stage.isNotBlank()) {
                                 Text(" • ", color = MaterialTheme.colorScheme.onSurfaceVariant)
                             }
                         }
                         if (startup.stage.isNotBlank()) {
                             Text(
                                 text = startup.stage,
                                 style = MaterialTheme.typography.bodySmall,
                                 color = MaterialTheme.colorScheme.onSurfaceVariant
                             )
                         }
                    }
                }
                
                val context = androidx.compose.ui.platform.LocalContext.current
                IconButton(onClick = { 
                    android.widget.Toast.makeText(context, "Update is coming soon", android.widget.Toast.LENGTH_SHORT).show()
                }) {
                    Icon(TablerIcons.Bookmark, contentDescription = "Save", tint = MaterialTheme.colorScheme.outlineVariant)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Description Snippet
            if (startup.description.isNotBlank()) {
                Text(
                    text = startup.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, // Cool gray
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            // Footer (Tags & Action)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Determine status tag (mock logic for demo, can be actual status)
                val statusText = if (startup.fundingAsk.isNotBlank()) "Raising" else "Bootstrapped"
                val statusColor = if (startup.fundingAsk.isNotBlank()) Color(0xFF10B981) else Color(0xFF6366F1) // Green vs Indigo
                
                Surface(
                    color = statusColor.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = statusText,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = statusColor,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Button(
                    onClick = onClick,
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.onSurface, // Dark Almost Black in Light, White in Dark (inverse)
                        contentColor = MaterialTheme.colorScheme.surface
                    ),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    Text("View Profile", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}
