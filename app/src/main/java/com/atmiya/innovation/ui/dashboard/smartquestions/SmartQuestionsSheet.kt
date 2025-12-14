package com.atmiya.innovation.ui.dashboard.smartquestions

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.CopyAll
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.atmiya.innovation.data.Startup
import com.atmiya.innovation.ui.theme.AtmiyaPrimary
import com.atmiya.innovation.ui.theme.AtmiyaSecondary
import compose.icons.TablerIcons
import compose.icons.tablericons.Copy

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartQuestionsSheet(
    startup: Startup,
    pitchSummary: String, // Description or custom pitch
    onDismiss: () -> Unit,
    viewModel: SmartQuestionsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    // Trigger generation on first load if idle
    LaunchedEffect(Unit) {
        if (uiState is SmartQuestionsUiState.Idle) {
            viewModel.generateQuestions(startup, pitchSummary)
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
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Smart Questions",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text("AI Due Diligence", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Outlined.Close, contentDescription = "Close")
                }
            }

            Divider(color = Color.LightGray.copy(alpha = 0.3f))
            Spacer(modifier = Modifier.height(16.dp))

            when (val state = uiState) {
                is SmartQuestionsUiState.Idle, is SmartQuestionsUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxWidth().height(300.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = AtmiyaPrimary)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Analyzing Startup Profile...", color = Color.Gray)
                            Text("Generating smart questions...", style = MaterialTheme.typography.bodySmall, color = Color.LightGray)
                        }
                    }
                }
                is SmartQuestionsUiState.Error -> {
                    Box(modifier = Modifier.fillMaxWidth().height(300.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(state.message, color = Color.Red, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { viewModel.generateQuestions(startup, pitchSummary) },
                                colors = ButtonDefaults.buttonColors(containerColor = AtmiyaSecondary)
                            ) {
                                Icon(Icons.Outlined.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Retry")
                            }
                        }
                    }
                }
                is SmartQuestionsUiState.Success -> {
                    val questions = state.data.questions
                    
                    Column(
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .verticalScroll(rememberScrollState())
                    ) {
                        questions.forEach { (category, qList) ->
                            Text(
                                text = category.uppercase(),
                                style = MaterialTheme.typography.labelMedium,
                                color = AtmiyaPrimary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                            
                            qList.forEach { q ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 8.dp)
                                        .clickable {
                                            clipboardManager.setText(AnnotatedString(q))
                                            Toast.makeText(context, "Copied!", Toast.LENGTH_SHORT).show()
                                        },
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF9FAFB)),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE5E7EB))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Text(
                                            text = "•",
                                            color = AtmiyaSecondary,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(end = 8.dp)
                                        )
                                        Text(
                                            text = q,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Color(0xFF374151),
                                            modifier = Modifier.weight(1f)
                                        )
                                        Icon(
                                            TablerIcons.Copy,
                                            contentDescription = "Copy",
                                            tint = Color.LightGray,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Actions Footer
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Button(
                            onClick = { viewModel.generateQuestions(startup, pitchSummary) },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF3F4F6), contentColor = Color.Black)
                        ) {
                            Icon(Icons.Outlined.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Regenerate")
                        }
                        
                        Spacer(modifier = Modifier.width(16.dp))
                        
                        Button(
                            onClick = {
                                val allText = questions.flatMap { entry -> 
                                    listOf("\n--- ${entry.key} ---") + entry.value
                                }.joinToString("\n")
                                clipboardManager.setText(AnnotatedString(allText))
                                Toast.makeText(context, "All Questions Copied", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = AtmiyaPrimary)
                        ) {
                            Icon(Icons.Outlined.CopyAll, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Copy All")
                        }
                    }
                }
            }
        }
    }
}
