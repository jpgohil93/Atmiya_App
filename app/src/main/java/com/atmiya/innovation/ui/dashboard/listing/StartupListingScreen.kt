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
    val filterOptions = listOf("All", "FinTech", "EdTech", "HealthTech", "AgriTech", "SaaS")

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
        containerColor = Color(0xFFF8F9FA), // Soft gray background
        topBar = {
            Column(modifier = Modifier.background(Color.White)) {
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
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
                )
                
                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search startups, sectors...", color = Color.Gray) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = Color(0xFFF3F4F6),
                        focusedContainerColor = Color.White,
                        unfocusedBorderColor = Color.Transparent,
                        focusedBorderColor = AtmiyaPrimary
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
                                selectedContainerColor = AtmiyaPrimary.copy(alpha = 0.1f),
                                selectedLabelColor = AtmiyaPrimary
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = selectedFilter == filter,
                                borderColor = if (selectedFilter == filter) AtmiyaPrimary else Color.Transparent
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
                    color = AtmiyaPrimary,
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
        colors = CardDefaults.cardColors(containerColor = Color.White),
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
                            .background(Color(0xFFF3F4F6)),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE0E7FF)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            startup.startupName.take(1).uppercase(),
                            style = MaterialTheme.typography.headlineSmall,
                            color = AtmiyaPrimary,
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
                                color = Color(0xFF1F2937),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    } else {
                        if (founderName.isNotBlank()) {
                             Text(
                                text = founderName,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF1F2937),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                         if (startup.sector.isNotBlank()) {
                             Text(
                                 text = startup.sector,
                                 style = MaterialTheme.typography.bodySmall,
                                 color = Color.Gray
                             )
                             if (startup.stage.isNotBlank()) {
                                 Text(" • ", color = Color.Gray)
                             }
                         }
                         if (startup.stage.isNotBlank()) {
                             Text(
                                 text = startup.stage,
                                 style = MaterialTheme.typography.bodySmall,
                                 color = Color.Gray
                             )
                         }
                    }
                }
                
                IconButton(onClick = { /* TODO: Save/Bookmark */ }) {
                    Icon(TablerIcons.Bookmark, contentDescription = "Save", tint = Color.LightGray)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Description Snippet
            if (startup.description.isNotBlank()) {
                Text(
                    text = startup.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF4B5563), // Cool gray
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
                        containerColor = Color(0xFF111827), // Dark Almost Black
                        contentColor = Color.White
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
