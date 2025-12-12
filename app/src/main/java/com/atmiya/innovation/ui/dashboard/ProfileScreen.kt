package com.atmiya.innovation.ui.dashboard

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import compose.icons.TablerIcons
import compose.icons.tablericons.Pencil
import compose.icons.tablericons.Logout
import compose.icons.tablericons.Settings
import compose.icons.tablericons.CircleCheck
import compose.icons.tablericons.Share
import compose.icons.tablericons.List
import compose.icons.tablericons.ArrowLeft
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ExitToApp
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.atmiya.innovation.repository.FirestoreRepository
import com.atmiya.innovation.repository.StorageRepository
import com.atmiya.innovation.ui.theme.AtmiyaPrimary
import com.atmiya.innovation.ui.theme.AtmiyaSecondary
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onLogout: () -> Unit,
    onEditProfile: () -> Unit = {}, // Deprecated, kept for signature compatibility
    onBack: () -> Unit
) {
    val auth = FirebaseAuth.getInstance()
    val user = auth.currentUser
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    val firestoreRepository = remember { FirestoreRepository() }
    val storageRepository = remember { StorageRepository() }
    
    // UI State
    var isEditing by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    
    // Base Data
    var profileImageUrl by remember { mutableStateOf<String?>(null) }
    var userName by remember { mutableStateOf("") }
    var userEmail by remember { mutableStateOf("") }
    var userCity by remember { mutableStateOf("") }
    var userRegion by remember { mutableStateOf("") }
    var userBio by remember { mutableStateOf("") } // About/Description
    var userRole by remember { mutableStateOf("") } // "startup", "investor", "mentor"
    var displayRole by remember { mutableStateOf("") } // UI String
    
    // Startup Fields
    var startupName by remember { mutableStateOf("") }
    var startupSector by remember { mutableStateOf("") }
    var startupStage by remember { mutableStateOf("") }
    var startupFundingAsk by remember { mutableStateOf("") }
    
    // Investor Fields
    var investorFirm by remember { mutableStateOf("") }
    var investorTicketMin by remember { mutableStateOf("") }
    // var investorTicketMax by remember { mutableStateOf("") } // Removed
    var investorSectors by remember { mutableStateOf("") } // Comma separated
    var investorStages by remember { mutableStateOf("") }
    var investorType by remember { mutableStateOf("") }
    
    // Mentor Fields
    var mentorTitle by remember { mutableStateOf("") }
    var mentorOrg by remember { mutableStateOf("") }
    var mentorExpertise by remember { mutableStateOf("") } // Comma separated

    // Initial Load
    LaunchedEffect(user) {
        if (user != null) {
            val userProfile = firestoreRepository.getUser(user.uid)
            if (userProfile != null) {
                profileImageUrl = userProfile.profilePhotoUrl
                userName = userProfile.name
                userEmail = userProfile.email
                userCity = userProfile.city
                userRegion = userProfile.region
                userRole = userProfile.role
                
                when (userRole) {
                    "startup" -> {
                        val startup = firestoreRepository.getStartup(user.uid)
                        if (startup != null) {
                            startupName = startup.startupName
                            startupSector = startup.sector
                            startupStage = startup.stage
                            startupFundingAsk = startup.fundingAsk
                            userBio = startup.description
                            displayRole = "Startup"
                        }
                    }
                    "investor" -> {
                        val investor = firestoreRepository.getInvestor(user.uid)
                        if (investor != null) {
                            investorFirm = investor.firmName
                            investorTicketMin = investor.ticketSizeMin
                            // investorTicketMax = investor.ticketSizeMax
                            investorSectors = investor.sectorsOfInterest.joinToString(", ")
                            investorStages = investor.preferredStages.joinToString(", ")
                            investorType = investor.investmentType
                            userBio = investor.bio
                            displayRole = "Investor"
                        }
                    }
                    "mentor" -> {
                        val mentor = firestoreRepository.getMentor(user.uid)
                        if (mentor != null) {
                            mentorTitle = mentor.title
                            mentorOrg = mentor.organization
                            mentorExpertise = mentor.expertiseAreas.joinToString(", ")
                            userBio = mentor.bio
                            displayRole = "Mentor"
                        }
                    }
                    else -> {
                        displayRole = userRole.replaceFirstChar { it.uppercase() }
                    }
                }
            }
        }
    }
    
    // Image Picker
    val cropImageLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = com.canhub.cropper.CropImageContract()
    ) { result ->
        if (result.isSuccessful) {
            val uri = result.uriContent
            if (uri != null && user != null) {
                scope.launch {
                    try {
                        Toast.makeText(context, "Uploading...", Toast.LENGTH_SHORT).show()
                        val url = storageRepository.uploadProfilePhoto(context, user.uid, uri)
                        profileImageUrl = url
                        firestoreRepository.updateUser(user.uid, mapOf("profilePhotoUrl" to url))
                        
                        // Sync to Role Collection
                        when (userRole) {
                            "startup" -> firestoreRepository.updateStartup(user.uid, mapOf("logoUrl" to url))
                            "investor" -> firestoreRepository.updateInvestor(user.uid, mapOf("profilePhotoUrl" to url))
                            "mentor" -> firestoreRepository.updateMentor(user.uid, mapOf("profilePhotoUrl" to url))
                        }
                        
                        Toast.makeText(context, "Profile Photo Updated", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        Toast.makeText(context, "Upload Failed: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }
    val imagePickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        if (uri != null) {
             val cropOptions = com.canhub.cropper.CropImageOptions().apply {
                imageSourceIncludeGallery = true
                imageSourceIncludeCamera = false
                cropShape = com.canhub.cropper.CropImageView.CropShape.RECTANGLE
                fixAspectRatio = false
            }
            cropImageLauncher.launch(
                com.canhub.cropper.CropImageContractOptions(uri, cropOptions)
            )
        }
    }

    Scaffold(
        containerColor = Color.White
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 80.dp)
                    .imePadding() // Essential for scrolling when keyboard is open
            ) {
                // --- Top Banner & Photo (Same Look) ---
                Box(
                    modifier = Modifier.fillMaxWidth().height(420.dp)
                ) {
                     if (profileImageUrl != null) {
                        AsyncImage(
                            model = profileImageUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Box(
                            modifier = Modifier.fillMaxSize()
                                .background(Brush.verticalGradient(
                                    colors = listOf(Color(0xFFEEEEEE), Color(0xFFDDDDDD))
                                ))
                        ) {
                             Text(
                                text = if (userName.isNotEmpty()) userName.take(1).uppercase() else "A",
                                style = MaterialTheme.typography.displayLarge,
                                color = Color.Gray.copy(alpha = 0.5f),
                                modifier = Modifier.align(Alignment.Center)
                            )
                        }
                    }
                    // Gradient
                    Box(modifier = Modifier.fillMaxSize().background(
                        Brush.verticalGradient(colors = listOf(Color.Transparent, Color.Black.copy(alpha=0.7f)))
                    ))
                    
                    // Buttons: Back & Edit Photo
                    Row(
                        modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        IconButton(
                            onClick = onBack,
                            colors = IconButtonDefaults.iconButtonColors(containerColor = Color.Black.copy(alpha=0.3f))
                        ) {
                            Icon(TablerIcons.ArrowLeft, contentDescription = "Back", tint = Color.White, modifier = Modifier.size(28.dp))
                        }
                        if (isEditing) {
                            IconButton(
                                onClick = { imagePickerLauncher.launch("image/*") },
                                colors = IconButtonDefaults.iconButtonColors(containerColor = Color.Black.copy(alpha = 0.3f))
                            ) {
                                Icon(TablerIcons.Pencil, contentDescription = "Edit Photo", tint = Color.White, modifier = Modifier.size(24.dp))
                            }
                        }
                    }
                    
                    // Header Info
                    Column(
                        modifier = Modifier.align(Alignment.BottomStart).padding(24.dp)
                    ) {
                        if (isEditing) {
                             OutlinedTextField(
                                value = userName, 
                                onValueChange = { userName = it },
                                label = { Text("Full Name") },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = Color.White.copy(alpha=0.9f),
                                    unfocusedContainerColor = Color.White.copy(alpha=0.7f),
                                    focusedBorderColor = AtmiyaSecondary,
                                    unfocusedBorderColor = Color.Transparent
                                ),
                                modifier = Modifier.fillMaxWidth()
                             )
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = userName.ifEmpty { "User Name" },
                                    style = MaterialTheme.typography.headlineMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 28.sp
                                    ),
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(
                                    TablerIcons.CircleCheck, 
                                    contentDescription = "Verified", 
                                    tint = Color(0xFF29B6F6), // Blue
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        
                         Text(
                            text = displayRole,
                            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 18.sp),
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }
                }
                
                // --- Content Fields ---
                Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                         Text("Basic Info", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = AtmiyaPrimary)
                    }
                    
                    ProfileField(label = "Email Address", value = userEmail, isEditing = isEditing) { userEmail = it }
                    ProfileField(label = "City", value = userCity, isEditing = isEditing) { userCity = it }
                    ProfileField(label = "Region", value = userRegion, isEditing = isEditing) { userRegion = it }
                    ProfileField(label = "About", value = userBio, isEditing = isEditing, minLines = 3) { userBio = it }
                    
                    Divider(color = Color.LightGray.copy(alpha=0.5f), thickness = 1.dp)
                    
                    Text("Role Details", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = AtmiyaPrimary)
                    
                    // Role Specific
                     when (userRole) {
                        "startup" -> {
                            ProfileField(label = "Startup Name", value = startupName, isEditing = isEditing) { startupName = it }
                            ProfileField(label = "Sector", value = startupSector, isEditing = isEditing) { startupSector = it }
                            ProfileField(label = "Stage", value = startupStage, isEditing = isEditing) { startupStage = it }
                            ProfileField(label = "Funding Ask", value = startupFundingAsk, isEditing = isEditing) { startupFundingAsk = it }
                        }
                        "investor" -> {
                            ProfileField(label = "Firm Name", value = investorFirm, isEditing = isEditing) { investorFirm = it }
                            ProfileField(label = "Ticket Size", value = investorTicketMin, isEditing = isEditing) { investorTicketMin = it }
                            ProfileField(label = "Sectors (Comma sep)", value = investorSectors, isEditing = isEditing) { investorSectors = it }
                            ProfileField(label = "Preferred Stages (Comma sep)", value = investorStages, isEditing = isEditing) { investorStages = it }
                            ProfileField(label = "Investment Style", value = investorType, isEditing = isEditing) { investorType = it }
                        }
                        "mentor" -> {
                            ProfileField(label = "Job Title", value = mentorTitle, isEditing = isEditing) { mentorTitle = it }
                            ProfileField(label = "Organization", value = mentorOrg, isEditing = isEditing) { mentorOrg = it }
                            ProfileField(label = "Expertise (Comma sep)", value = mentorExpertise, isEditing = isEditing) { mentorExpertise = it }
                        }
                    }
                    
                    if (isEditing) {
                        Spacer(modifier = Modifier.height(32.dp))
                    } else {
                         // Logout at bottom when viewing
                         Spacer(modifier = Modifier.height(24.dp))
                    // Logout Option
                    TextButton(
                        onClick = onLogout,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.textButtonColors(contentColor = AtmiyaSecondary)
                    ) {
                        Icon(TablerIcons.Logout, contentDescription = null, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Logout")
                    }
                    }
                }
            }
            
            // FAB for Edit/Save
             FloatingActionButton(
                onClick = {
                    if (isEditing) {
                        // SAVE
                        if (user != null) {
                            scope.launch {
                                isSaving = true
                                try {
                                    // Base
                                    firestoreRepository.updateUser(user.uid, mapOf(
                                        "name" to userName, 
                                        "email" to userEmail,
                                        "city" to userCity,
                                        "region" to userRegion
                                    ))
                                    // Role
                                    when(userRole) {
                                        "startup" -> firestoreRepository.updateStartup(user.uid, mapOf(
                                            "startupName" to startupName,
                                            "sector" to startupSector,
                                            "stage" to startupStage,
                                            "fundingAsk" to startupFundingAsk,
                                            "description" to userBio
                                        ))
                                        "investor" -> firestoreRepository.updateInvestor(user.uid, mapOf(
                                            "name" to userName,
                                            "firmName" to investorFirm,
                                            "ticketSizeMin" to investorTicketMin,
                                            "sectorsOfInterest" to investorSectors.split(",").map{it.trim()},
                                            "preferredStages" to investorStages.split(",").map{it.trim()},
                                            "investmentType" to investorType,
                                            "bio" to userBio
                                        ))
                                        "mentor" -> firestoreRepository.updateMentor(user.uid, mapOf(
                                            "name" to userName,
                                            "title" to mentorTitle,
                                            "organization" to mentorOrg,
                                            "expertiseAreas" to mentorExpertise.split(",").map{it.trim()},
                                            "bio" to userBio
                                        ))
                                    }
                                    Toast.makeText(context, "Profile Saved", Toast.LENGTH_SHORT).show()
                                    isEditing = false
                                } catch(e: Exception) {
                                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                                } finally {
                                    isSaving = false
                                }
                            }
                        }
                    } else {
                        // START EDIT
                        isEditing = true
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(24.dp),
                containerColor = if (isEditing) AtmiyaPrimary else AtmiyaSecondary,
                contentColor = Color.White
            ) {
                 if (isSaving) {
                     CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                 } else {
                     Icon(if(isEditing) Icons.Outlined.Save else Icons.Outlined.Edit, contentDescription = if(isEditing) "Save" else "Edit")
                 }
            }
            
            // Cancel Button if Editing
            if (isEditing) {
                 SmallFloatingActionButton(
                    onClick = { isEditing = false },
                    modifier = Modifier.align(Alignment.BottomEnd).padding(bottom = 90.dp, end = 28.dp),
                    containerColor = Color.Gray,
                    contentColor = Color.White
                ) {
                    Icon(Icons.Outlined.Close, contentDescription = "Cancel")
                }
            }
        }
    }
}

@Composable
fun ProfileField(
    label: String,
    value: String,
    isEditing: Boolean,
    minLines: Int = 1,
    onValueChange: (String) -> Unit
) {
    if (isEditing) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            modifier = Modifier.fillMaxWidth(),
            minLines = minLines,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = AtmiyaSecondary,
                focusedLabelColor = AtmiyaSecondary
            )
        )
    } else {
        Column(modifier = Modifier.padding(vertical = 8.dp)) {
            Text(text = label, style = MaterialTheme.typography.labelMedium, color = Color.Gray)
            Text(text = value.ifEmpty { "-" }, style = MaterialTheme.typography.bodyLarge, color = AtmiyaPrimary)
        }
    }
}
