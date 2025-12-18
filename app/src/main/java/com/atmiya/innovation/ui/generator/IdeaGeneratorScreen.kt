package com.atmiya.innovation.ui.generator

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.atmiya.innovation.data.GeneratorInputs
import com.atmiya.innovation.data.StartupIdea
import compose.icons.TablerIcons
import compose.icons.tablericons.Bulb
import compose.icons.tablericons.DeviceFloppy
import com.atmiya.innovation.ui.theme.AtmiyaPrimary
import com.atmiya.innovation.ui.theme.AtmiyaSecondary

// ...

// --- Entry Card for Dashboard ---
@Composable
fun IdeaGeneratorEntryCard(
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier.background(
                brush = androidx.compose.ui.graphics.Brush.linearGradient(
                    colors = listOf(
                        AtmiyaPrimary,
                        AtmiyaSecondary
                    )
                )
            )
        ) {
            Row(
                modifier = Modifier.padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = "Startup Idea Generator",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Get ideas tailored to your skills & interests",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun IdeaGeneratorScreen(
    onBack: () -> Unit,
    viewModel: IdeaGeneratorViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showValidationErrors by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Idea Generator") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingVals ->
        Box(modifier = Modifier.padding(paddingVals).fillMaxSize()) {
            if (uiState.isLoading) {
                // Animated progress messages
                val progressMessages = listOf(
                    "Analyzing your profile...",
                    "Exploring market opportunities...",
                    "Matching ideas to your skills...",
                    "Crafting execution plans...",
                    "Almost there..."
                )
                var currentMessageIndex by remember { mutableStateOf(0) }
                
                LaunchedEffect(Unit) {
                    while (true) {
                        kotlinx.coroutines.delay(2000)
                        currentMessageIndex = (currentMessageIndex + 1) % progressMessages.size
                    }
                }
                
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = progressMessages[currentMessageIndex],
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "This may take a few seconds",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else if (uiState.currentStep == 4 && uiState.ideas.isNotEmpty()) {
                val context = androidx.compose.ui.platform.LocalContext.current
                IdeaResultScreen(
                    ideas = uiState.ideas,
                    onRegenerate = { viewModel.generateIdeas() },
                    onStartOver = { viewModel.resetGenerator() },
                    onSave = { viewModel.saveIdea(it) }
                )
            } else {
                // Input Steps
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .imePadding() // Handles keyboard overlap
                        .padding(20.dp)
                ) {
                    // Error Message Banner (inside Column, not overlay)
                    if (uiState.error != null) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = uiState.error ?: "Unknown Error",
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(16.dp),
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 5
                            )
                        }
                    }

                     // ... rest of input steps ...
                    // Stepper Indicator
                    LinearProgressIndicator(
                        progress = { uiState.currentStep / 3f }, // Fixed deprecated usage if applicable, or keep as simple float if not
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Text(
                        text = when(uiState.currentStep) {
                            1 -> "Step 1: Focus & Background"
                            2 -> "Step 2: Preferences"
                            else -> "Step 3: Execution Style"
                        },
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    // Step Content
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                    ) {
                        AnimatedContent(targetState = uiState.currentStep) { step ->
                            when (step) {
                                1 -> Step1Interests(uiState.inputs, viewModel::onSectorToggle, viewModel::onFieldChange, showValidationErrors)
                                2 -> Step2Constraints(uiState.inputs, viewModel::onFieldChange, showValidationErrors)
                                3 -> Step3Execution(uiState.inputs, viewModel::onFieldChange, showValidationErrors)
                            }
                        }
                    }

                    // Validation Logic
                    // Validation Logic

                    fun validateStep1(): Boolean {
                        return uiState.inputs.selectedSectors.isNotEmpty() &&
                               uiState.inputs.skills.isNotBlank() &&
                               uiState.inputs.problemsToSolve.isNotBlank()
                    }

                    fun validateStep2(): Boolean {
                        return uiState.inputs.budgetRange.isNotBlank() &&
                               uiState.inputs.timeAvailability.isNotBlank() &&
                               uiState.inputs.geography.isNotBlank()
                    }

                    fun validateStep3(): Boolean {
                        return uiState.inputs.preferredModel.isNotBlank()
                    }

                    // Navigation Buttons
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        if (uiState.currentStep > 1) {
                            OutlinedButton(onClick = { 
                                viewModel.prevStep() 
                                showValidationErrors = false // Reset on back
                            }) {
                                Text("Back")
                            }
                        } else {
                            Spacer(Modifier.width(1.dp))
                        }

                        Button(
                            onClick = { 
                                val isValid = when(uiState.currentStep) {
                                    1 -> validateStep1()
                                    2 -> validateStep2()
                                    3 -> validateStep3()
                                    else -> false
                                }

                                if (isValid) {
                                    showValidationErrors = false
                                    if (uiState.currentStep < 3) viewModel.nextStep() else viewModel.generateIdeas() 
                                } else {
                                    showValidationErrors = true
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text(if (uiState.currentStep == 3) "Generate Ideas" else "Next")
                        }
                    }
                }
            }
        }
    }
}

// --- Step 1: Interests ---
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun Step1Interests(
    inputs: GeneratorInputs,
    onSectorToggle: (String) -> Unit,
    onFieldChange: ((GeneratorInputs) -> GeneratorInputs) -> Unit,
    showErrors: Boolean = false // Added
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Column {
            Text("Select Sectors of Interest", fontWeight = FontWeight.Medium)
            if (showErrors && inputs.selectedSectors.isEmpty()) {
                Text("Please select at least one sector", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }
        }
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val sectors = listOf("FinTech", "HealthTech", "AgriTech", "EdTech", "Logistics", "Retail", "Social Impact", "SaaS", "AI/ML", "Manufacturing")
            sectors.forEach { sector ->
                FilterChip(
                    selected = inputs.selectedSectors.contains(sector),
                    onClick = { onSectorToggle(sector) },
                    label = { Text(sector) },
                    leadingIcon = if (inputs.selectedSectors.contains(sector)) {
                        { Icon(Icons.Default.Check, null) }
                    } else null
                )
            }
        }
        
        OutlinedTextField(
            value = inputs.skills,
            onValueChange = { if (it.length <= 100) { val str = it; onFieldChange { curr -> curr.copy(skills = str) } } },
            label = { Text("Key Skills / Experience") },
            placeholder = { Text("e.g. Sales, Python, Marketing") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            supportingText = { 
                if (showErrors && inputs.skills.isBlank()) {
                    Text("Required", color = MaterialTheme.colorScheme.error)
                } else {
                    Text("${inputs.skills.length}/100") 
                }
            },
            isError = showErrors && inputs.skills.isBlank(),
            singleLine = true
        )

        OutlinedTextField(
            value = inputs.problemsToSolve,
            onValueChange = { if (it.length <= 150) { val str = it; onFieldChange { curr -> curr.copy(problemsToSolve = str) } } },
            label = { Text("Problems you care about") },
            placeholder = { Text("e.g. Broken supply chains, Pollution") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            supportingText = { 
                if (showErrors && inputs.problemsToSolve.isBlank()) {
                    Text("Required", color = MaterialTheme.colorScheme.error)
                } else {
                    Text("${inputs.problemsToSolve.length}/150") 
                }
            },
            isError = showErrors && inputs.problemsToSolve.isBlank(),
            minLines = 2,
            maxLines = 3
        )
    }
}

// --- Step 2: Constraints ---
@Composable
fun Step2Constraints(
    inputs: GeneratorInputs,
    onFieldChange: ((GeneratorInputs) -> GeneratorInputs) -> Unit,
    showErrors: Boolean = false // Added
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Column {
            Text("Budget Range", fontWeight = FontWeight.Medium)
             if (showErrors && inputs.budgetRange.isBlank()) {
                Text("Please select a budget range", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }
        }
        SingleSelectChips(
            options = listOf("< ₹50k", "₹50k - 2L", "₹2L - 10L", "₹10L+"),
            selected = inputs.budgetRange,
            onSelect = { val str = it; onFieldChange { curr -> curr.copy(budgetRange = str) } }
        )

        Column {
            Text("Time Availability", fontWeight = FontWeight.Medium)
            if (showErrors && inputs.timeAvailability.isBlank()) {
                Text("Please select availability", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }
        }
        SingleSelectChips(
            options = listOf("Part-time/Evenings", "Full-time"),
            selected = inputs.timeAvailability,
            onSelect = { val str = it; onFieldChange { curr -> curr.copy(timeAvailability = str) } }
        )
        
        Column {
             Text("Geography", fontWeight = FontWeight.Medium)
             if (showErrors && inputs.geography.isBlank()) {
                Text("Please select geography", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }
        }
        SingleSelectChips(
            options = listOf("Local City", "Gujarat", "Pan-India", "Global"),
            selected = inputs.geography,
            onSelect = { val str = it; onFieldChange { curr -> curr.copy(geography = str) } }
        )
    }
}

// --- Step 3: Execution ---
@Composable
fun Step3Execution(
    inputs: GeneratorInputs,
    onFieldChange: ((GeneratorInputs) -> GeneratorInputs) -> Unit,
    showErrors: Boolean = false // Added
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Column {
             Text("Preferred Business Model", fontWeight = FontWeight.Medium)
             if (showErrors && inputs.preferredModel.isBlank()) {
                Text("Please select a business model", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }
        }
        SingleSelectChips(
            options = listOf("B2B", "B2C", "Marketplace", "SaaS", "Service-based"),
            selected = inputs.preferredModel,
            onSelect = { val str = it; onFieldChange { curr -> curr.copy(preferredModel = str) } }
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = inputs.isSoloFounder,
                onCheckedChange = { val b = it; onFieldChange { curr -> curr.copy(isSoloFounder = b) } }
            )
            Text("Solo Founder Friendly")
        }
        
         Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = inputs.isTechHeavy,
                onCheckedChange = { val b = it; onFieldChange { curr -> curr.copy(isTechHeavy = b) } }
            )
            Text("Tech Heavy Solution")
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SingleSelectChips(options: List<String>, selected: String, onSelect: (String) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { opt ->
            FilterChip(
                selected = (opt == selected),
                onClick = { onSelect(opt) },
                label = { Text(opt) }
            )
        }
    }
}

// --- Result Screen ---
// --- Result Screen ---
@Composable
fun IdeaResultScreen(
    ideas: List<StartupIdea>,
    onRegenerate: () -> Unit,
    onStartOver: () -> Unit,
    onSave: (StartupIdea) -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    
    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text("Generated Ideas", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text("Here are tailored startup concepts for you.", color = Color.Gray)
            }

            items(ideas) { idea ->
                IdeaCard(
                    idea = idea, 
                    onSave = { 
                        onSave(idea)
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                message = "Idea saved successfully!",
                                duration = SnackbarDuration.Short
                            )
                        }
                    },
                    showActions = true
                )
            }

            item {
                 Row(
                    modifier = Modifier.fillMaxWidth().padding(top=16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedButton(modifier = Modifier.weight(1f), onClick = onStartOver) {
                        Text("Start Over")
                    }
                    Button(modifier = Modifier.weight(1f), onClick = onRegenerate, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)) {
                        Text("Regenerate")
                    }
                }
            }
        }
        
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IdeaCard(
    idea: StartupIdea, 
    onSave: (() -> Unit)? = null,
    showActions: Boolean = true
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface) // Use surface color
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Header
            Row(verticalAlignment = Alignment.Top) {
                Box(Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) { // Use primaryContainer
                    Icon(TablerIcons.Bulb, null, tint = MaterialTheme.colorScheme.primary) // Use primary
                }
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(idea.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(idea.oneLineSummary, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (showActions && onSave != null) {
                    IconButton(onClick = onSave) {
                        Icon(TablerIcons.DeviceFloppy, contentDescription = "Save", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
            
            HorizontalDivider()
            
            DetailSection("Problem", idea.problem)
            DetailSection("Solution", idea.solution)
            DetailSection("Business Model", idea.businessModel)
            
            Text("Execution Plan", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            idea.executionPlan.forEach { phase ->
                 Column(Modifier.padding(start = 8.dp).fillMaxWidth()) {
                    Text("• ${phase.phaseName} (${phase.duration})", fontWeight = FontWeight.SemiBold)
                    phase.tasks.take(2).forEach { t ->
                        Text("  - $t", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                 }
            }
            
            // Action Buttons
            if (showActions && onSave != null) {
                HorizontalDivider()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    var isSaved by remember { mutableStateOf(false) }

                    OutlinedButton(
                        modifier = Modifier.weight(1f),
                        onClick = {
                            if (!isSaved) {
                                isSaved = true
                                onSave()
                            }
                        },
                        colors = if (isSaved) ButtonDefaults.outlinedButtonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.primary
                        ) else ButtonDefaults.outlinedButtonColors()
                    ) {
                        if (isSaved) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Saved")
                        } else {
                            Icon(TablerIcons.DeviceFloppy, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Save Idea")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DetailSection(title: String, content: String) {
    Column {
        Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        Text(content, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
    }
}
