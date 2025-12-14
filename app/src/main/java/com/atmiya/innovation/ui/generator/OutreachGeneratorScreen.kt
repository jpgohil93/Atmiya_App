package com.atmiya.innovation.ui.generator

import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.atmiya.innovation.repository.OutreachRepository
import com.atmiya.innovation.ui.components.SoftButton
import com.atmiya.innovation.ui.components.SoftCard
import com.atmiya.innovation.ui.theme.AtmiyaPrimary
import compose.icons.TablerIcons
import compose.icons.tablericons.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OutreachGeneratorScreen(
    onBack: () -> Unit
) {
    val viewModel: OutreachGeneratorViewModel = viewModel()
    val state = viewModel.startup.collectAsState().value
    val generatedMessage by viewModel.generatedMessage.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    
    val selectedType by viewModel.selectedType.collectAsState()
    val selectedStyle by viewModel.selectedStyle.collectAsState()
    
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pitch Generator", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        },
        containerColor = Color(0xFFF9FAFB)
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            
            // Header Info
            if (state != null) {
                Text(
                    text = "Generating for ${state.startupName}",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.Gray
                )
            }

            // 1. Configuration Section
            SoftCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("Configuration", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    
                    // Type Selection
                    SelectionGroup(
                        title = "Outreach Type",
                        options = OutreachRepository.OutreachType.values().toList(),
                        selected = selectedType,
                        onSelect = { viewModel.onTypeSelected(it) },
                        labelProvider = { it.displayName },
                        iconProvider = {
                            when(it) {
                                OutreachRepository.OutreachType.COLD_EMAIL -> TablerIcons.Mail
                                OutreachRepository.OutreachType.WHATSAPP -> TablerIcons.BrandWhatsapp
                                OutreachRepository.OutreachType.LINKEDIN -> TablerIcons.BrandLinkedin
                                OutreachRepository.OutreachType.FOLLOW_UP -> TablerIcons.Repeat
                            }
                        }
                    )
                    
                    HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))

                    // Language / Style Selection
                    SelectionGroup(
                        title = "Communication Style",
                        options = OutreachRepository.OutreachStyle.values().toList(),
                        selected = selectedStyle,
                        onSelect = { viewModel.onStyleSelected(it) },
                        labelProvider = { it.displayName },
                        iconProvider = {
                            when(it) {
                                OutreachRepository.OutreachStyle.PROFESSIONAL -> TablerIcons.Briefcase
                                OutreachRepository.OutreachStyle.FOUNDER_TO_FOUNDER -> TablerIcons.Users
                                OutreachRepository.OutreachStyle.CONFIDENT -> TablerIcons.TrendingUp
                                OutreachRepository.OutreachStyle.PROBLEM_LED -> TablerIcons.Bulb
                                OutreachRepository.OutreachStyle.NATIVE -> TablerIcons.Language
                            }
                        }
                    )
                }
            }

            // Generate Button
            SoftButton(
                text = if (isLoading) "Generating..." else "Generate Draft",
                onClick = { viewModel.generateMessage() },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                enabled = !isLoading && state != null,
                isLoading = isLoading
            )

            // 2. Output Section
            if (generatedMessage.isNotBlank()) {
                SoftCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Generated Draft", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Row {
                                IconButton(onClick = { 
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = android.content.ClipData.newPlainText("Pitch", generatedMessage)
                                    clipboard.setPrimaryClip(clip)
                                    Toast.makeText(context, "Copied!", Toast.LENGTH_SHORT).show()
                                }) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = AtmiyaPrimary)
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFF5F5F5), RoundedCornerShape(8.dp))
                                .border(1.dp, Color.LightGray.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                .padding(16.dp)
                        ) {
                            Text(
                                text = generatedMessage,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Black
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Text(
                            text = "Tip: Review and personalize before sending.",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Gray
                        )
                    }
                }
            }
            
            if (error != null) {
                Text(
                    text = error!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(8.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(50.dp))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun <T> SelectionGroup(
    title: String,
    options: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    labelProvider: (T) -> String,
    iconProvider: (T) -> ImageVector? = { null }
) {
    Column {
        Text(title, style = MaterialTheme.typography.labelMedium, color = Color.Gray)
        Spacer(modifier = Modifier.height(8.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            options.forEach { option ->
                val isSelected = option == selected
                FilterChip(
                    selected = isSelected,
                    onClick = { onSelect(option) },
                    label = { Text(labelProvider(option)) },
                    leadingIcon = {
                        val icon = iconProvider(option)
                        if (icon != null) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = AtmiyaPrimary.copy(alpha = 0.1f),
                        selectedLabelColor = AtmiyaPrimary,
                        selectedLeadingIconColor = AtmiyaPrimary
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = isSelected,
                        borderColor = if (isSelected) AtmiyaPrimary else Color.LightGray
                    )
                )
            }
        }
    }
}
