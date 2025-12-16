package com.atmiya.innovation.ui.admin

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.atmiya.innovation.ui.theme.AtmiyaPrimary

@Composable
fun CsvImportScreen(
    viewModel: ImportViewModel = viewModel()
) {
    val validationSummary by viewModel.validationSummary.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val context = LocalContext.current
    
    // Use ViewModel's selectedUri so it persists across navigation
    val selectedUri by viewModel.selectedUri.collectAsState()
    
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.setSelectedUri(uri)
            viewModel.validateCsv(context, uri)
        }
    }

    val uploadProgress by viewModel.uploadProgress.collectAsState()
    val uploadStatusText by viewModel.uploadStatusText.collectAsState()
    val uploadResult by viewModel.uploadResult.collectAsState()
    
    // Delete confirmation dialog state
    var showDeleteDialog by remember { mutableStateOf(false) }
    var deleteConfirmText by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Bulk Startup Onboarding", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        
        // Actions
        if (uploadResult == null && !isLoading) {
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    OutlinedButton(
                        onClick = { 
                            val csvContent = "Full Name,Email,City,Region,Organization\nJohn Doe,john@example.com,Mumbai,Maharashtra,IIT Bombay"
                            val sendIntent = android.content.Intent().apply {
                                action = android.content.Intent.ACTION_SEND
                                putExtra(android.content.Intent.EXTRA_TEXT, csvContent)
                                type = "text/csv"
                                putExtra(android.content.Intent.EXTRA_TITLE, "Share Startup Template")
                            }
                            val shareIntent = android.content.Intent.createChooser(sendIntent, "Share Template")
                            context.startActivity(shareIntent)
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Download Template")
                    }
                    Button(
                        onClick = { 
                            viewModel.clearValidation()
                            launcher.launch("*/*") 
                        }, 
                        colors = ButtonDefaults.buttonColors(containerColor = AtmiyaPrimary),
                        modifier = Modifier.weight(1f)
                    ) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Select CSV File")
                    }
                }
                
                // Delete Utility (For Testing/Reset)
                OutlinedButton(
                    onClick = { showDeleteDialog = true },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.fillMaxWidth()
                ) {
                     Spacer(modifier = Modifier.width(8.dp))
                     Text("Delete All Bulk Users (Reset)")
                }
            }
            
            // Delete Confirmation Dialog
            if (showDeleteDialog) {
                androidx.compose.material3.AlertDialog(
                    onDismissRequest = { 
                        showDeleteDialog = false
                        deleteConfirmText = ""
                    },
                    title = { Text("⚠️ Confirm Deletion", fontWeight = FontWeight.Bold) },
                    text = {
                        Column {
                            Text("This will permanently delete ALL bulk-uploaded users, their startups, and bulk invites.")
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Type DELETE to confirm:", fontWeight = FontWeight.Medium)
                            Spacer(modifier = Modifier.height(8.dp))
                            androidx.compose.material3.OutlinedTextField(
                                value = deleteConfirmText,
                                onValueChange = { deleteConfirmText = it },
                                placeholder = { Text("DELETE") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    },
                    confirmButton = {
                        androidx.compose.material3.Button(
                            onClick = {
                                viewModel.deleteBulkData()
                                showDeleteDialog = false
                                deleteConfirmText = ""
                            },
                            enabled = deleteConfirmText == "DELETE",
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                        ) {
                            Text("Delete All")
                        }
                    },
                    dismissButton = {
                        androidx.compose.material3.TextButton(
                            onClick = { 
                                showDeleteDialog = false 
                                deleteConfirmText = ""
                            }
                        ) {
                            Text("Cancel")
                        }
                    }
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        if (isLoading) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                if (uploadProgress < 0) {
                    // Indeterminate progress for cloud upload
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(0.8f),
                        color = AtmiyaPrimary,
                    )
                } else {
                    // Determinate progress for local upload
                    LinearProgressIndicator(
                        progress = { uploadProgress },
                        modifier = Modifier.fillMaxWidth(0.8f),
                        color = AtmiyaPrimary,
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(if (uploadStatusText.isNotBlank()) uploadStatusText else "Processing...", color = Color.Gray)
            }
        } else if (uploadResult != null) {
            // Final Report
            val result = uploadResult!!
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4)), // Light green bg
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        if (result.failureCount == 0) androidx.compose.material.icons.Icons.Default.CheckCircle else androidx.compose.material.icons.Icons.Default.Warning,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = if (result.failureCount == 0) Color(0xFF4CAF50) else Color(0xFFFF9800)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        if (result.failureCount == 0) "Upload Successful!" else "Upload Completed with Errors",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Row(horizontalArrangement = Arrangement.spacedBy(32.dp)) {
                        SummaryCard("Total", result.totalCount.toString(), Color.Black)
                        SummaryCard("Success", result.successCount.toString(), Color(0xFF4CAF50))
                        SummaryCard("Failed", result.failureCount.toString(), if (result.failureCount > 0) Color.Red else Color.Black)
                    }
                    
                    if (result.failureCount > 0) {
                        Spacer(modifier = Modifier.height(24.dp))
                        OutlinedButton(
                            onClick = {
                                val errorContent = "Error Log:\n" + result.errors.joinToString("\n")
                                val sendIntent = android.content.Intent().apply {
                                    action = android.content.Intent.ACTION_SEND
                                    putExtra(android.content.Intent.EXTRA_TEXT, errorContent)
                                    type = "text/plain"
                                    putExtra(android.content.Intent.EXTRA_TITLE, "Upload Errors")
                                }
                                context.startActivity(android.content.Intent.createChooser(sendIntent, "Share Error Log"))
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red)
                        ) {
                            Text("Download Error Log")
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(32.dp))
                    Button(onClick = { viewModel.clearValidation() }) {
                        Text("Done")
                    }
                }
            }

        } else if (validationSummary.totalRows > 0) {
            // Summary UI
            Text("Validation Result", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                SummaryCard(
                    "Total Rows", 
                    validationSummary.totalRows.toString(), 
                    Color.Blue,
                    modifier = Modifier.weight(1f)
                )
                SummaryCard(
                    "Valid", 
                    validationSummary.validRows.toString(), 
                    if(validationSummary.invalidRows == 0 && validationSummary.validRows > 0) Color(0xFF4CAF50) else Color.Black,
                    modifier = Modifier.weight(1f)
                )
                SummaryCard(
                    "Invalid", 
                    validationSummary.invalidRows.toString(), 
                    if(validationSummary.invalidRows > 0) Color.Red else Color.Black,
                    modifier = Modifier.weight(1f)
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            if (validationSummary.invalidRows > 0) {
                Text("Errors Found:", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.error)
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    val invalidItems = validationSummary.rows.filter { !it.isValid }
                    items(invalidItems) { row ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("Line ${row.line}: ${row.data["phone"] ?: "Unknown Phone"}", fontWeight = FontWeight.Bold)
                                row.errors.forEach { error ->
                                    Text("• $error", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }
            } else {
                // All Valid State
                Column(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("All rows are valid!", style = MaterialTheme.typography.titleLarge, color = Color.Green)
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    if (validationSummary.validRows > 500) {
                        Text("Large dataset detected (${validationSummary.validRows} rows)", color = Color.Gray)
                        Text("Fast server-side processing available", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                    } else {
                        Text("Ready for upload (${validationSummary.validRows} rows)", color = Color.Gray)
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    if (validationSummary.validRows > 500) {
                        // Cloud Function Upload for large datasets
                        Button(
                            onClick = { 
                                selectedUri?.let { uri ->
                                    viewModel.uploadViaCloudFunction(context, uri)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3)),
                            modifier = Modifier.fillMaxWidth(0.7f).height(50.dp),
                            enabled = selectedUri != null
                        ) {
                            Text("⚡ Fast Upload")
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedButton(
                            onClick = { viewModel.uploadValidRows() },
                            modifier = Modifier.fillMaxWidth(0.5f)
                        ) {
                            Text("Upload Locally (Slow)")
                        }
                    } else {
                        Button(
                            onClick = { viewModel.uploadValidRows() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                            modifier = Modifier.fillMaxWidth(0.5f).height(50.dp)
                        ) {
                            Text("Upload to Server", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        } else {
            // Empty State
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("Upload a CSV file to validate", color = Color.Gray)
            }
        }
    }
}

@Composable
fun SummaryCard(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    ElevatedCard(
        modifier = modifier,
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = color)
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
