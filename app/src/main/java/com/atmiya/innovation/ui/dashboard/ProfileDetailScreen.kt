package com.atmiya.innovation.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Work
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
import com.atmiya.innovation.data.Investor
import com.atmiya.innovation.repository.FirestoreRepository
import com.atmiya.innovation.ui.components.*
import com.atmiya.innovation.ui.theme.AtmiyaPrimary
import com.atmiya.innovation.ui.theme.AtmiyaSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvestorDetailScreen(investorId: String, onBack: () -> Unit) {
    val repository = remember { FirestoreRepository() }
    var investor by remember { mutableStateOf<Investor?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(investorId) {
        try {
            investor = repository.getInvestor(investorId)
        } catch (e: Exception) {
            android.util.Log.e("InvestorDetailScreen", "Error fetching investor", e)
        } finally {
            isLoading = false
        }
    }

    SoftScaffold(
        topBar = {
            TopAppBar(
                title = { Text("Investor Profile", fontWeight = FontWeight.Bold) },
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
        } else if (investor == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Investor not found.")
            }
        } else {
            val inv = investor!!
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Profile Photo
                if (inv.profilePhotoUrl != null) {
                    AsyncImage(
                        model = inv.profilePhotoUrl,
                        contentDescription = null,
                        modifier = Modifier.size(120.dp).clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier.size(120.dp).clip(CircleShape).background(AtmiyaSecondary.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(inv.name.take(1), style = MaterialTheme.typography.displayMedium, color = AtmiyaPrimary)
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(inv.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(inv.firmName, style = MaterialTheme.typography.titleMedium, color = AtmiyaPrimary)
                Text(inv.city, style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        SectionHeader("About")
                        Text(inv.bio)
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        SectionHeader("Ticket Size")
                        Text("${inv.ticketSizeMin} - ${inv.ticketSizeMax}")
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        SectionHeader("Sectors of Interest")
                        Text(inv.sectorsOfInterest.joinToString(", "))
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Button(
                    onClick = { /* TODO: Send Proposal */ },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = AtmiyaSecondary)
                ) {
                    Text("Send Proposal")
                }
            }
        }
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = Color.Gray,
        modifier = Modifier.padding(bottom = 4.dp)
    )
}
