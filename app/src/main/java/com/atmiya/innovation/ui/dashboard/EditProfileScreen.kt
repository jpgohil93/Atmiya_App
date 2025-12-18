package com.atmiya.innovation.ui.dashboard

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import compose.icons.TablerIcons
import compose.icons.tablericons.ArrowLeft
import compose.icons.tablericons.Video
import compose.icons.tablericons.X
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.atmiya.innovation.data.Startup
import com.atmiya.innovation.data.Investor
import com.atmiya.innovation.data.Mentor
import com.atmiya.innovation.repository.FirestoreRepository
import com.atmiya.innovation.repository.StorageRepository
import com.atmiya.innovation.ui.components.SoftScaffold
import com.atmiya.innovation.ui.components.SoftTextField
import com.atmiya.innovation.ui.components.SoftButton
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    onBack: () -> Unit
) {
    val auth = FirebaseAuth.getInstance()
    val userId = auth.currentUser?.uid ?: return
    val repository = remember { FirestoreRepository() }
    val storageRepository = remember { StorageRepository() } // Added Storage Repo
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var isLoading by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }
    
    var role by remember { mutableStateOf("") }
    
    // Startup Fields
    var startupName by remember { mutableStateOf("") }
    var startupSector by remember { mutableStateOf("") }
    var startupStage by remember { mutableStateOf("") }
    var startupFundingAsk by remember { mutableStateOf("") }
    var startupDemoVideoUrl by remember { mutableStateOf("") } // Added
    
    // Video Upload State
    var isVideoUploadMode by remember { mutableStateOf(false) }
    var videoUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var uploadedVideoFileName by remember { mutableStateOf<String?>(null) }
    var uploadProgress by remember { mutableStateOf(0f) }
    var isVideoUploading by remember { mutableStateOf(false) }
    
    val videoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        if (uri != null) {
            // Validate size immediately (100MB)
            val sizeDetails = context.contentResolver.openFileDescriptor(uri, "r")?.statSize ?: 0
            if (sizeDetails > 100 * 1024 * 1024) {
                 Toast.makeText(context, "Video too large (Max 100MB)", Toast.LENGTH_LONG).show()
            } else {
                 videoUri = uri
                 uploadedVideoFileName = "selected_video.mp4" // Placeholder until upload
            }
        }
    }
    
    // Investor Fields
    var investorFirm by remember { mutableStateOf("") }
    var investorTicketMin by remember { mutableStateOf("") }
    var investorSectors by remember { mutableStateOf("") } // Comma separated
    
    // Mentor Fields
    var mentorTitle by remember { mutableStateOf("") }
    var mentorOrg by remember { mutableStateOf("") }
    var mentorExpertise by remember { mutableStateOf("") } // Comma separated
    
    // Common
    var name by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var bio by remember { mutableStateOf("") } // Description

    LaunchedEffect(userId) {
        val user = repository.getUser(userId)
        if (user != null) {
            role = user.role
            name = user.name
            city = user.city
            
            when (role) {
                "startup" -> {
                    val s = repository.getStartup(userId)
                    if (s != null) {
                        startupName = s.startupName
                        startupSector = s.sector
                        startupStage = s.stage
                        startupFundingAsk = s.fundingAsk
                        bio = s.description
                        startupDemoVideoUrl = s.demoVideoUrl ?: ""
                        uploadedVideoFileName = s.uploadedVideoFileName
                        
                        // Set Mode based on existing data
                        if (!s.uploadedVideoFileName.isNullOrBlank()) {
                            isVideoUploadMode = true
                        } else {
                            isVideoUploadMode = false // Default or Link
                        }
                    }
                }
                "investor" -> {
                    val i = repository.getInvestor(userId)
                    if (i != null) {
                        investorFirm = i.firmName
                        investorTicketMin = i.ticketSizeMin
                        // investorTicketMax = i.ticketSizeMax
                        investorSectors = i.sectorsOfInterest.joinToString(", ")
                        bio = i.bio
                    }
                }
                "mentor" -> {
                    val m = repository.getMentor(userId)
                    if (m != null) {
                        // Assuming Mentor model has title/org/expertise
                        mentorTitle = m.title
                        mentorOrg = m.organization
                        mentorExpertise = m.expertiseAreas.joinToString(", ")
                        bio = m.bio
                    }
                }

            }
        }
        isLoading = false
    }

    SoftScaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Profile") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(TablerIcons.ArrowLeft, contentDescription = "Back", modifier = Modifier.size(28.dp))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { padding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    // Removed .imePadding() from here to avoid conflict with Scaffold
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                SoftTextField(value = name, onValueChange = { name = it }, label = "Full Name")
                SoftTextField(value = city, onValueChange = { city = it }, label = "City")
                SoftTextField(value = bio, onValueChange = { bio = it }, label = "About / Bio", minLines = 3)

                Divider()
                Text("Role Specific Details", style = MaterialTheme.typography.titleMedium)

                when (role) {
                    "startup" -> {
                        SoftTextField(value = startupName, onValueChange = { startupName = it }, label = "Startup Name")
                        SoftTextField(value = startupSector, onValueChange = { startupSector = it }, label = "Sector")
                        SoftTextField(value = startupStage, onValueChange = { startupStage = it }, label = "Stage")
                        SoftTextField(value = startupFundingAsk, onValueChange = { startupFundingAsk = it }, label = "Funding Ask")
                        
                        // --- Video Upload / Link UI ---
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Product Demo", style = MaterialTheme.typography.labelLarge)
                        
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = isVideoUploadMode, onClick = { isVideoUploadMode = true })
                            Text("Upload Video")
                            Spacer(modifier = Modifier.width(16.dp))
                            RadioButton(selected = !isVideoUploadMode, onClick = { isVideoUploadMode = false })
                            Text("External Link")
                        }

                        if (isVideoUploadMode) {
                            OutlinedCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { videoLauncher.launch("video/*") },
                                colors = CardDefaults.outlinedCardColors(containerColor = Color.Transparent),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color.Gray)
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(TablerIcons.Video, contentDescription = null, modifier = Modifier.size(32.dp), tint = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = if (videoUri != null) "Selected: New Video" else if (!uploadedVideoFileName.isNullOrBlank()) "Current: $uploadedVideoFileName" else "Tap to select video (Max 100MB)",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = if (videoUri != null) MaterialTheme.colorScheme.primary else Color.Gray
                                    )
                                    
                                    if (videoUri != null) {
                                         Spacer(modifier = Modifier.height(8.dp))
                                         IconButton(onClick = { videoUri = null }) {
                                             Icon(TablerIcons.X, contentDescription = "Clear Selection")
                                         }
                                    }
                                }
                                
                                if (isVideoUploading) {
                                    LinearProgressIndicator(
                                        progress = { uploadProgress },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        } else {
                            SoftTextField(value = startupDemoVideoUrl, onValueChange = { startupDemoVideoUrl = it }, label = "Product Demo Link (YouTube/Vimeo)")
                        }
                    }
                    "investor" -> {
                        SoftTextField(value = investorFirm, onValueChange = { investorFirm = it }, label = "Firm Name")
                        SoftTextField(value = investorTicketMin, onValueChange = { investorTicketMin = it }, label = "Min Ticket Size")
                        SoftTextField(value = investorSectors, onValueChange = { investorSectors = it }, label = "Sectors (Comma separated)")
                    }
                    "mentor" -> {
                        SoftTextField(value = mentorTitle, onValueChange = { mentorTitle = it }, label = "Title")
                        SoftTextField(value = mentorOrg, onValueChange = { mentorOrg = it }, label = "Organization")
                        SoftTextField(value = mentorExpertise, onValueChange = { mentorExpertise = it }, label = "Expertise Areas (Comma separated)")
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                SoftButton(
                    onClick = {
                        scope.launch {
                            isSaving = true
                            try {
                                var finalDemoUrl = startupDemoVideoUrl
                                var finalVideoName = uploadedVideoFileName
                                
                                // Upload Video if needed
                                if (role == "startup" && isVideoUploadMode && videoUri != null) {
                                    isVideoUploading = true // Show progress
                                    try {
                                        finalDemoUrl = storageRepository.uploadStartupDemoVideo(context, userId, videoUri!!) { prog ->
                                            uploadProgress = prog / 100f
                                        }
                                        finalVideoName = "demo_video.mp4" // Or Keep UUID? Repo handles it. But we need to save *something* to indicate file mode.
                                        // The repo actually generates a UUID filename. 
                                        // But we can't easily get it back unless we change repo to return Pair.
                                        // Wait, repo returns URL.
                                        // We should just set finalVideoName to "Uploaded Video" or similar if we don't have exact name,
                                        // OR, simply rely on isVideoUploadMode logic -> which checks !uploadedVideoFileName.isNullOrBlank()
                                        // So we MUST set it to something non-null.
                                        finalVideoName = "uploaded_demo.mp4" 
                                    } catch (e: Exception) {
                                        throw e
                                    } finally {
                                        isVideoUploading = false
                                    }
                                } else if (role == "startup" && !isVideoUploadMode) {
                                    // Switch to Link Mode
                                    finalVideoName = null // Clear video file name marker
                                    // finalDemoUrl is already set from TextField
                                }

                                // 1. Update Base User
                                repository.updateUser(userId, mapOf("name" to name, "city" to city))
                                
                                // 2. Update Role Doc
                                when(role) {
                                    "startup" -> {
                                        val data = mapOf(
                                            "startupName" to startupName,
                                            "sector" to startupSector,
                                            "stage" to startupStage,
                                            "fundingAsk" to startupFundingAsk,
                                            "description" to bio,
                                            "demoVideoUrl" to finalDemoUrl,
                                            "uploadedVideoFileName" to (finalVideoName ?: "") // Ensure non-null
                                        )
                                        repository.updateStartup(userId, data)
                                    }
                                    "investor" -> {
                                        val data = mapOf(
                                            "name" to name, // Sync name
                                            "firmName" to investorFirm,
                                            "ticketSizeMin" to investorTicketMin,
                                            // "ticketSizeMax" to investorTicketMax,
                                            "sectorsOfInterest" to investorSectors.split(",").map { it.trim() },
                                            "bio" to bio
                                        )
                                        repository.updateInvestor(userId, data)
                                    }
                                    "mentor" -> {
                                        val data = mapOf(
                                            "name" to name,
                                            "title" to mentorTitle,
                                            "organization" to mentorOrg,
                                            "expertiseAreas" to mentorExpertise.split(",").map { it.trim() },
                                            "bio" to bio
                                        )
                                        repository.updateMentor(userId, data)
                                    }
                                }
                                Toast.makeText(context, "Profile Updated", Toast.LENGTH_SHORT).show()
                                onBack()
                            } catch(e: Exception) {
                                Toast.makeText(context, "Failed to update profile: ${e.message}", Toast.LENGTH_LONG).show()
                                android.util.Log.e("EditProfile", "Update failed", e)
                            } finally {
                                isSaving = false
                            }
                        }
                    },
                    text = if(isSaving) "Saving..." else "Save Changes",
                    isLoading = isSaving,
                    modifier = Modifier.fillMaxWidth()
                )

                // Add Spacer for Keyboard (Last item in scrollable Column)
                Spacer(modifier = Modifier.height(WindowInsets.ime.asPaddingValues().calculateBottomPadding()))
            }
        }
    }
}
