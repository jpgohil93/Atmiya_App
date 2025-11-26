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
import com.atmiya.innovation.ui.theme.AtmiyaPrimary
import com.atmiya.innovation.ui.theme.AtmiyaSecondary

data class Investor(
    val id: String,
    val name: String,
    val firmName: String,
    val sectors: List<String>,
    val ticketSize: String
)

@Composable
fun InvestorsScreen() {
    // Mock Data
    val investors = listOf(
        Investor("1", "Ravi Patel", "Gujarat Ventures", listOf("Agri-tech", "Food"), "₹10L - ₹50L"),
        Investor("2", "Sarah Lee", "Global Impact Fund", listOf("Health-tech", "Ed-tech"), "₹50L - ₹2Cr"),
        Investor("3", "Atmiya Angel Network", "AAN", listOf("All Sectors"), "₹5L - ₹25L")
    )

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
            InvestorCard(investor)
        }
    }
}

@Composable
fun InvestorCard(investor: Investor) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
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
                            fontWeight = FontWeight.Bold
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
                Column {
                    Text(text = "Sectors", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    Text(text = investor.sectors.joinToString(", "), style = MaterialTheme.typography.bodySmall)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(text = "Ticket Size", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    Text(text = investor.ticketSize, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = { /* TODO: View Details */ },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = AtmiyaPrimary)
            ) {
                Text("View Profile")
            }
        }
    }
}
