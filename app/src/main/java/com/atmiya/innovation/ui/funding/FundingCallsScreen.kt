package com.atmiya.innovation.ui.funding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.atmiya.innovation.data.FundingCall
import com.atmiya.innovation.repository.FirestoreRepository
import com.atmiya.innovation.ui.dashboard.startup.FundingCallsSection
import com.atmiya.innovation.ui.theme.AtmiyaPrimary
import com.atmiya.innovation.ui.theme.AtmiyaAccent
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
    var recommendedCalls by remember { mutableStateOf<List<FundingCall>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        try {
            // Fetch All Calls
            allCalls = repository.getFundingCalls(limit = 50)
            
            // Fetch Recommended (based on user sector)
            val userId = auth.currentUser?.uid
            if (userId != null) {
                try {
                    val user = repository.getUser(userId)
                    val userSector = user?.startupCategory ?: ""
                    if (userSector.isNotEmpty()) {
                        recommendedCalls = repository.getRecommendedFundingCalls(userSector)
                    }
                } catch (e: Exception) {
                    android.util.Log.e("FundingScreen", "Error fetching user sector", e)
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("FundingScreen", "Error fetching calls", e)
        } finally {
            isLoading = false
        }
    }

    // State for filters
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }
    var showExpired by remember { mutableStateOf(false) }
    
    val categories = listOf("All", "Tech", "EdTech", "FinTech", "AgriTech", "HealthTech", "CleanTech")

    Scaffold(
        topBar = {
            Column(modifier = Modifier.background(Color.White)) {
                TopAppBar(
                    title = { Text("Funding Opportunities", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        TextButton(onClick = { showExpired = !showExpired }) {
                            Text(
                                text = if (showExpired) "Hide Expired" else "Show Expired",
                                color = AtmiyaPrimary,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.labelLarge
                            )
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
                    // Search Bar
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Search funding calls...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AtmiyaPrimary,
                            unfocusedBorderColor = Color.Gray.copy(alpha = 0.5f)
                        ),
                        singleLine = true
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Filters Row
                    // Category Chips
                    androidx.compose.foundation.lazy.LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(categories) { category ->
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
                HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))
            }
        },
        floatingActionButton = {
            if (role == "investor") {
                FloatingActionButton(
                    onClick = { onNavigate("create_funding_call") },
                    containerColor = AtmiyaPrimary,
                    contentColor = Color.White
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Create Call")
                }
            }
        }
    ) { innerPadding ->
        // Filter Logic
        val filteredCalls = remember(allCalls, searchQuery, selectedCategory, showExpired) {
            allCalls.filter { call ->
                val matchesSearch = call.title.contains(searchQuery, ignoreCase = true) ||
                                  call.description.contains(searchQuery, ignoreCase = true) ||
                                  call.investorName.contains(searchQuery, ignoreCase = true)
                
                val matchesCategory = selectedCategory == "All" || call.sectors.contains(selectedCategory)
                
                val isExpired = call.applicationDeadline?.toDate()?.before(java.util.Date()) == true
                val matchesStatus = showExpired || !isExpired
                
                matchesSearch && matchesCategory && matchesStatus
            }
        }

        Box(modifier = Modifier.padding(innerPadding)) {
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = AtmiyaPrimary)
                }
            } else if (filteredCalls.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (allCalls.isEmpty()) "No funding calls loaded." 
                                   else "No matching calls found.\nTry enabling 'Show Expired'.",
                            color = Color.Gray,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        if (allCalls.isNotEmpty()) {
                            Text(
                                text = "Loaded: ${allCalls.size} | Hidden: ${allCalls.size - filteredCalls.size}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.LightGray
                            )
                        }
                    }
                }
            } else {
                androidx.compose.foundation.lazy.LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    items(filteredCalls) { call ->
                        com.atmiya.innovation.ui.dashboard.startup.FundingCallCard(
                            call = call,
                            isRecommended = false,
                            onClick = { onNavigate("funding_call/${call.id}") }
                        )
                    }
                }
            }
        }
    }
}
