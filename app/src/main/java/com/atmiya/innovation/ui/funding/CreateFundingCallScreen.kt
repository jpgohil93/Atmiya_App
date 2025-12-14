package com.atmiya.innovation.ui.funding

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.*
import androidx.compose.foundation.clickable
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.atmiya.innovation.data.FundingCall
import com.atmiya.innovation.data.WallPost
import com.atmiya.innovation.repository.FirestoreRepository
import com.atmiya.innovation.repository.StorageRepository
import com.atmiya.innovation.ui.components.*
import com.atmiya.innovation.ui.theme.AtmiyaPrimary
import com.atmiya.innovation.ui.theme.AtmiyaSecondary
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateFundingCallScreen(
    callId: String? = null,
    onBack: () -> Unit,
    onCallCreated: () -> Unit
) {
    val repository = remember { FirestoreRepository() }
    val storageRepository = remember { StorageRepository() }
    val scope = rememberCoroutineScope()
    val auth = FirebaseAuth.getInstance()
    val context = LocalContext.current

    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var minTicket by remember { mutableStateOf("") }
    var maxTicket by remember { mutableStateOf("") }
    var minEquity by remember { mutableStateOf("") }
    var maxEquity by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var deadline by remember { mutableStateOf("") } 

    // Multi-select states
    val allSectors = listOf("Tech", "Fintech", "Healthcare", "EdTech", "AgriTech", "CleanTech", "Consumer", "B2B")
    val selectedSectors = remember { mutableStateListOf<String>() }
    
    val allStages = listOf("Pre-Seed", "Seed", "Pre-Series A", "Series A", "Series B+")
    val selectedStages = remember { mutableStateListOf<String>() }

    // Attachments
    val attachments = remember { mutableStateListOf<Pair<Uri, String>>() } // Uri, Name

    var isUploading by remember { mutableStateOf(false) }
    var isLoadingInitial by remember { mutableStateOf(callId != null) }

    // Load Data if Edit Mode
    LaunchedEffect(callId) {
        if (callId != null) {
            try {
                val snapshot = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                    .collection("fundingCalls").document(callId).get().await()
                val call = snapshot.toObject(FundingCall::class.java)
            
                call?.let { c ->
                    title = c.title
                    description = c.description
                    minTicket = c.minTicketAmount
                    maxTicket = c.maxTicketAmount
                    minEquity = c.minEquity ?: ""
                    maxEquity = c.maxEquity ?: ""
                    location = c.locationPreference ?: ""
                    val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                    deadline = c.applicationDeadline?.toDate()?.let { dateFormat.format(it) } ?: ""
                    
                    selectedSectors.clear()
                    selectedSectors.addAll(c.sectors)
                    selectedStages.clear()
                    selectedStages.addAll(c.stages)
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error loading call", Toast.LENGTH_SHORT).show()
            } finally {
                isLoadingInitial = false
            }
        }
    }

    val docPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            attachments.add(uri to (uri.lastPathSegment ?: "Document"))
        }
    }

    SoftScaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (callId == null) "Create Funding Call" else "Update Funding Call", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = AtmiyaPrimary
                )
            )
        }
    ) { innerPadding ->
        if (isLoadingInitial) {
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = AtmiyaPrimary)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Basic Info Card
                SoftCard(modifier = Modifier.fillMaxWidth()) {
                    SectionHeader("Basic Information")
                    Spacer(modifier = Modifier.height(16.dp))
                    SoftTextField(value = title, onValueChange = { title = it }, label = "Title (e.g., Seed Round for Fintech)")
                    Spacer(modifier = Modifier.height(12.dp))
                    SoftTextField(value = description, onValueChange = { description = it }, label = "Description", minLines = 4)
                }

                // Financials Card
                SoftCard(modifier = Modifier.fillMaxWidth()) {
                    SectionHeader("Financials & Equity")
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SoftTextField(
                            value = minTicket, 
                            onValueChange = { if (it.all { char -> char.isDigit() }) minTicket = it }, 
                            label = "Min Ticket (₹)", 
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        SoftTextField(
                            value = maxTicket, 
                            onValueChange = { if (it.all { char -> char.isDigit() }) maxTicket = it }, 
                            label = "Max Ticket (₹)", 
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SoftTextField(
                            value = minEquity, 
                            onValueChange = { if (it.all { char -> char.isDigit() || char == '.' }) minEquity = it }, 
                            label = "Min Equity (%)", 
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        SoftTextField(
                            value = maxEquity, 
                            onValueChange = { if (it.all { char -> char.isDigit() || char == '.' }) maxEquity = it }, 
                            label = "Max Equity (%)", 
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Target Profile Card
                SoftCard(modifier = Modifier.fillMaxWidth()) {
                    SectionHeader("Target Profile")
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text("Sectors", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(allSectors) { sector ->
                            FilterChip(
                                selected = selectedSectors.contains(sector),
                                onClick = { 
                                    if (selectedSectors.contains(sector)) selectedSectors.remove(sector) 
                                    else selectedSectors.add(sector) 
                                },
                                label = { Text(sector) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = AtmiyaPrimary.copy(alpha = 0.1f),
                                    selectedLabelColor = AtmiyaPrimary
                                )
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Stages", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(allStages) { stage ->
                            FilterChip(
                                selected = selectedStages.contains(stage),
                                onClick = { 
                                    if (selectedStages.contains(stage)) selectedStages.remove(stage) 
                                    else selectedStages.add(stage) 
                                },
                                label = { Text(stage) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = AtmiyaSecondary.copy(alpha = 0.1f),
                                    selectedLabelColor = AtmiyaSecondary
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    SoftTextField(value = location, onValueChange = { location = it }, label = "Location Preference (Optional)")
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Date Picker
                    var showDatePicker by remember { mutableStateOf(false) }
                    val datePickerState = rememberDatePickerState()
                    
                    Box {
                        SoftTextField(
                             value = deadline, 
                             onValueChange = { }, // Read Only
                             label = "Application Deadline (YYYY-MM-DD)",
                             modifier = Modifier.clickable { showDatePicker = true }, // Making the whole field clickable might be tricky with SoftTextField internals, wrapping in Box with matchParentSize clickable is safer or just passing Enabled=false and handling click on Box.
                             // Actually SoftTextField might consume touch.
                             // Let's use OutlinedTextField directly for more control or an overlay.
                             enabled = false // Disable direct editing
                        )
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .clickable { showDatePicker = true }
                        )
                    }

                    if (showDatePicker) {
                        DatePickerDialog(
                            onDismissRequest = { showDatePicker = false },
                            confirmButton = {
                                TextButton(onClick = {
                                    datePickerState.selectedDateMillis?.let { millis ->
                                        val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                                        deadline = dateFormat.format(java.util.Date(millis))
                                    }
                                    showDatePicker = false
                                }) {
                                    Text("OK", color = AtmiyaPrimary)
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showDatePicker = false }) {
                                    Text("Cancel", color = AtmiyaPrimary)
                                }
                            }
                        ) {
                            DatePicker(state = datePickerState)
                        }
                    }
                }

                // Attachments Card
                SoftCard(modifier = Modifier.fillMaxWidth()) {
                    SectionHeader("Attachments (PDF/PPT)")
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedButton(
                        onClick = { docPicker.launch(arrayOf("application/pdf", "application/vnd.ms-powerpoint", "application/vnd.openxmlformats-officedocument.presentationml.presentation")) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Outlined.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Add Document")
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    attachments.forEachIndexed { index, (uri, name) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.Gray.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(name, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                            IconButton(onClick = { attachments.removeAt(index) }) {
                                Icon(Icons.Outlined.Close, contentDescription = "Remove", tint = Color.Gray)
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Submit Button
                SoftButton(
                    onClick = {
                        if (validateInputs(title, description, minTicket, maxTicket, selectedSectors, selectedStages)) {
                            isUploading = true
                            scope.launch {
                                try {
                                    val user = auth.currentUser ?: return@launch
                                    val userProfile = repository.getUser(user.uid)
                                    val newCallId = callId ?: UUID.randomUUID().toString()
                                    
                                    // Upload NEW Attachments
                                    val uploadedAttachments = attachments.map { (uri, name) ->
                                        val isPdf = context.contentResolver.getType(uri)?.contains("pdf") == true
                                        val url = storageRepository.uploadFundingAttachment(context, newCallId, uri, isPdf)
                                        mapOf("name" to name, "url" to url, "type" to if (isPdf) "pdf" else "ppt")
                                    }
                                    
                                    val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                                    val parsedDate = try {
                                        dateFormat.parse(deadline)
                                    } catch (e: Exception) {
                                        null
                                    }
                                    val deadlineTimestamp = if (parsedDate != null) Timestamp(parsedDate) else null
                                    
                                    val currentTimestamp = Timestamp.now()

                                    val finalCall = FundingCall(
                                        id = newCallId,
                                        investorId = user.uid,
                                        investorName = userProfile?.name ?: "Unknown Firm",
                                        title = title,
                                        description = description,
                                        sectors = selectedSectors.toList(),
                                        stages = selectedStages.toList(),
                                        minTicketAmount = minTicket,
                                        maxTicketAmount = maxTicket,
                                        minEquity = minEquity,
                                        maxEquity = maxEquity,
                                        locationPreference = location,
                                        applicationDeadline = deadlineTimestamp,
                                        attachments = uploadedAttachments,
                                        isActive = true,
                                        createdAt = if (callId != null) Timestamp.now() else currentTimestamp
                                    )
                                    
                                    repository.createFundingCall(finalCall)

                                    if (callId == null) {
                                        val wallPost = WallPost(
                                            id = UUID.randomUUID().toString(),
                                            authorUserId = user.uid,
                                            authorName = userProfile?.name ?: "Unknown Firm",
                                            authorRole = "investor",
                                            authorPhotoUrl = userProfile?.profilePhotoUrl,
                                            content = "New Funding Opportunity: $title\n\n$description",
                                            postType = "funding_call",
                                            fundingCallId = newCallId,
                                            sector = selectedSectors.firstOrNull() ?: "General",
                                            isActive = true, 
                                            createdAt = Timestamp.now(),
                                            likesCount = 0,
                                            commentsCount = 0
                                        )
                                        repository.addWallPost(wallPost)
                                    }

                                    Toast.makeText(context, if (callId == null) "Funding Call Created!" else "Funding Call Updated!", Toast.LENGTH_LONG).show()
                                    onCallCreated()
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Failed to save funding call. Please try again.", Toast.LENGTH_LONG).show()
                                } finally {
                                    isUploading = false
                                }
                            }
                        } else {
                            Toast.makeText(context, "Please fill all required fields correctly.", Toast.LENGTH_SHORT).show()
                        }
                    },
                    text = if (isUploading) (if (callId == null) "Creating..." else "Updating...") else (if (callId == null) "Publish Funding Call" else "Update Funding Call"),
                    icon = if (callId == null) Icons.AutoMirrored.Outlined.Send else Icons.Filled.Add,
                    isLoading = isUploading
                )
                
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = AtmiyaPrimary
    )
}

fun validateInputs(
    title: String, 
    description: String, 
    minTicket: String, 
    maxTicket: String, 
    sectors: List<String>, 
    stages: List<String>
): Boolean {
    return title.isNotBlank() && 
           description.isNotBlank() && 
           minTicket.isNotBlank() && 
           maxTicket.isNotBlank() && 
           sectors.isNotEmpty() && 
           stages.isNotEmpty()
}
