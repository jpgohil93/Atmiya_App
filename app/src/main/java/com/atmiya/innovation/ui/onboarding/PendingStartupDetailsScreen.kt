package com.atmiya.innovation.ui.onboarding

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.atmiya.innovation.data.AppConstants
import com.atmiya.innovation.repository.FirestoreRepository
import com.atmiya.innovation.repository.StorageRepository
import com.atmiya.innovation.ui.components.*
import com.atmiya.innovation.ui.theme.AtmiyaPrimary
import com.google.firebase.auth.FirebaseAuth
import compose.icons.TablerIcons
import compose.icons.tablericons.Upload
import compose.icons.tablericons.Id
import kotlinx.coroutines.launch

import androidx.compose.material3.ExperimentalMaterial3Api

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PendingStartupDetailsScreen(
    onComplete: () -> Unit,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val auth = FirebaseAuth.getInstance()
    val firestoreRepository = remember { FirestoreRepository() }
    val storageRepository = remember { StorageRepository() }

    var isLoading by remember { mutableStateOf(false) }
    var startupName by remember { mutableStateOf("") }
    var founderNames by remember { mutableStateOf("") }
    var startupSector by remember { mutableStateOf<String?>(null) }
    var startupStage by remember { mutableStateOf<String?>(null) }
    var fundingAsk by remember { mutableStateOf("") }
    var supportNeeded by remember { mutableStateOf("") }
    var pitchDeckUri by remember { mutableStateOf<Uri?>(null) }
    var logoUri by remember { mutableStateOf<Uri?>(null) }
    var demoLink by remember { mutableStateOf("") }
    var websiteUrl by remember { mutableStateOf("") }
    var socialLinks by remember { mutableStateOf("") }
    var organization by remember { mutableStateOf("") }

    var videoUri by remember { mutableStateOf<Uri?>(null) }
    var videoFileName by remember { mutableStateOf<String?>(null) }
    var uploadedVideoFileName by remember { mutableStateOf<String?>(null) } // Current existing one
    var uploadProgress by remember { mutableStateOf(0f) }
    var isVideoUploading by remember { mutableStateOf(false) }
    var isVideoUploadMode by remember { mutableStateOf(true) }

    var showErrors by remember { mutableStateOf(false) }
    var videoLinkError by remember { mutableStateOf<String?>(null) } // Validation state

    fun isValidVideoUrl(url: String): Boolean {
        if (url.isBlank()) return true
        val regex = Regex("^(https?://)?(www\\.)?(youtube\\.com|youtu\\.?be|vimeo\\.com)/.+", RegexOption.IGNORE_CASE)
        return regex.matches(url)
    }
    
    fun onVideoSelected(uri: Uri) {
        videoUri = uri
        // Get generic name or query
         val cursor = context.contentResolver.query(uri, null, null, null, null)
         val nameIndex = cursor?.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
         cursor?.moveToFirst()
         videoFileName = if (nameIndex != null && nameIndex >= 0) cursor.getString(nameIndex) else "video.mp4"
         cursor?.close()
         // Clear link if upload selected? Or keep them separate. Logic says preference to upload.
         demoLink = "" 
    }

    // Load Initial Data
    LaunchedEffect(Unit) {
        val uid = auth.currentUser?.uid
        if (uid != null) {
            val user = firestoreRepository.getUser(uid)
            val startup = firestoreRepository.getStartup(uid)
            
            if (user != null) {
                organization = user.organization // Pre-fill from CSV if available
                founderNames = user.name // Pre-fill Founder with Full Name
            }
            if (startup != null) {
                // If bulk imported, startupName is initially set to User Name. We want to clear that for the UI.
                if (user?.createdVia == "bulk" && startup.startupName == user?.name) {
                    startupName = "" 
                } else {
                    startupName = startup.startupName
                    demoLink = startup.demoVideoUrl ?: ""
                    uploadedVideoFileName = startup.uploadedVideoFileName
                    // Heuristic: If we have a filename, assume it was an upload mode
                    if (!uploadedVideoFileName.isNullOrEmpty()) {
                         // Keep upload mode true
                    } else if (demoLink.isNotEmpty()) {
                        // If link exists but no filename, likely external link
                        // isVideoUploadMode = false // But keep default true as user might want to upgrade
                    }
                }
            }
        }
    }

    // Launchers
    val logoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) logoUri = uri
    }
    val pitchDeckLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) pitchDeckUri = uri
    }

    fun submitDetails() {
        showErrors = true
        val isValid = startupName.isNotBlank() && startupSector != null && startupStage != null && pitchDeckUri != null && (isVideoUploadMode || videoLinkError == null)
        
        if (!isValid) return

        isLoading = true
        scope.launch {
            try {
                val uid = auth.currentUser?.uid ?: return@launch
                
                // Upload Files
                val uploadedLogoUrl = logoUri?.let { storageRepository.uploadStartupLogo(context, uid, it) }
                val uploadedPitchDeckUrl = pitchDeckUri?.let { storageRepository.uploadPitchDeck(context, uid, it, true) }
                
                var finalDemoUrl = demoLink
                var finalUploadedFileName = uploadedVideoFileName

                if (isVideoUploadMode && videoUri != null) {
                    isVideoUploading = true
                    finalDemoUrl = storageRepository.uploadStartupDemoVideo(context, uid, videoUri!!) { progress ->
                        uploadProgress = progress / 100f
                    }
                    finalUploadedFileName = videoFileName
                    isVideoUploading = false
                }

                // Update Startup Doc
                val updates = mutableMapOf<String, Any>(
                    "startupName" to startupName,
                    "founderNames" to founderNames,
                    "sector" to (startupSector ?: ""),
                    "stage" to (startupStage ?: ""),
                    "fundingAsk" to fundingAsk,
                    "supportNeeded" to supportNeeded,
                    "website" to websiteUrl,
                    "demoVideoUrl" to finalDemoUrl,
                    "uploadedVideoFileName" to (finalUploadedFileName ?: ""),
                    "socialLinks" to socialLinks,
                    "organization" to organization
                )
                if (uploadedLogoUrl != null) updates["logoUrl"] = uploadedLogoUrl
                if (uploadedPitchDeckUrl != null) updates["pitchDeckUrl"] = uploadedPitchDeckUrl

                firestoreRepository.updateStartup(uid, updates)

                // Update User Doc (Mark Complete)
                firestoreRepository.updateUser(uid, mapOf("hasCompletedRoleDetails" to true))

                onComplete()
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to update profile. Please try again.", Toast.LENGTH_LONG).show()
            } finally {
                isLoading = false
            }
        }
    }

    SoftScaffold(
        topBar = {
            SmallTopAppBar(
                title = { Text("Complete Profile") },
                actions = {
                    TextButton(onClick = onLogout) {
                        Text("Logout", color = MaterialTheme.colorScheme.error)
                    }
                },
                colors = TopAppBarDefaults.smallTopAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        bottomBar = {
            SoftCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding(),
                radius = 0.dp,
                elevation = 16.dp
            ) {
                SoftButton(
                    onClick = { submitDetails() },
                    text = "Submit & Continue",
                    isLoading = isLoading,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                "Final Step", 
                style = MaterialTheme.typography.headlineSmall, 
                fontWeight = FontWeight.Bold,
                color = AtmiyaPrimary
            )
            Text(
                "Please provide the missing details for your startup.", 
                style = MaterialTheme.typography.bodyMedium, 
                color = androidx.compose.ui.graphics.Color.Gray
            )
            Spacer(modifier = Modifier.height(24.dp))

            ValidatedTextField(startupName, { startupName = it }, "Startup / Project Name", errorMessage = if (showErrors && startupName.isBlank()) "Required" else null)
            Spacer(modifier = Modifier.height(16.dp))
            ValidatedTextField(founderNames, { founderNames = it }, "Founder Name(s)")
            Spacer(modifier = Modifier.height(16.dp))
            ValidatedTextField(organization, { organization = it }, "Organization / College (Optional)")
            Spacer(modifier = Modifier.height(16.dp))
            
            DropdownField("Sector", AppConstants.SECTOR_OPTIONS, startupSector, { startupSector = it }, if (showErrors && startupSector == null) "Required" else null)
            Spacer(modifier = Modifier.height(16.dp))
            DropdownField("Stage", AppConstants.STAGE_OPTIONS, startupStage, { startupStage = it }, if (showErrors && startupStage == null) "Required" else null)
            
            Spacer(modifier = Modifier.height(24.dp))
            Text("Pitch Material", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            
            // Pitch Deck Upload
            OutlinedCard(
                onClick = { pitchDeckLauncher.launch("application/pdf") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(TablerIcons.Upload, contentDescription = null, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text("Pitch Deck (PDF) *", fontWeight = FontWeight.Bold)
                        if (pitchDeckUri != null) {
                            Text("Selected: ${pitchDeckUri?.lastPathSegment}", style = MaterialTheme.typography.bodySmall, color = AtmiyaPrimary)
                        } else {
                            Text("Tap to upload", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
            if (showErrors && pitchDeckUri == null) {
                Text("Pitch deck is mandatory", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            Spacer(modifier = Modifier.height(16.dp))
            // Logo Upload
            OutlinedCard(
                onClick = { logoLauncher.launch("image/*") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(TablerIcons.Id, contentDescription = null, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text("Startup Logo (Optional)", fontWeight = FontWeight.Bold)
                        if (logoUri != null) {
                            Text("Selected", style = MaterialTheme.typography.bodySmall, color = AtmiyaPrimary)
                        } else {
                            Text("Tap to upload", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Spacer(modifier = Modifier.height(24.dp))
            Text("Product Demo", style = MaterialTheme.typography.titleMedium)
            
            // var isVideoUploadMode moved to top scope
            val videoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
                if (uri != null) {
                    onVideoSelected(uri)
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = isVideoUploadMode, onClick = { isVideoUploadMode = true })
                Text("Upload Video")
                Spacer(modifier = Modifier.width(16.dp))
                RadioButton(selected = !isVideoUploadMode, onClick = { isVideoUploadMode = false })
                Text("External Link")
            }

            if (isVideoUploadMode) {
                OutlinedCard(
                    onClick = { videoLauncher.launch("video/*") },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(TablerIcons.Upload, contentDescription = null, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text("Upload Demo Video (Max 100MB)", fontWeight = FontWeight.Bold)
                                if (videoUri != null) {
                                    Text("Selected: ${videoFileName}", style = MaterialTheme.typography.bodySmall, color = AtmiyaPrimary)
                                } else if (uploadedVideoFileName != null && demoLink.isNotEmpty()) {
                                     Text("Current: $uploadedVideoFileName", style = MaterialTheme.typography.bodySmall, color = AtmiyaPrimary)
                                } else {
                                    Text("Tap to select video", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                        
                        if (isVideoUploading) {
                            Spacer(modifier = Modifier.height(8.dp))
                            LinearProgressIndicator(
                                progress = uploadProgress, 
                                modifier = Modifier.fillMaxWidth()
                            )
                            Text("${(uploadProgress * 100).toInt()}% Uploading...", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            } else {
                 ValidatedTextField(
                     value = demoLink, 
                     onValueChange = { 
                         demoLink = it 
                         videoLinkError = if (isValidVideoUrl(it)) null else "Only YouTube/Vimeo allowed"
                     }, 
                     label = "Product Demo Link (YouTube/Vimeo)", 
                     errorMessage = videoLinkError
                 )
            }
            Spacer(modifier = Modifier.height(16.dp))
            ValidatedTextField(websiteUrl, { websiteUrl = it }, "Website URL (Optional)")
            Spacer(modifier = Modifier.height(16.dp))
            ValidatedTextField(socialLinks, { socialLinks = it }, "Social Media Handles (Optional)")

            Spacer(modifier = Modifier.height(24.dp))
            Text("Preferences", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            ValidatedTextField(
                value = fundingAsk,
                onValueChange = { 
                    val filtered = it.filter { char -> char.isDigit() }
                    val num = filtered.toLongOrNull()
                    if (filtered.isEmpty() || (num != null && num <= 1000000000L)) {
                        fundingAsk = filtered
                    }
                  }, 
                label = "Funding Requirement (Amount)",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                visualTransformation = IndianRupeeVisualTransformation()
            )
            Spacer(modifier = Modifier.height(16.dp))
            ValidatedTextField(supportNeeded, { supportNeeded = it }, "Type of Support Needed")
            
            Spacer(modifier = Modifier.height(80.dp)) // Bottom spacing
        }
    }
}
