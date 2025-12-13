package com.atmiya.innovation.ui.funding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.material3.TabRow
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.atmiya.innovation.data.FundingCall
import com.atmiya.innovation.repository.FirestoreRepository
import com.atmiya.innovation.ui.dashboard.startup.FundingCallCard
import com.atmiya.innovation.ui.components.SoftScaffold
import com.atmiya.innovation.ui.components.SoftTextField
import com.atmiya.innovation.ui.theme.AtmiyaPrimary
import com.atmiya.innovation.ui.theme.AtmiyaSecondary
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FundingCallsScreen(
    role: String,
    onNavigate: (String) -> Unit,
    onBack: () -> Unit
) {
    val repository = remember { FirestoreRepository() }
    val auth = FirebaseAuth.getInstance()
    val scope = rememberCoroutineScope()
    
    // State for calls
    var allCalls by remember { mutableStateOf<List<FundingCall>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isRefreshing by remember { mutableStateOf(false) }

    // User State
    var userSector by remember { mutableStateOf<String?>(null) }

    // Fetch Logic
    val fetchData: () -> Unit = {
        scope.launch {
            try {
                // Fetch All Calls
                val calls = repository.getFundingCalls(limit = 100)
                allCalls = calls
                
                // Fetch User Sector if not loaded
                if (userSector == null) {
                    val userId = auth.currentUser?.uid
                    if (userId != null) {
                        val user = repository.getUser(userId)
                        userSector = user?.startupCategory
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("FundingScreen", "Error fetching calls", e)
            } finally {
                isLoading = false
                isRefreshing = false
            }
        }
    }

    LaunchedEffect(Unit) {
        fetchData()
    }
    
    // Refresh on Resume (e.g. coming back from Detail screen after Closing)
    val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                fetchData()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    
    // Filters State
    var searchQuery by remember { mutableStateOf("") }
    // Dynamic Categories: Merger of Standard Sectors + Extracted from loaded calls
    val dynamicCategories = remember(allCalls) {
        val standardSectors = listOf("Tech", "Fintech", "EdTech", "Healthcare", "AgriTech", "DeepTech", "SaaS", "AI/ML", "Consumer", "CleanTech", "Logistics", "Manufacturing")
        val extracted = allCalls.flatMap { it.sectors }.filter { it.isNotBlank() }
        val combined = (standardSectors + extracted).distinct().sorted()
        listOf("All") + combined
    }
    
    var selectedCategory by remember { mutableStateOf("All") }
    
    // Default Filter Logic: If user has a sector, default to it ONCE
    val hasSetDefault = remember { mutableStateOf(false) }
    LaunchedEffect(userSector, dynamicCategories) {
        if (!hasSetDefault.value && !userSector.isNullOrBlank()) {
             // Find matching category (case insensitive)
             val match = dynamicCategories.find { it.equals(userSector, ignoreCase = true) }
             if (match != null) {
                 selectedCategory = match
                 hasSetDefault.value = true
             }
        }
    }

    // Tabs for Status
    var selectedTab by remember { mutableIntStateOf(0) }
    
    // Dynamic Tabs based on Role
    val tabs = remember(role) {
        if (role == "investor") {
            listOf("My Calls", "Other Funding Calls", "Past Calls")
        } else {
            listOf("Active Opportunities", "Past Funding Calls")
        }
    }

    SoftScaffold(
        topBar = {
            Column(modifier = Modifier.background(Color.White)) {
                TopAppBar(
                    title = { Text("Funding Opportunities", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.White,
                        titleContentColor = AtmiyaPrimary
                    )
                )
                
                // Search and Filters Header
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    // --- DEBUG UI ---
    // Search Bar
                    SoftTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        label = "",
                        placeholder = "Search funding calls...",
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = AtmiyaPrimary) },
                        minLines = 1
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Tabs - Scrollable if needed, or fixed if fit
                    // Tabs - Scrollable only if more than 3 tabs
                    if (tabs.size > 3) {
                         ScrollableTabRow(
                            selectedTabIndex = selectedTab,
                            containerColor = Color.White,
                            contentColor = AtmiyaPrimary,
                            edgePadding = 0.dp,
                            indicator = { tabPositions ->
                                TabRowDefaults.SecondaryIndicator(
                                    Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                                    color = AtmiyaPrimary
                                )
                            }
                        ) {
                            tabs.forEachIndexed { index, title ->
                                Tab(
                                    selected = selectedTab == index,
                                    onClick = { selectedTab = index },
                                    text = { Text(title, maxLines = 1, style = MaterialTheme.typography.bodyMedium) }
                                )
                            }
                        }
                    } else {
                         TabRow(
                            selectedTabIndex = selectedTab,
                            containerColor = Color.White,
                            contentColor = AtmiyaPrimary,
                            indicator = { tabPositions ->
                                TabRowDefaults.SecondaryIndicator(
                                    Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                                    color = AtmiyaPrimary
                                )
                            }
                        ) {
                            tabs.forEachIndexed { index, title ->
                                Tab(
                                    selected = selectedTab == index,
                                    onClick = { selectedTab = index },
                                    text = { 
                                        Text(
                                            text = title, 
                                            maxLines = 2, 
                                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                            style = MaterialTheme.typography.bodyMedium
                                        ) 
                                    }
                                )
                            }
                        }
                    }
                   

                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Dynamic Category Filters
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        items(dynamicCategories) { category ->
                            FilterChip(
                                selected = selectedCategory == category,
                                onClick = { selectedCategory = category },
                                label = { Text(category) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = AtmiyaPrimary.copy(alpha = 0.1f),
                                    selectedLabelColor = AtmiyaPrimary
                                )
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
                HorizontalDivider(color = Color.LightGray.copy(alpha = 0.1f))
            }
        },
        floatingActionButton = {
            if (role == "investor") {
                FloatingActionButton(
                    onClick = { onNavigate("create_funding_call") },
                    containerColor = AtmiyaPrimary,
                    contentColor = Color.White,
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Create Call")
                }
            }
        }
    ) { innerPadding ->
        // Filtering Logic
        val currentUserId = auth.currentUser?.uid
        val filteredCalls = remember(allCalls, searchQuery, selectedCategory, selectedTab, role, currentUserId) {
            allCalls.filter { call ->
                // Search
                val matchesSearch = call.title.contains(searchQuery, ignoreCase = true) ||
                                  call.description.contains(searchQuery, ignoreCase = true) ||
                                  call.investorName.contains(searchQuery, ignoreCase = true)
                
                // Category - Strict Match
                val matchesCategory = selectedCategory == "All" || call.sectors.any { it.equals(selectedCategory, ignoreCase = true) }
                
                // Status & Role-based Filtering
                val isExpired = call.applicationDeadline?.toDate()?.before(java.util.Date()) == true
                val isActive = call.isActive
                
                val matchesTab = if (role == "investor") {
                     when(selectedTab) {
                         0 -> isActive && !isExpired && call.investorId == currentUserId // My Calls (Active)
                         1 -> isActive && !isExpired && call.investorId != currentUserId // Marketplace (Active by others)
                         2 -> !isActive || isExpired // Past (Generic - either closed manually OR expired)
                         else -> false
                     }
                } else {
                     if (selectedTab == 0) isActive && !isExpired else (!isActive || isExpired)
                }
                
                matchesSearch && matchesCategory && matchesTab
            }
        }

        SwipeRefresh(
            state = rememberSwipeRefreshState(isRefreshing),
            onRefresh = {
                isRefreshing = true
                fetchData()
            },
            modifier = Modifier.padding(innerPadding)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                if (isLoading && !isRefreshing) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = AtmiyaPrimary)
                    }
                } else if (filteredCalls.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            val emptyText = if (role == "investor") {
                                when(selectedTab) {
                                    0 -> "You haven't created any funding calls yet."
                                    1 -> "No active funding calls from other investors."
                                    else -> "No past funding calls found."
                                }
                            } else {
                                if (selectedTab == 0) "No active funding calls for this filter." 
                                else "No expired calls found."
                            }
                            
                            Text(
                                text = emptyText,
                                color = Color.Gray,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                } else {
                    androidx.compose.foundation.lazy.LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(16.dp)
                    ) {
                        items(filteredCalls) { call ->
                            // Modify opacity or visuals if expired
                            val isPastTab = if(role == "investor") selectedTab == 2 else selectedTab == 1
                            val alpha = if (isPastTab) 0.6f else 1f
                            val modifier = Modifier.graphicsLayer { this.alpha = alpha }
                            
                            Box(modifier = modifier) {
                                FundingCallCard(
                                    call = call,
                                    isRecommended = false,
                                    onClick = { onNavigate("funding_call/${call.id}") }
                                )
                                // Optional Overlay for Expired
                                if (isPastTab) {
                                    Box(
                                        modifier = Modifier
                                            .matchParentSize()
                                            .background(Color.White.copy(alpha = 0.3f))
                                            .clickable { /* Consumes click but maybe we want to allow viewing details */
                                                onNavigate("funding_call/${call.id}")
                                            }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
