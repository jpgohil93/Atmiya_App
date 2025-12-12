package com.atmiya.innovation.ui.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.atmiya.innovation.data.Investor
import com.atmiya.innovation.repository.FirestoreRepository
import com.atmiya.innovation.ui.theme.AtmiyaPrimary
import com.atmiya.innovation.ui.theme.AtmiyaSecondary
import com.atmiya.innovation.ui.components.SoftCard

@Composable
fun InvestorsScreen(onInvestorClick: (String) -> Unit) {
    val repository = remember { FirestoreRepository() }
    var investors by remember { mutableStateOf<List<Investor>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        try {
            investors = repository.getAllInvestors()
        } catch (e: Exception) {
            // Handle error
        } finally {
            isLoading = false
        }
    }

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = AtmiyaPrimary)
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = "Connect with Investors",
                    style = MaterialTheme.typography.headlineMedium,
                    color = AtmiyaPrimary,
                    fontWeight = FontWeight.Bold
                )
            }
            items(investors) { investor ->
                InvestorCard(investor, onInvestorClick)
            }
        }
    }
}

@Composable
fun InvestorCard(investor: Investor, onInvestorClick: (String) -> Unit) {
    SoftCard(
        modifier = Modifier.fillMaxWidth(),
        elevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(50.dp),
                    shape = MaterialTheme.shapes.medium,
                    color = AtmiyaSecondary
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = investor.name.take(1),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(text = investor.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Text(text = investor.firmName, style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Sectors", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    Text(text = investor.sectorsOfInterest.joinToString(", "), style = MaterialTheme.typography.bodySmall)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(text = "Ticket Size", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    Text(text = "${investor.ticketSizeMin}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = { onInvestorClick(investor.uid) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = AtmiyaPrimary)
            ) {
                Text("View Profile")
            }
        }
    }
}

