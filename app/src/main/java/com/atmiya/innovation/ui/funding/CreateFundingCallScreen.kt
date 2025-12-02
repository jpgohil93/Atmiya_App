package com.atmiya.innovation.ui.funding

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Publish
import androidx.compose.material3.*
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
import com.atmiya.innovation.ui.components.SoftButton
import com.atmiya.innovation.ui.components.SoftScaffold
import com.atmiya.innovation.ui.components.SoftTextField
import com.atmiya.innovation.ui.theme.AtmiyaPrimary
import com.atmiya.innovation.ui.theme.AtmiyaSecondary
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateFundingCallScreen(
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
    var deadline by remember { mutableStateOf("") } // Simple string for now, could be DatePicker

    // Multi-select states
    val allSectors = listOf("Tech", "Fintech", "Healthcare", "EdTech", "AgriTech", "CleanTech", "Consumer", "B2B")
    val selectedSectors = remember { mutableStateListOf<String>() }
    
    val allStages = listOf("Pre-Seed", "Seed", "Pre-Series A", "Series A", "Series B+")
    val selectedStages = remember { mutableStateListOf<String>() }

    // Attachments
    val attachments = remember { mutableStateListOf<Pair<Uri, String>>() } // Uri, Name
    var isUploading by remember { mutableStateOf(false) }

    val docPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            // Basic check, real validation happens on upload
            attachments.add(uri to (uri.lastPathSegment ?: "Document"))
        }
    }

    SoftScaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create Funding Call", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = AtmiyaPrimary
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Basic Info
            SectionHeader("Basic Information")
            SoftTextField(value = title, onValueChange = { title = it }, label = "Title (e.g., Seed Round for Fintech)")
            SoftTextField(value = description, onValueChange = { description = it }, label = "Description", minLines = 4)

            // Financials
            SectionHeader("Financials & Equity")
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

            // Target Profile
            SectionHeader("Target Profile")
            Text("Sectors", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(allSectors) { sector ->
                    FilterChip(
                        selected = selectedSectors.contains(sector),
                        onClick = { 
                            if (selectedSectors.contains(sector)) selectedSectors.remove(sector) 
                            else selectedSectors.add(sector) 
                        },
                        label = { Text(sector) }
                    )
                }
            }
            
            Text("Stages", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(allStages) { stage ->
                    FilterChip(
                        selected = selectedStages.contains(stage),
                        onClick = { 
                            if (selectedStages.contains(stage)) selectedStages.remove(stage) 
                            else selectedStages.add(stage) 
                        },
                        label = { Text(stage) }
                    )
                }
            }

            SoftTextField(value = location, onValueChange = { location = it }, label = "Location Preference (Optional)")
            SoftTextField(value = deadline, onValueChange = { deadline = it }, label = "Application Deadline (YYYY-MM-DD)")

            // Attachments
            SectionHeader("Attachments (PDF/PPT)")
            OutlinedButton(
                onClick = { docPicker.launch(arrayOf("application/pdf", "application/vnd.ms-powerpoint", "application/vnd.openxmlformats-officedocument.presentationml.presentation")) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.AttachFile, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add Document")
            }
            
            attachments.forEachIndexed { index, (uri, name) ->
                Row(
                    modifier = Modifier.fillMaxWidth().background(Color.Gray.copy(alpha = 0.1f), RoundedCornerShape(8.dp)).padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(name, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    IconButton(onClick = { attachments.removeAt(index) }) {
                        Icon(Icons.Default.Close, contentDescription = "Remove")
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Submit
            SoftButton(
                onClick = {
                    if (validateInputs(title, description, minTicket, maxTicket, selectedSectors, selectedStages)) {
                        isUploading = true
                        scope.launch {
                            try {
                                val user = auth.currentUser ?: return@launch
                                val userProfile = repository.getUser(user.uid)
                                val callId = UUID.randomUUID().toString()
                                
                                // Upload Attachments
                                val uploadedAttachments = attachments.map { (uri, name) ->
                                    val isPdf = context.contentResolver.getType(uri)?.contains("pdf") == true
                                    val url = storageRepository.uploadFundingAttachment(context, callId, uri, isPdf)
                                    mapOf("name" to name, "url" to url, "type" to if (isPdf) "pdf" else "ppt")
                                }

                                val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                                val parsedDate = try {
                                    dateFormat.parse(deadline)
                                } catch (e: Exception) {
                                    null
                                }
                                val deadlineTimestamp = if (parsedDate != null) Timestamp(parsedDate) else null

                                val newCall = FundingCall(
                                    id = callId,
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
                                    createdAt = Timestamp.now()
                                )
                                
                                repository.createFundingCall(newCall)

                                // Auto-Post to Wall
                                val wallPost = WallPost(
                                    id = UUID.randomUUID().toString(),
                                    authorUserId = user.uid,
                                    authorName = userProfile?.name ?: "Unknown Firm",
                                    authorRole = "investor",
                                    authorPhotoUrl = userProfile?.profilePhotoUrl,
                                    content = "New Funding Opportunity: $title\n\n$description",
                                    postType = "funding_call",
                                    fundingCallId = callId,
                                    sector = selectedSectors.firstOrNull() ?: "General",
                                    isActive = true, // Explicitly set
                                    createdAt = Timestamp.now(),
                                    likesCount = 0,
                                    commentsCount = 0
                                )
                                repository.addWallPost(wallPost)

                                Toast.makeText(context, "Funding Call Created & Posted to Wall!", Toast.LENGTH_LONG).show()
                                onCallCreated()
                            } catch (e: Exception) {
                                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                            } finally {
                                isUploading = false
                            }
                        }
                    } else {
                        Toast.makeText(context, "Please fill all required fields correctly.", Toast.LENGTH_SHORT).show()
                    }
                },
                text = if (isUploading) "Creating..." else "Publish Funding Call",
                icon = Icons.Default.Publish,
                isLoading = isUploading,
                modifier = Modifier.fillMaxWidth().height(50.dp)
            )
        }
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = AtmiyaPrimary,
        modifier = Modifier.padding(top = 8.dp)
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
