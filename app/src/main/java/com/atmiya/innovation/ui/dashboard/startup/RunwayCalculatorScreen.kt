package com.atmiya.innovation.ui.dashboard.startup

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.atmiya.innovation.ui.components.*
import com.atmiya.innovation.ui.theme.AtmiyaPrimary
import com.atmiya.innovation.ui.theme.AtmiyaSecondary
import androidx.compose.ui.unit.Dp
import compose.icons.TablerIcons
import compose.icons.tablericons.TrendingUp
import compose.icons.tablericons.AlertTriangle
import compose.icons.tablericons.Check
import compose.icons.tablericons.Rocket
import compose.icons.tablericons.InfoCircle
import compose.icons.tablericons.CalendarEvent
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RunwayCalculatorScreen(
    onBack: () -> Unit
) {
    var inputState by remember { mutableStateOf(InputState()) }
    var result by remember { mutableStateOf<CalculationResult?>(null) }
    
    // Helper to update state
    fun update(updateFn: (InputState) -> InputState) {
        inputState = updateFn(inputState)
        // Manual calculation only
        result = null // Reset result on change? Or keep stale? Better to reset to encourage re-calc or keep stale with generic warning.
        // User asked: "calculation should happen after clicking the submit button... unless and until it shouldn't provide the calculations"
        // So we should hide results or keep them stale. Let's hide them (set to null) to avoid confusion.
        result = null 
    }

    var showOneTimeCostDialog by remember { mutableStateOf(false) }

    if (showOneTimeCostDialog) {
        AddOneTimeCostDialog(
            onDismiss = { showOneTimeCostDialog = false },
            onConfirm = { newItem ->
                update { s -> s.copy(oneTimeCosts = s.oneTimeCosts + newItem) }
                showOneTimeCostDialog = false
            }
        )
    }



    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text("Runway Calculator", fontWeight = FontWeight.Bold)
                        Text("Survival & Planning", style = MaterialTheme.typography.bodySmall)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {
            
            // 1. Stage Selection
            item {
                Text(
                    "Startup Stage",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = AtmiyaPrimary,
                    modifier = Modifier.padding(top = 8.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                DropdownField(
                    label = "Select Stage",
                    options = StartupStage.values().map { it.label },
                    selectedOption = inputState.stage.label,
                    onOptionSelected = { label ->
                        val stage = StartupStage.values().find { it.label == label }!!
                        update { it.copy(stage = stage) }
                    }
                )
            }

            // 2. Common Financials
            item {
                SectionHeader("Base Financials")
                
                MoneyInput(
                    label = "Cash in Bank",
                    value = inputState.cashInBank,
                    onValueChange = { v -> update { it.copy(cashInBank = v) } }
                )
                Spacer(modifier = Modifier.height(12.dp))
                MoneyInput(
                    label = "Fixed Monthly Costs",
                    value = inputState.fixedMonthlyCosts,
                    onValueChange = { v -> update { it.copy(fixedMonthlyCosts = v) } }
                )
                Spacer(modifier = Modifier.height(12.dp))
                MoneyInput(
                    label = "Variable Monthly Costs (Avg)",
                    value = inputState.variableMonthlyCosts,
                    onValueChange = { v -> update { it.copy(variableMonthlyCosts = v) } }
                )
            }

            // 3. Stage Specific
            item {
                AnimatedVisibility(visible = true) {
                    Column {
                        SectionHeader("Stage Specifics: ${inputState.stage.label}")
                        
                        when (inputState.stage) {
                            StartupStage.IDEA_PRE_REVENUE -> {
                                DaysSelector(
                                    label = "Validation Target",
                                    selected = inputState.validationTargetDays,
                                    onSelect = { d -> update { it.copy(validationTargetDays = d) } }
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text("Monthly Validation Budgets", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(8.dp))
                                MoneyInput("Research / Surveys", inputState.monthlyResearchBudget) { v -> update { it.copy(monthlyResearchBudget = v) } }
                                Spacer(modifier = Modifier.height(8.dp))
                                MoneyInput("Landing Page / Ads", inputState.monthlyLandingPageBudget) { v -> update { it.copy(monthlyLandingPageBudget = v) } }
                                Spacer(modifier = Modifier.height(8.dp))
                                MoneyInput("Prototyping", inputState.monthlyPrototypeBudget) { v -> update { it.copy(monthlyPrototypeBudget = v) } }
                            }
                            
                            StartupStage.MVP_BUILDING -> {
                                DaysSelector(
                                    label = "MVP Build Target",
                                    selected = inputState.mvpTargetDays,
                                    onSelect = { d -> update { it.copy(mvpTargetDays = d) } }
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                MoneyInput("MVP Dev Spend (Monthly)", inputState.mvpMonthlySpend) { v -> update { it.copy(mvpMonthlySpend = v) } }
                                Spacer(modifier = Modifier.height(8.dp))
                                MoneyInput("Infra / Tools Cost", inputState.infraMonthlyCost) { v -> update { it.copy(infraMonthlyCost = v) } }
                                Spacer(modifier = Modifier.height(8.dp))
                                MoneyInput("Pilot Marketing Budget", inputState.pilotMarketingBudget) { v -> update { it.copy(pilotMarketingBudget = v) } }
                            }
                            
                            StartupStage.PILOT_USERS -> {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("Do you have revenue?", style = MaterialTheme.typography.bodyMedium)
                                    Spacer(modifier = Modifier.weight(1f))
                                    Switch(
                                        checked = inputState.hasRevenue,
                                        onCheckedChange = { c -> update { it.copy(hasRevenue = c) } }
                                    )
                                }
                                if (inputState.hasRevenue) {
                                    MoneyInput("Current Monthly Revenue", inputState.currentMonthlyRevenue) { v -> update { it.copy(currentMonthlyRevenue = v) } }
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                MoneyInput("Pilot Operations Cost", inputState.pilotOpsCost) { v -> update { it.copy(pilotOpsCost = v) } }
                                Spacer(modifier = Modifier.height(8.dp))
                                MoneyInput("Customer Acquisition Budget", inputState.customerAcquisitionBudget) { v -> update { it.copy(customerAcquisitionBudget = v) } }
                            }
                            
                            StartupStage.EARLY_REVENUE -> {
                                MoneyInput("Current Monthly Revenue", inputState.currentMonthlyRevenue) { v -> update { it.copy(currentMonthlyRevenue = v) } }
                                Spacer(modifier = Modifier.height(8.dp))
                                PercentInput("Gross Margin %", inputState.grossMarginPercent ?: 0.0) { v -> update { it.copy(grossMarginPercent = v) } }
                                Spacer(modifier = Modifier.height(8.dp))
                                PercentInput("MoM Growth Rate %", inputState.revenueGrowthRate) { v -> update { it.copy(revenueGrowthRate = v) } }
                            }
                        }
                    }
                }
            }
            
            // 4. One Time Costs
            item {
                SectionHeader("One-Time Costs (Optional)")
                inputState.oneTimeCosts.forEach { item ->
                    OneTimeCostRow(
                        item = item,
                        onDelete = { 
                            update { s -> s.copy(oneTimeCosts = s.oneTimeCosts - item) }
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
                
                OutlinedButton(
                    onClick = { showOneTimeCostDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Add, null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add One-Time Cost")
                }
            }

            item {
                Button(
                    onClick = { result = RunwayCalculatorEngine.calculate(inputState) },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AtmiyaPrimary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Calculate Runway", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }


            item {
                Divider(modifier = Modifier.padding(vertical = 16.dp))
            }
            
            // 5. Results
            if (result != null) {
                item {
                    ResultDashboard(result!!)
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
        color = Color.Gray,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}

@Composable
fun MoneyInput(label: String, value: Double, onValueChange: (Double) -> Unit) {
    ValidatedTextField(
        value = if (value == 0.0) "" else value.toLong().toString(), // integer input for simplicity
        onValueChange = { 
            if (it.isEmpty()) onValueChange(0.0)
            else if (it.all { c -> c.isDigit() }) onValueChange(it.toDouble()) 
        },
        label = label,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
        visualTransformation = IndianRupeeVisualTransformation()
    )
}

@Composable
fun PercentInput(label: String, value: Double, onValueChange: (Double) -> Unit) {
    ValidatedTextField(
        value = if (value == 0.0) "" else value.toString(),
        onValueChange = { 
             val v = it.toDoubleOrNull()
             if (v != null) onValueChange(v) else if (it.isEmpty()) onValueChange(0.0)
        },
        label = label,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
        errorMessage = if (value < 0 || value > 100) "Must be 0-100" else null
    )
}

@Composable
fun DaysSelector(label: String, selected: Int?, onSelect: (Int) -> Unit) {
    Column {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(30, 60, 90).forEach { days ->
                val isSelected = selected == days
                FilterChip(
                    selected = isSelected,
                    onClick = { onSelect(days) },
                    label = { Text("$days Days") },
                    leadingIcon = {
                        Icon(TablerIcons.CalendarEvent, null, modifier = Modifier.size(16.dp))
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

@Composable
fun OneTimeCostRow(item: OneTimeCostItem, onDelete: () -> Unit) {
    SoftCard(modifier = Modifier.fillMaxWidth(), padding = 8.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(item.name, fontWeight = FontWeight.Bold)
                Text("${formatCurrency(item.amount)} at Month ${item.monthOffset}", style = MaterialTheme.typography.bodySmall)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

// Convenience wrapper for SoftCard
@Composable
fun SoftCard(
    modifier: Modifier = Modifier, 
    padding: Dp = 16.dp, 
    backgroundColor: Color = MaterialTheme.colorScheme.surface,
    content: @Composable ColumnScope.() -> Unit
) {
    com.atmiya.innovation.ui.components.SoftCard(
        modifier = modifier, 
        backgroundColor = backgroundColor
    ) {
        Column(modifier = Modifier.padding(padding)) {
            content()
        }
    }
}


@Composable
fun ResultDashboard(result: CalculationResult) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        
        // Main Runway Card
        SoftCard(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = if (result.runwayMonths < 3) Color(0xFFFFEBEE) else if (result.runwayMonths > 12) Color(0xFFE8F5E9) else Color(0xFFFFF3E0)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Text("ESTIMATED RUNWAY", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (result.runwayMonths >= 24) "24+ Months" else "${result.runwayMonths} ${pluralize(result.runwayMonths, "Month")}",
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (result.runwayMonths < 3) Color.Red else if (result.runwayMonths > 12) Color(0xFF2E7D32) else Color(0xFFEF6C00)
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Zero Cash: ${result.zeroCashMonthApprox}", style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.width(4.dp))
                    InfoIconWithDialog("Zero Cash Date", "The estimated date when your cash balance will reach 0 based on current monthly burn.")
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Status Badge
                if (result.validationStatus != ValidationStatus.NA) {
                    val (color, text, icon) = when(result.validationStatus) {
                        ValidationStatus.ACHIEVABLE -> Triple(Color(0xFF2E7D32), "Validation Achievable", TablerIcons.Check)
                        ValidationStatus.AT_RISK -> Triple(Color(0xFFEF6C00), "Validation At Risk", TablerIcons.AlertTriangle)
                        ValidationStatus.NOT_ACHIEVABLE -> Triple(Color.Red, "Not Achievable", TablerIcons.AlertTriangle)
                        else -> Triple(Color.Gray, "", null)
                    }
                    Surface(color = color.copy(alpha=0.1f), shape = RoundedCornerShape(50)) {
                        Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                            if(icon!=null) Icon(icon, null, tint = color, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text, color = color, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
        }
        
        // Breakdown
        SoftCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Monthly Burn Breakdown", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(8.dp))
                    InfoIconWithDialog("Burn Breakdown", "Gross Burn: Total monthly spending including fixed costs, variable costs, and budgets.\n\nNet Burn: Gross burn minus revenue. This is the actual cash leaving your bank.")
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Gross Burn")
                    Text(formatCurrency(result.monthlyBurnBreakdown.grossBurn))
                }
                Divider(modifier = Modifier.padding(vertical = 8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Net Burn (after rev)", fontWeight = FontWeight.Bold)
                    Text(formatCurrency(result.monthlyBurnBreakdown.netBurn), fontWeight = FontWeight.Bold)
                }
            }
        }
        
        // Scenarios
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Scenarios", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.width(8.dp))
            InfoIconWithDialog("Scenarios", "Conservative: Assumes 5% higher costs and 10% lower revenue.\n\nAggressive: Assumes 5% lower costs and 15% higher growth.")
        }
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            ScenarioCard("Conservative", "${result.scenarios.conservative} Mo", Modifier.weight(1f))
            ScenarioCard("Aggressive", "${result.scenarios.aggressive} Mo", Modifier.weight(1f))
        }
        
        // Levers
        if (result.levers.isNotEmpty()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Actionable Levers", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.width(8.dp))
                InfoIconWithDialog("Levers", "Actionable steps you can take to extend your runway, calculated based on your specific inputs.")
            }
            result.levers.forEach { lever ->
                LeverCard(lever)
            }
        }
    }
}

@Composable
fun ScenarioCard(title: String, value: String, modifier: Modifier) {
    Surface(
        modifier = modifier.height(100.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(title, style = MaterialTheme.typography.labelMedium)
            Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun LeverCard(lever: LeverAction) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, AtmiyaPrimary.copy(alpha = 0.3f)),
        color = Color.White
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(TablerIcons.TrendingUp, null, tint = AtmiyaPrimary)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(lever.description, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            }
            Surface(color = AtmiyaPrimary, shape = RoundedCornerShape(8.dp)) {
                Text("+${lever.runwayDelta} Mo", color = Color.White, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
fun AddOneTimeCostDialog(
    onDismiss: () -> Unit,
    onConfirm: (OneTimeCostItem) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf(0.0) }
    var month by remember { mutableStateOf<String>("0") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add One-Time Cost") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name, 
                    onValueChange = { name = it }, 
                    label = { Text("Expense Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                MoneyInput("Amount", amount) { amount = it }
                
                OutlinedTextField(
                    value = month,
                    onValueChange = { if(it.all { c -> c.isDigit() }) month = it },
                    label = { Text("Month Offset (0-12)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                if (name.isNotEmpty() && amount > 0) {
                    val m = month.toIntOrNull()?.coerceIn(0, 12) ?: 0
                    onConfirm(OneTimeCostItem(name = name, amount = amount, monthOffset = m))
                }
            }) { Text("Add") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

fun formatCurrency(amount: Double): String {
    val format = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
    format.maximumFractionDigits = 0
    return format.format(amount)
}

fun pluralize(count: Int, singular: String, plural: String = singular + "s"): String {
    return if (count == 1) singular else plural
}

@Composable
fun InfoIconWithDialog(title: String, text: String) {
    var showDialog by remember { mutableStateOf(false) }
    
    IconButton(onClick = { showDialog = true }, modifier = Modifier.size(20.dp)) {
        Icon(TablerIcons.InfoCircle, "Info", tint = MaterialTheme.colorScheme.primary)
    }
    
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            confirmButton = { TextButton(onClick = { showDialog = false }) { Text("OK") } },
            title = { Text(title) },
            text = { Text(text) }
        )
    }
}
