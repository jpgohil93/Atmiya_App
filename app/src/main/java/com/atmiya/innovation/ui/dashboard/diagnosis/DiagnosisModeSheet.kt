package com.atmiya.innovation.ui.dashboard.diagnosis

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.atmiya.innovation.data.Startup
import com.atmiya.innovation.ui.theme.AtmiyaPrimary
import com.atmiya.innovation.ui.theme.AtmiyaSecondary
import compose.icons.TablerIcons
import compose.icons.tablericons.AlertTriangle
import compose.icons.tablericons.Bulb
import compose.icons.tablericons.ClipboardList
import compose.icons.tablericons.Target

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiagnosisModeSheet(
    startup: Startup,
    onDismiss: () -> Unit,
    viewModel: DiagnosisViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val advice by viewModel.adviceState.collectAsState()
    val isGeneratingAdvice by viewModel.isGeneratingAdvice.collectAsState()
    
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        if (uiState is DiagnosisUiState.Idle) {
            viewModel.generateDiagnosis(startup)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
                .fillMaxHeight(0.9f) // Tall sheet
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Startup Diagnosis",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text("AI-Powered Due Diligence Snapshot", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Outlined.Close, contentDescription = "Close")
                }
            }

            Divider(color = Color.LightGray.copy(alpha = 0.3f))
            Spacer(modifier = Modifier.height(16.dp))

            when (val state = uiState) {
                is DiagnosisUiState.Idle, is DiagnosisUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = AtmiyaPrimary)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Analysing signals...", color = Color.Gray)
                            Text("Generating diagnosis...", style = MaterialTheme.typography.bodySmall, color = Color.LightGray)
                        }
                    }
                }
                is DiagnosisUiState.Error -> {
                    Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(state.message, color = Color.Red, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { viewModel.generateDiagnosis(startup) },
                                colors = ButtonDefaults.buttonColors(containerColor = AtmiyaSecondary)
                            ) {
                                Icon(Icons.Outlined.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Retry")
                            }
                        }
                    }
                }
                is DiagnosisUiState.Success -> {
                    val diagnosis = state.data
                    
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                    ) {
                        // Section 1: What's Missing
                        DiagnosisSection(
                            title = "What's Missing",
                            icon = TablerIcons.ClipboardList,
                            color = Color(0xFFEF4444), // Red
                            items = diagnosis.missing
                        )
                        
                        Spacer(modifier = Modifier.height(24.dp))

                        // Section 2: Likely Failure Points
                        DiagnosisSection(
                            title = "Likely Failure Points",
                            icon = TablerIcons.AlertTriangle,
                            color = AtmiyaSecondary, // Orange
                            items = diagnosis.failurePoints
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        // Section 3: Focus Next
                        DiagnosisSection(
                            title = "If I were you, I'd focus on this next:",
                            icon = TablerIcons.Target,
                            color = AtmiyaPrimary, // Brand Blue
                            items = diagnosis.focusNext
                        )
                        
                        Spacer(modifier = Modifier.height(32.dp))

                        // Mentor Advice Section
                        if (advice.isBlank()) {
                            Button(
                                onClick = { viewModel.generateAdvice(startup, diagnosis) },
                                modifier = Modifier.fillMaxWidth().height(50.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Black, contentColor = Color.White),
                                shape = RoundedCornerShape(12.dp),
                                enabled = !isGeneratingAdvice
                            ) {
                                if (isGeneratingAdvice) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Drafting Advice...", fontWeight = FontWeight.SemiBold)
                                } else {
                                    Icon(TablerIcons.Bulb, contentDescription = null, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Generate Mentor Advice", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                }
                            }
                        } else {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF)), // Light Blue
                                border = androidx.compose.foundation.BorderStroke(1.dp, AtmiyaPrimary.copy(alpha=0.2f))
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        "Mentor Advice (Draft)",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = AtmiyaPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    // Editable text field could be here, but for now just selectable text
                                    Text(
                                        advice,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color(0xFF1E3A8A)
                                    )
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(32.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun DiagnosisSection(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    items: List<String>
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.Black)
    }
    Spacer(modifier = Modifier.height(8.dp))
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE5E7EB)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            if (items.isEmpty()) {
                Text("None identified.", style = MaterialTheme.typography.bodyMedium, color = Color.Gray, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
            } else {
                items.forEach { item ->
                    Row(modifier = Modifier.padding(bottom = 6.dp), verticalAlignment = Alignment.Top) {
                        Text("•", color = color, fontWeight = FontWeight.Bold, modifier = Modifier.padding(end = 8.dp))
                        Text(item, style = MaterialTheme.typography.bodyMedium, color = Color(0xFF374151))
                    }
                }
            }
        }
    }
}
