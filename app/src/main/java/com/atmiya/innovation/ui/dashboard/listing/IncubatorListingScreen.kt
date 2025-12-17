package com.atmiya.innovation.ui.dashboard.listing

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import com.atmiya.innovation.R
import com.atmiya.innovation.data.Incubator
import com.atmiya.innovation.ui.theme.AtmiyaPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IncubatorListingScreen(
    onBack: () -> Unit,
    onItemClick: (Incubator) -> Unit,
    viewModel: IncubatorViewModel = viewModel()
) {
    val filteredList by viewModel.filteredIncubators.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    
    val selectedState by viewModel.selectedState.collectAsState()
    val selectedSector by viewModel.selectedSector.collectAsState()
    val availableStates by viewModel.availableStates.collectAsState()
    val availableSectors by viewModel.availableSectors.collectAsState()

    val context = LocalContext.current
    var showStateSheet by remember { mutableStateOf(false) }
    var showSectorSheet by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            Column(modifier = Modifier.background(MaterialTheme.colorScheme.surface)) {
                TopAppBar(
                    title = { Text("Incubators", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
                )
                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.onSearchQueryChanged(it) },
                    placeholder = { Text("Search Incubators...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer, // Slightly darker than surface
                        focusedContainerColor = MaterialTheme.colorScheme.surface
                    ),
                    singleLine = true
                )
                
                // Filters Row
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        FilterButton(
                            text = selectedState ?: "State",
                            isActive = selectedState != null,
                            onClick = { 
                                if (selectedState != null) viewModel.onStateSelected(null) else showStateSheet = true 
                            }
                        )
                    }
                    item {
                        FilterButton(
                            text = selectedSector ?: "Sector",
                            isActive = selectedSector != null,
                            onClick = { 
                                if (selectedSector != null) viewModel.onSectorSelected(null) else showSectorSheet = true 
                            }
                        )
                    }
                }
                HorizontalDivider(color = Color.LightGray.copy(alpha = 0.2f))
            }
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow // Contrast for Surface Cards
    ) { paddingValues ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                items(filteredList) { incubator ->
                    IncubatorCard(incubator) { 
                        onItemClick(incubator)
                    }
                }
            }
        }
    }

    // Filter Sheets
    if (showStateSheet) {
        ModalBottomSheet(onDismissRequest = { showStateSheet = false }, containerColor = Color.White) {
            FilterSheetContent(
                title = "Select State",
                options = availableStates,
                selectedOption = selectedState,
                onSelect = { 
                    viewModel.onStateSelected(it)
                    showStateSheet = false
                }
            )
        }
    }
    
    if (showSectorSheet) {
        ModalBottomSheet(onDismissRequest = { showSectorSheet = false }, containerColor = Color.White) {
             FilterSheetContent(
                title = "Select Sector",
                options = availableSectors,
                selectedOption = selectedSector,
                onSelect = { 
                    viewModel.onSectorSelected(it)
                    showSectorSheet = false
                }
            )
        }
    }
}

@Composable
fun FilterButton(text: String, isActive: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = if (isActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal
            )
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                if (isActive) Icons.Default.Close else Icons.Default.FilterList,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun FilterSheetContent(
    title: String,
    options: List<String>,
    selectedOption: String?,
    onSelect: (String?) -> Unit
) {
    Column(modifier = Modifier.padding(16.dp).padding(bottom = 32.dp)) {
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
            item {
                Text(
                    "All",
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(null) }
                        .padding(vertical = 12.dp),
                    fontWeight = if (selectedOption == null) FontWeight.Bold else FontWeight.Normal,
                    color = if (selectedOption == null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
            }
            items(options) { option ->
                 Text(
                    option,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(option) }
                        .padding(vertical = 12.dp),
                    fontWeight = if (selectedOption == option) FontWeight.Bold else FontWeight.Normal,
                    color = if (selectedOption == option) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
            }
        }
    }
}

@Composable
fun IncubatorCard(incubator: Incubator, onClick: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Image on Left
                AsyncImage(
                   model = incubator.logoUrl,
                   contentDescription = null,
                   modifier = Modifier
                       .size(60.dp)
                       .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                   contentScale = ContentScale.Fit, // Changed to Fit to avoid cropping logos
                   error = rememberAsyncImagePainter(model = R.drawable.ic_launcher_foreground),
                   placeholder = rememberAsyncImagePainter(model = R.drawable.ic_launcher_foreground)
                )
                
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = incubator.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${incubator.city}, ${incubator.state}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                InfoItem(label = "Sector", value = incubator.sector)
                // InfoItem(label = "Funding", value = incubator.approvedFunding) // Removed to show left
                InfoItem(label = "Funds Left", value = incubator.remainingFunding)
            }
        }
    }
}

@Composable
fun InfoItem(label: String, value: String) {
    Column {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
    }
}
