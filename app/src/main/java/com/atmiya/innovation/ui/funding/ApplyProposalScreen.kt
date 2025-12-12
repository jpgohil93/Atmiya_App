package com.atmiya.innovation.ui.funding

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import compose.icons.TablerIcons
import compose.icons.tablericons.ArrowLeft
import compose.icons.tablericons.Send
import compose.icons.tablericons.Upload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.atmiya.innovation.data.FundingApplication
import com.atmiya.innovation.repository.FirestoreRepository
import com.atmiya.innovation.repository.StorageRepository
import com.atmiya.innovation.ui.components.*
import com.atmiya.innovation.ui.theme.AtmiyaPrimary
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApplyProposalScreen(
    callId: String,
    onBack: () -> Unit,
    onSuccess: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val firestoreRepository = remember { FirestoreRepository() }
    val storageRepository = remember { StorageRepository() }
    val auth = FirebaseAuth.getInstance()
    val user = auth.currentUser

    var isLoading by remember { mutableStateOf(true) }
    var isSubmitting by remember { mutableStateOf(false) }

    // Form Fields
    var startupName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var sector by remember { mutableStateOf<String?>(null) }
    var stage by remember { mutableStateOf<String?>(null) }
    var city by remember { mutableStateOf("") }
    var state by remember { mutableStateOf("") }
    var fundingAsk by remember { mutableStateOf("") }
    var additionalNote by remember { mutableStateOf("") }
    var pitchDeckUrl by remember { mutableStateOf<String?>(null) }

    // Prefill Data
    LaunchedEffect(user) {
        if (user != null) {
            val userProfile = firestoreRepository.getUser(user.uid)
            val startupProfile = firestoreRepository.getStartup(user.uid)
            
            if (userProfile != null) {
                email = userProfile.email
                phone = userProfile.phoneNumber
                city = userProfile.city
                state = userProfile.region
            }
            
            if (startupProfile != null) {
                startupName = startupProfile.startupName
                sector = startupProfile.sector.ifBlank { null }
                stage = startupProfile.stage.ifBlank { null }
                fundingAsk = startupProfile.fundingAsk
                pitchDeckUrl = startupProfile.pitchDeckUrl
            }
            isLoading = false
        }
    }

    val deckLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null && user != null) {
            scope.launch {
                try {
                    Toast.makeText(context, "Uploading Deck...", Toast.LENGTH_SHORT).show()
                    val url = storageRepository.uploadPitchDeck(context, user.uid, uri, isPdf = true)
                    pitchDeckUrl = url
                    Toast.makeText(context, "Deck Uploaded", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(context, "Upload Failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = { Text("Submit Proposal") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(TablerIcons.ArrowLeft, contentDescription = "Back", modifier = Modifier.size(28.dp))
                    }
                },
                colors = TopAppBarDefaults.smallTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = AtmiyaPrimary
                )
            )
        }
    ) { padding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = AtmiyaPrimary)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                Text("Startup Details", style = MaterialTheme.typography.titleMedium, color = AtmiyaPrimary)
                Spacer(modifier = Modifier.height(16.dp))
                
                ValidatedTextField(startupName, { startupName = it }, "Startup Name")
                Spacer(modifier = Modifier.height(16.dp))
                ValidatedTextField(email, { email = it }, "Email", keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email))
                Spacer(modifier = Modifier.height(16.dp))
                ValidatedTextField(phone, { phone = it }, "Phone", keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone))
                Spacer(modifier = Modifier.height(16.dp))
                DropdownField("Sector", listOf("AgriTech", "HealthTech", "FinTech", "EdTech", "CleanTech", "Other"), sector, { sector = it })
                Spacer(modifier = Modifier.height(16.dp))
                DropdownField("Stage", listOf("Ideation", "Prototype", "MVP", "Early Traction", "Scaling"), stage, { stage = it })
                
                Spacer(modifier = Modifier.height(24.dp))
                Text("Proposal Details", style = MaterialTheme.typography.titleMedium, color = AtmiyaPrimary)
                Spacer(modifier = Modifier.height(16.dp))
                
                ValidatedTextField(fundingAsk, { fundingAsk = it }, "Funding Ask (₹)", keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                Spacer(modifier = Modifier.height(16.dp))
                ValidatedTextField(additionalNote, { additionalNote = it }, "Additional Note / Cover Letter", minLines = 4)
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Text("Pitch Deck", style = MaterialTheme.typography.titleMedium, color = AtmiyaPrimary)
                Spacer(modifier = Modifier.height(8.dp))
                SoftButton(
                    onClick = { deckLauncher.launch(arrayOf("application/pdf")) },
                    text = if (pitchDeckUrl != null) "Update Pitch Deck (PDF)" else "Upload Pitch Deck (PDF)",
                    icon = TablerIcons.Upload,
                    containerColor = if (pitchDeckUrl != null) androidx.compose.ui.graphics.Color.Green.copy(alpha = 0.2f) else com.atmiya.innovation.ui.theme.AtmiyaSecondary
                )
                if (pitchDeckUrl != null) {
                    Text("Deck attached ✅", style = MaterialTheme.typography.bodySmall, color = androidx.compose.ui.graphics.Color.Green, modifier = Modifier.padding(top = 4.dp))
                } else {
                    Text("Required", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 4.dp))
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                
                SoftButton(
                    onClick = {
                        if (startupName.isBlank() || email.isBlank() || phone.isBlank() || fundingAsk.isBlank() || pitchDeckUrl == null) {
                            Toast.makeText(context, "Please fill all required fields and upload deck", Toast.LENGTH_LONG).show()
                        } else {
                            isSubmitting = true
                            scope.launch {
                                try {
                                    val appId = UUID.randomUUID().toString()
                                    val application = FundingApplication(
                                        id = appId,
                                        callId = callId,
                                        startupId = user?.uid ?: "",
                                        startupName = startupName,
                                        startupEmail = email,
                                        startupPhone = phone,
                                        startupSector = sector ?: "",
                                        startupStage = stage ?: "",
                                        city = city,
                                        state = state,
                                        fundingAsk = fundingAsk,
                                        pitchDeckUrl = pitchDeckUrl,
                                        additionalNote = additionalNote,
                                        status = "applied",
                                        appliedAt = Timestamp.now()
                                    )
                                    
                                    firestoreRepository.applyToFundingCall(application)
                                    Toast.makeText(context, "Proposal Submitted Successfully!", Toast.LENGTH_LONG).show()
                                    onSuccess()
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                                } finally {
                                    isSubmitting = false
                                }
                            }
                        }
                    },
                    text = "Submit Proposal",
                    icon = TablerIcons.Send,
                    isLoading = isSubmitting,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
