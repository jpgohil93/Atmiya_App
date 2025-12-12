package com.atmiya.innovation.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.atmiya.innovation.data.Startup
import com.atmiya.innovation.repository.FirestoreRepository
import com.atmiya.innovation.ui.components.SoftScaffold
import com.atmiya.innovation.ui.theme.AtmiyaPrimary
import com.atmiya.innovation.ui.theme.AtmiyaSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StartupDetailScreen(
    startupId: String,
    onBack: () -> Unit
) {
    val repository = remember { FirestoreRepository() }
    var startup by remember { mutableStateOf<Startup?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(startupId) {
        try {
            startup = repository.getStartup(startupId)
        } catch (e: Exception) {
            // Handle error
        } finally {
            isLoading = false
        }
    }

    SoftScaffold(
        topBar = {
            TopAppBar(
                title = { Text("Startup Profile", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = AtmiyaPrimary
                )
            )
        }
    ) { innerPadding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = AtmiyaPrimary)
            }
        } else if (startup == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Startup not found.")
            }
        } else {
            val s = startup!!
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Logo
                if (s.logoUrl != null) {
                    AsyncImage(
                        model = s.logoUrl,
                        contentDescription = null,
                        modifier = Modifier.size(120.dp).clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier.size(120.dp).clip(CircleShape).background(AtmiyaSecondary.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(s.startupName.take(1), style = MaterialTheme.typography.displayMedium, color = AtmiyaPrimary)
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(s.startupName, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(s.sector, style = MaterialTheme.typography.titleMedium, color = AtmiyaPrimary)
                Text(s.stage, style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        StartupSectionHeader("About")
                        Text(s.description)
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        StartupSectionHeader("Funding Ask")
                        Text(s.fundingAsk)
                        
                        Spacer(modifier = Modifier.height(16.dp))

                        StartupSectionHeader("Team Size")
                        Text(s.teamSize)
                        
                        if (s.website.isNotBlank()) {
                            Spacer(modifier = Modifier.height(16.dp))
                            StartupSectionHeader("Website")
                            Text(s.website, color = AtmiyaSecondary) // visual cue it is link-like
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StartupSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = Color.Gray,
        modifier = Modifier.padding(bottom = 4.dp)
    )
}
