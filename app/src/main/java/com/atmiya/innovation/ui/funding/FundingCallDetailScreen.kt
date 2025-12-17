package com.atmiya.innovation.ui.funding

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.atmiya.innovation.data.FundingApplication
import com.atmiya.innovation.data.FundingCall
import com.atmiya.innovation.data.Startup
import com.atmiya.innovation.data.User
import com.atmiya.innovation.repository.FirestoreRepository
import com.atmiya.innovation.ui.components.DetailRow
import com.atmiya.innovation.ui.components.QuickStatItem
import com.atmiya.innovation.ui.components.SectionHeader
import com.atmiya.innovation.ui.components.SoftScaffold
import com.atmiya.innovation.ui.theme.AtmiyaPrimary
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import compose.icons.TablerIcons
import compose.icons.tablericons.*
import java.util.UUID
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FundingCallDetailScreen(
    callId: String,
    onBack: () -> Unit,
    onApply: (String) -> Unit = {},
    onEdit: (String) -> Unit = {}
) {
    val repository = remember { FirestoreRepository() }
    val auth = FirebaseAuth.getInstance()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    
    var call by remember { mutableStateOf<FundingCall?>(null) }
    var investorProfile by remember { mutableStateOf<User?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var currentUser by remember { mutableStateOf<User?>(null) }
    var currentStartup by remember { mutableStateOf<Startup?>(null) } // Added
    var hasApplied by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(callId) {
        try {
            val user = auth.currentUser
            if (user != null) {
                currentUser = repository.getUser(user.uid)
                if (currentUser?.role == "startup") {
                    currentStartup = repository.getStartup(user.uid)
                    hasApplied = repository.hasApplied(callId, user.uid)
                }
            }

            val fetchedCall = repository.getFundingCall(callId)
            call = fetchedCall
            
            if (fetchedCall != null && fetchedCall.investorId.isNotEmpty()) {
                try {
                    investorProfile = repository.getUser(fetchedCall.investorId)
                } catch (e: Exception) {
                    android.util.Log.e("FundingDetail", "Error fetching investor", e)
                }
            }
        } catch (e: Exception) {
            val stack = e.stackTraceToString()
            errorText = "Error loading call ID: $callId\nUser: ${auth.currentUser?.uid}\n\n$stack"
            android.util.Log.e("FundingDetail", "Error loading call", e)
        } finally {
            isLoading = false
        }
    }

    SoftScaffold(
        topBar = {
            TopAppBar(
                title = { Text("Funding Opportunity", fontWeight = FontWeight.Bold, fontSize = 20.sp) }, // Generic title as per InvestorDetail
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {}, // Edit button removed as requested
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { innerPadding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else if (errorText != null) {
             Box(modifier = Modifier.fillMaxSize().padding(innerPadding).padding(16.dp)) {
                androidx.compose.foundation.text.selection.SelectionContainer {
                    Text(
                        text = "DEBUG ERROR:\n$errorText",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        } else if (call == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Funding Call not found.", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            val c = call!!
            
            Box(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // --- Hero Section (Investor Image) ---
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(400.dp)
                    ) {
                        if (investorProfile?.profilePhotoUrl?.isNotEmpty() == true) {
                            AsyncImage(
                                model = investorProfile!!.profilePhotoUrl,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    c.investorName.take(1).uppercase(),
                                    style = MaterialTheme.typography.displayLarge,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        
                        // Gradient Overlay
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    androidx.compose.ui.graphics.Brush.verticalGradient(
                                        colorStops = arrayOf(
                                            0.0f to Color.Transparent,
                                            0.6f to Color.Transparent,
                                            1.0f to MaterialTheme.colorScheme.background
                                        )
                                    )
                                )
                        )
                    }

                    // --- Header Info ---
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                           // Investor Avatar Small
                            if (investorProfile?.profilePhotoUrl?.isNotEmpty() == true) {
                                AsyncImage(
                                    model = investorProfile!!.profilePhotoUrl,
                                    contentDescription = null,
                                    modifier = Modifier.size(32.dp).clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            // Investor Name (Big)
                            Text(
                                c.investorName, 
                                style = MaterialTheme.typography.displaySmall.copy(fontSize = 26.sp), // Big
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        // Funding Call Title (Small)
                        Text(
                            c.title, 
                            style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp), // Small
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary, // Use Primary for the sub-title/role
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(32.dp))

                    // --- Quick Stats ---
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        val min = c.minTicketAmount.toLongOrNull() ?: 0L
                        val ticketStr = if (min > 0) "₹${formatAmountSimple(min)}" else "N/A"
                        
                        QuickStatItem(
                             icon = TablerIcons.CurrencyRupee, 
                             label = "Start Ticket", 
                             value = ticketStr
                        )
                        
                        QuickStatItem(
                             icon = TablerIcons.ChartPie, 
                             label = "Start Equity", 
                             value = "${c.minEquity}%"
                        )
                        
                        val deadlineText = if (c.applicationDeadline != null) {
                             val sdf = java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault())
                             sdf.format(c.applicationDeadline.toDate())
                        } else {
                             "No Deadline"
                        }
                        
                        QuickStatItem(
                             icon = TablerIcons.Clock, 
                             label = "Deadline", 
                             value = deadlineText
                        )
                    }

                    Spacer(modifier = Modifier.height(32.dp))
                    
                    // --- Details Card ---
                    Card(
                        modifier = Modifier
                           .fillMaxWidth()
                           .padding(horizontal = 16.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(modifier = Modifier.padding(24.dp)) {
                            
                            // Bio
                            SectionHeader("About the Opportunity")
                             Text(
                                c.description,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 24.sp
                            )
                            
                            Spacer(modifier = Modifier.height(24.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha=0.3f))
                            Spacer(modifier = Modifier.height(24.dp))
                            
                            // Detailed Info
                            Text("Funding Details", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            DetailRow("Firm Name", c.investorName, TablerIcons.Building)
                            
                            if (c.sectors.isNotEmpty()) {
                                DetailRow("Focus Sectors", c.sectors.joinToString(", "), TablerIcons.ChartPie)
                            }
                            
                            val ticketRange = try {
                                val minT = c.minTicketAmount.toLongOrNull() ?: 0L
                                val maxT = c.maxTicketAmount.toLongOrNull() ?: 0L
                                "₹${formatAmountSimple(minT)} - ₹${formatAmountSimple(maxT)}"
                            } catch (e: Exception) {
                                "N/A"
                            }
                            DetailRow("Ticket Size", ticketRange, TablerIcons.Coin)
                            
                            if (c.stages.isNotEmpty()) {
                                DetailRow("Stage", c.stages.joinToString(", "), TablerIcons.DeviceAnalytics)
                            }
                             
                            if (!c.locationPreference.isNullOrEmpty()) {
                                DetailRow("Location", c.locationPreference, TablerIcons.MapPin)
                            }

                            // Attachments
                            if (c.attachments.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(16.dp))
                                Text("Attachments", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.height(8.dp))
                                c.attachments.forEach { attachment ->
                                    val name = attachment["name"] ?: "Document"
                                    val url = attachment["url"] ?: ""
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp)
                                            .background(MaterialTheme.colorScheme.surfaceContainerHighest, RoundedCornerShape(8.dp))
                                            .padding(12.dp)
                                            .clickable { 
                                                if (url.isNotEmpty()) {
                                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                                    context.startActivity(intent)
                                                }
                                            },
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(TablerIcons.FileText, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(name, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                    }
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(100.dp)) // Padding for FAB
                }
                
                // --- Floating CTA ---
                // Always show CTA container, but vary content based on role
                val showCta = !isLoading && call != null
                
                if (showCta) {
                    val c = call!!
                    val isCreator = currentUser?.uid == c.investorId
                    
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        if (isCreator) {
                            // --- Investor Management View ---
                            var showDatePicker by remember { mutableStateOf(false) }
                            var showCloseConfirm by remember { mutableStateOf(false) }
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                // Close/Disable Button
                                OutlinedButton(
                                    onClick = { showCloseConfirm = true },
                                    enabled = c.isActive,
                                    modifier = Modifier.weight(1f).height(50.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, if(c.isActive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outlineVariant),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = MaterialTheme.colorScheme.surface,
                                        contentColor = MaterialTheme.colorScheme.error,
                                        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                        disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                ) {
                                    Text(if (c.isActive) "Close Call" else "Closed")
                                }
                                
                                // Extend Deadline Button
                                Button(
                                    onClick = { showDatePicker = true },
                                    enabled = c.isActive,
                                    modifier = Modifier.weight(1f).height(50.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                ) {
                                    Text("Extend")
                                }
                            }
                            
                            // Confirm Close Dialog
                            if (showCloseConfirm) {
                                AlertDialog(
                                    onDismissRequest = { showCloseConfirm = false },
                                    title = { Text("Close Funding Call?") },
                                    text = { Text("This will mark the call as inactive. Startups will be notified.") },
                                    confirmButton = {
                                        TextButton(onClick = {
                                            showCloseConfirm = false
                                            scope.launch {
                                                repository.updateFundingCall(c.id, mapOf("isActive" to false))
                                                Toast.makeText(context, "Call Closed. Notification Sent.", Toast.LENGTH_LONG).show()
                                                // Refresh local state
                                                call = call?.copy(isActive = false)
                                            }
                                        }) { Text("Confirm", color = MaterialTheme.colorScheme.error) }
                                    },
                                    dismissButton = {
                                        TextButton(onClick = { showCloseConfirm = false }) { Text("Cancel") }
                                    }
                                )
                            }
                            
                            // Date Picker Logic (Simplified for brevity, using standard DatePickerDialog for reliability without experimental APIs if possible, but composing DatePicker is cleaner)
                            // We will use a standard Android DatePickerDialog triggered from context
                            if (showDatePicker) {
                                val currentCal = java.util.Calendar.getInstance()
                                if (c.applicationDeadline != null) {
                                    currentCal.time = c.applicationDeadline!!.toDate()
                                }
                                
                                val datePickerDialog = android.app.DatePickerDialog(
                                    context,
                                    { _, year, month, dayOfMonth ->
                                        val newCal = java.util.Calendar.getInstance()
                                        newCal.set(year, month, dayOfMonth, 23, 59, 59)
                                        val newTimestamp = Timestamp(newCal.time)
                                        
                                        scope.launch {
                                            repository.updateFundingCall(c.id, mapOf("applicationDeadline" to newTimestamp))
                                            Toast.makeText(context, "Deadline Extended! Notification Sent.", Toast.LENGTH_LONG).show()
                                            call = call?.copy(applicationDeadline = newTimestamp)
                                        }
                                        showDatePicker = false
                                    },
                                    currentCal.get(java.util.Calendar.YEAR),
                                    currentCal.get(java.util.Calendar.MONTH),
                                    currentCal.get(java.util.Calendar.DAY_OF_MONTH)
                                )
                                // Ensure min date is today
                                datePickerDialog.datePicker.minDate = System.currentTimeMillis()
                                datePickerDialog.show()
                                
                                // Reset state immediately since dialog handles itself
                                showDatePicker = false 
                            }
                            
                        } else if (currentUser?.role != "startup") {
                            // Non-Startup View (View Only)
                            Button(
                                onClick = {},
                                enabled = false,
                                colors = ButtonDefaults.buttonColors(
                                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                modifier = Modifier.fillMaxWidth().height(50.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("View Only (${currentUser?.role?.replaceFirstChar { it.uppercase() } ?: "Guest"})")
                            }
                        } else {
                            // Startup Logic
                            val userSectorString = currentStartup?.sector?.takeIf { it.isNotBlank() } ?: currentUser?.startupCategory ?: ""
                            val userSectors = userSectorString.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                            
                            val isEligible = c.sectors.isEmpty() || c.sectors.any { callSec -> 
                                userSectors.any { userSec -> callSec.equals(userSec, ignoreCase = true) }
                            }
                            
                            // Calculate status
                            val isExpired = c.applicationDeadline?.toDate()?.before(java.util.Date()) == true
                            val isClosed = !c.isActive || isExpired

                            if (isClosed) {
                                // Closed View
                                Surface(
                                    modifier = Modifier.fillMaxWidth().height(50.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant, // Gray
                                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxSize(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(TablerIcons.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(if (isExpired) "Deadline Passed" else "Applications Closed", color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    }
                                }
                            } else if (hasApplied) {
                                Surface(
                                    modifier = Modifier.fillMaxWidth().height(50.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.secondaryContainer, // Light Green Background
                                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.secondary)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxSize(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(TablerIcons.CircleCheck, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(24.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Application Submitted", color = MaterialTheme.colorScheme.onSecondaryContainer, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    }
                                }
                            } else if (isEligible) {
                                var isSubmitting by remember { mutableStateOf(false) }
                                Button(
                                    onClick = { 
                                        scope.launch {
                                            isSubmitting = true
                                            try {
                                                val application = FundingApplication(
                                                    id = UUID.randomUUID().toString(),
                                                    callId = c.id,
                                                    investorId = c.investorId, // Should be populated now
                                                    startupId = currentUser!!.uid,
                                                    startupName = currentStartup?.startupName ?: currentUser?.name ?: "Unknown Startup",
                                                    status = "applied",
                                                    appliedAt = Timestamp.now(),
                                                    // Ensure other fields are populated if needed
                                                    fundingAsk = c.minTicketAmount, // Placeholder, normally user input
                                                    startupSector = currentStartup?.sector ?: "",
                                                    startupStage = currentStartup?.stage ?: ""
                                                )
                                                repository.applyToFundingCall(application)
                                                hasApplied = true
                                                Toast.makeText(context, "Application Sent Successfully!", Toast.LENGTH_LONG).show()
                                            } catch (e: Exception) {
                                                android.util.Log.e("FundingDetail", "Error applying", e)
                                                Toast.makeText(context, "Failed to apply: ${e.message}", Toast.LENGTH_LONG).show()
                                            } finally {
                                                isSubmitting = false
                                            }
                                        }
                                    },
                                    enabled = !isSubmitting,
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                    modifier = Modifier.fillMaxWidth().height(50.dp),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    if(isSubmitting) {
                                        CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(24.dp))
                                    } else {
                                        Text("Apply Now", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            } else {
                                // Ineligible View
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(MaterialTheme.colorScheme.errorContainer, RoundedCornerShape(8.dp))
                                        .padding(12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(TablerIcons.InfoCircle, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Eligibility Check", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        "This funding call is for ${c.sectors.joinToString(", ")} startups.\nYour profile is listed as '$userSectorString'.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }

            }
        }
    }
}

fun formatAmountSimple(amount: Long): String {
    return when {
        amount >= 10000000 -> "${String.format("%.1f", amount / 10000000.0)}Cr"
        amount >= 100000 -> "${String.format("%.0f", amount / 100000.0)}L"
        else -> java.text.NumberFormat.getInstance().format(amount)
    }
}
