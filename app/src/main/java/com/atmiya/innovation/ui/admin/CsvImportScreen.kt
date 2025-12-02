package com.atmiya.innovation.ui.admin

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.atmiya.innovation.data.ImportRecord
import com.atmiya.innovation.ui.theme.AtmiyaPrimary
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun CsvImportScreen(
    viewModel: ImportViewModel = viewModel()
) {
    val imports by viewModel.imports.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val context = LocalContext.current
    
    var selectedRole by remember { mutableStateOf("startup") }
    val roles = listOf("startup", "investor", "mentor")
    
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.uploadCsv(context, uri, selectedRole)
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Bulk Account Creation", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        
        // Role Selection
        Text("Select Role for Import:", style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            roles.forEach { role ->
                FilterChip(
                    selected = selectedRole == role,
                    onClick = { selectedRole = role },
                    label = { Text(role.capitalize()) }
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Actions
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            OutlinedButton(onClick = { 
                val csvContent = "Name,Email,Phone\nJohn Doe,john@example.com,+919876543210"
                val sendIntent = android.content.Intent().apply {
                    action = android.content.Intent.ACTION_SEND
                    putExtra(android.content.Intent.EXTRA_TEXT, csvContent)
                    type = "text/csv"
                    putExtra(android.content.Intent.EXTRA_TITLE, "Share CSV Template")
                }
                val shareIntent = android.content.Intent.createChooser(sendIntent, "Share Template")
                context.startActivity(shareIntent)
            }) {
                Text("Download Template")
            }
            Button(
                onClick = { launcher.launch("text/csv") }, // or text/* or application/vnd.ms-excel
                colors = ButtonDefaults.buttonColors(containerColor = AtmiyaPrimary)
            ) {
                Text("Upload CSV")
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text("Recent Imports", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        
        if (isLoading) {
            Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = AtmiyaPrimary)
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(imports) { record ->
                    ImportCard(record)
                }
            }
        }
    }
}

@Composable
fun ImportCard(record: ImportRecord) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp).fillMaxWidth()) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text(record.role.capitalize(), fontWeight = FontWeight.Bold)
                StatusBadge(record.status)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text("Total: ${record.totalRows} | Success: ${record.successCount} | Failed: ${record.failureCount}")
            Text(
                text = "CSV Format: Name, Email, Phone, City, State, [Role Field 1], [Role Field 2]",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Example: John Doe, john@example.com, 9876543210, Mumbai, Maharashtra, MyStartup, Tech",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
            )
            
            val date = record.createdAt?.toDate()
            if (date != null) {
                Text(
                    SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(date),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
        }
    }
}

@Composable
fun StatusBadge(status: String) {
    val color = when(status) {
        "completed" -> Color.Green
        "failed" -> Color.Red
        "processing" -> Color.Blue
        else -> Color.Gray
    }
    Text(
        text = status.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = color,
        fontWeight = FontWeight.Bold
    )
}

private fun String.capitalize(): String {
    return this.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
}
