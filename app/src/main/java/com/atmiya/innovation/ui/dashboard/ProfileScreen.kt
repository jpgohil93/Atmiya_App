package com.atmiya.innovation.ui.dashboard

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.atmiya.innovation.repository.FirestoreRepository
import com.atmiya.innovation.repository.StorageRepository
import com.atmiya.innovation.ui.components.DetailRow
import com.atmiya.innovation.ui.components.SectionHeader
import com.atmiya.innovation.ui.components.UserAvatar
import com.atmiya.innovation.ui.theme.AtmiyaPrimary
import com.atmiya.innovation.ui.theme.AtmiyaSecondary
import com.google.firebase.auth.FirebaseAuth
import compose.icons.TablerIcons
import compose.icons.tablericons.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onLogout: () -> Unit,
    onEditProfile: () -> Unit = {},
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
    var userPhone by remember { mutableStateOf("") } // Added Phone
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
    var startupFounderNames by remember { mutableStateOf("") } // Added
    var startupOrg by remember { mutableStateOf("") } // Added
    var startupSupportNeeded by remember { mutableStateOf("") } // Added
    var startupWebsite by remember { mutableStateOf("") } // Added
    var startupSocial by remember { mutableStateOf("") } // Added
    var startupPitchDeckUrl by remember { mutableStateOf<String?>(null) } // Added
    var startupLogoUrl by remember { mutableStateOf<String?>(null) } // Added
    
    // Investor Fields
    var investorFirm by remember { mutableStateOf("") }
    var investorTicketMin by remember { mutableStateOf("") }
    var investorSectors by remember { mutableStateOf("") } // Comma separated
    var investorStages by remember { mutableStateOf("") }
    var investorType by remember { mutableStateOf("") }
    var investorWebsite by remember { mutableStateOf("") } // Added
    
    // Mentor Fields
    var mentorTitle by remember { mutableStateOf("") }
    var mentorOrg by remember { mutableStateOf("") }
    var mentorExpertise by remember { mutableStateOf("") } // Comma separated
    var mentorExperience by remember { mutableStateOf("") } // Added
    var mentorTopics by remember { mutableStateOf("") } // Added

    // Initial Load
    LaunchedEffect(user) {
        if (user != null) {
            val userProfile = firestoreRepository.getUser(user.uid)
            if (userProfile != null) {
                profileImageUrl = userProfile.profilePhotoUrl
                userName = userProfile.name
                userEmail = userProfile.email
                userPhone = userProfile.phoneNumber // Loaded
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
                            startupFounderNames = startup.founderNames
                            startupOrg = startup.organization
                            startupSupportNeeded = startup.supportNeeded
                            startupWebsite = startup.website
                            startupSocial = startup.socialLinks
                            startupPitchDeckUrl = startup.pitchDeckUrl
                            startupLogoUrl = startup.logoUrl
                            userBio = startup.description
                            displayRole = "Startup"
                        }
                    }
                    "investor" -> {
                        val investor = firestoreRepository.getInvestor(user.uid)
                        if (investor != null) {
                            investorFirm = investor.firmName
                            investorTicketMin = investor.ticketSizeMin
                            investorSectors = investor.sectorsOfInterest.joinToString(", ")
                            investorStages = investor.preferredStages.joinToString(", ")
                            investorType = investor.investmentType
                            investorWebsite = investor.website
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
                            mentorExperience = mentor.experienceYears
                            mentorTopics = mentor.topicsToTeach.joinToString(", ")
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
    
    // Image Picker Logic (Profile Photo)
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
                        
                        // Sync to Role Collection for Profile Photo
                        when (userRole) {
                            "investor" -> firestoreRepository.updateInvestor(user.uid, mapOf("profilePhotoUrl" to url))
                            "mentor" -> firestoreRepository.updateMentor(user.uid, mapOf("profilePhotoUrl" to url))
                        }
                        
                        Toast.makeText(context, "Profile Photo Updated", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        Toast.makeText(context, "Profile photo upload failed. Please try again.", Toast.LENGTH_SHORT).show()
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

    // Startup Logo Launcher
    val logoLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        if (uri != null && user != null) {
            scope.launch {
                try {
                    Toast.makeText(context, "Uploading Logo...", Toast.LENGTH_SHORT).show()
                    val url = storageRepository.uploadStartupLogo(context, user.uid, uri)
                    startupLogoUrl = url
                    profileImageUrl = url // Update local state for immediate UI feedback
                    firestoreRepository.updateStartup(user.uid, mapOf("logoUrl" to url))
                    // Sync to users collection so listings show the updated image
                    firestoreRepository.updateUser(user.uid, mapOf("profilePhotoUrl" to url))
                    Toast.makeText(context, "Logo Updated", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(context, "Logo Upload Failed", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Pitch Deck Launcher
    val pitchDeckLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        if (uri != null && user != null) {
            scope.launch {
                try {
                    Toast.makeText(context, "Uploading Pitch Deck...", Toast.LENGTH_SHORT).show()
                    val isPdf = context.contentResolver.getType(uri)?.contains("pdf") == true
                    val url = storageRepository.uploadPitchDeck(context, user.uid, uri, isPdf)
                    startupPitchDeckUrl = url
                    firestoreRepository.updateStartup(user.uid, mapOf("pitchDeckUrl" to url))
                    Toast.makeText(context, "Pitch Deck Updated", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(context, "Pitch Deck Upload Failed", Toast.LENGTH_SHORT).show()
                }
            }
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
                    .imePadding()
            ) {
                // --- Hero Section (Premium Look) ---
                Box(
                    modifier = Modifier.fillMaxWidth().height(420.dp)
                ) {
                    UserAvatar(
                        model = profileImageUrl,
                        name = if (userName.isNotEmpty()) userName else "User",
                        modifier = Modifier.fillMaxSize(),
                        size = null,
                        shape = androidx.compose.ui.graphics.RectangleShape,
                        fontSize = MaterialTheme.typography.displayLarge.fontSize
                    )
                    
                    // Smooth Gradient (3-stop fade)
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colorStops = arrayOf(
                                        0.0f to Color.Transparent,
                                        0.6f to Color.Transparent,
                                        1.0f to Color.White // Seamless blend to background
                                    )
                                )
                            )
                    )
                    
                    // Buttons: Back & Edit Photo
                    Row(
                        modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        IconButton(
                            onClick = onBack,
                            colors = IconButtonDefaults.iconButtonColors(containerColor = Color.Black.copy(alpha=0.3f))
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                        }
                        if (isEditing) {
                            IconButton(
                                onClick = { imagePickerLauncher.launch("image/*") },
                                colors = IconButtonDefaults.iconButtonColors(containerColor = AtmiyaSecondary)
                            ) {
                                Icon(TablerIcons.Pencil, contentDescription = "Edit Photo", tint = Color.White, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                    
                    // Header Info Overlay
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
                                    style = MaterialTheme.typography.displaySmall.copy(
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = Color(0xFF1F2937) // Dark Text on White Gradient
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(
                                    Icons.Outlined.CheckCircle, 
                                    contentDescription = "Verified", 
                                    tint = AtmiyaSecondary, 
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        
                         Text(
                            text = displayRole,
                            style = MaterialTheme.typography.titleMedium,
                            color = AtmiyaPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                
                // --- Content Fields ---
                Column(modifier = Modifier.padding(24.dp)) {
                    
                    SectionHeader("Basic Info")
                    
                    ProfileField(label = "Email Address", value = userEmail, isEditing = isEditing, icon = TablerIcons.Mail) { userEmail = it }
                    if (userPhone.isNotEmpty()) {
                        ProfileField(label = "Mobile Number", value = com.atmiya.innovation.utils.StringUtils.formatPhoneNumber(userPhone), isEditing = false, icon = TablerIcons.Phone) { }
                    }
                    ProfileField(label = "City", value = userCity, isEditing = isEditing, icon = TablerIcons.MapPin) { userCity = it }
                    ProfileField(label = "Region", value = userRegion, isEditing = isEditing, icon = TablerIcons.World) { userRegion = it }
                    ProfileField(label = "About", value = userBio, isEditing = isEditing, minLines = 3, icon = TablerIcons.InfoCircle) { userBio = it }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    HorizontalDivider(color = Color.LightGray.copy(alpha=0.3f))
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    SectionHeader("Role Details")
                    
                    // Role Specific
                     when (userRole) {
                        "startup" -> {
                            ProfileField(label = "Startup Name", value = startupName, isEditing = isEditing, icon = TablerIcons.Home) { startupName = it }
                            ProfileField(label = "Founder Name(s)", value = startupFounderNames, isEditing = isEditing, icon = TablerIcons.Users) { startupFounderNames = it }
                            ProfileField(label = "Organization/College", value = startupOrg, isEditing = isEditing, icon = TablerIcons.Building) { startupOrg = it }
                            ProfileField(label = "Sector", value = startupSector, isEditing = isEditing, icon = TablerIcons.Hash) { startupSector = it }
                            ProfileField(label = "Stage", value = startupStage, isEditing = isEditing, icon = TablerIcons.ChartBar) { startupStage = it }
                            ProfileField(label = "Funding Ask", value = startupFundingAsk, isEditing = isEditing, icon = TablerIcons.CurrencyRupee) { startupFundingAsk = it }
                            ProfileField(label = "Support Needed", value = startupSupportNeeded, isEditing = isEditing, icon = TablerIcons.Help) { startupSupportNeeded = it }
                            ProfileField(label = "Website", value = startupWebsite, isEditing = isEditing, icon = TablerIcons.World) { startupWebsite = it }
                            ProfileField(label = "Social Links", value = startupSocial, isEditing = isEditing, icon = TablerIcons.BrandLinkedin) { startupSocial = it }
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            SectionHeader("Company Assets")
                            
                            // Logo Field
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(TablerIcons.Photo, contentDescription = null, tint = Color(0xFF9CA3AF))
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column {
                                        Text("Startup Logo", style = MaterialTheme.typography.bodyMedium)
                                        if (startupLogoUrl != null) {
                                            Text("Uploaded", style = MaterialTheme.typography.bodySmall, color = AtmiyaPrimary)
                                        } else {
                                            Text("No logo uploaded", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                        }
                                    }
                                }
                                if (isEditing) {
                                    Button(
                                        onClick = { logoLauncher.launch("image/*") },
                                        colors = ButtonDefaults.buttonColors(containerColor = AtmiyaSecondary)
                                    ) {
                                        Text("Upload")
                                    }
                                }
                            }
                            
                            // Pitch Deck Field
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(TablerIcons.Presentation, contentDescription = null, tint = Color(0xFF9CA3AF))
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column {
                                        Text("Pitch Deck", style = MaterialTheme.typography.bodyMedium)
                                        if (startupPitchDeckUrl != null) {
                                            Text("Uploaded", style = MaterialTheme.typography.bodySmall, color = AtmiyaPrimary)
                                        } else {
                                            Text("No deck uploaded", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                        }
                                    }
                                }
                                if (isEditing) {
                                    Button(
                                        onClick = { pitchDeckLauncher.launch("application/pdf") },
                                        colors = ButtonDefaults.buttonColors(containerColor = AtmiyaSecondary)
                                    ) {
                                        Text("Upload")
                                    }
                                }
                            }
                        }
                        "investor" -> {
                            ProfileField(label = "Firm Name", value = investorFirm, isEditing = isEditing, icon = TablerIcons.Building) { investorFirm = it }
                            ProfileField(label = "Ticket Size (Min)", value = investorTicketMin, isEditing = isEditing, icon = TablerIcons.Coin) { investorTicketMin = it }
                            ProfileField(label = "Sectors (Comma sep)", value = investorSectors, isEditing = isEditing, icon = TablerIcons.ChartPie) { investorSectors = it }
                            ProfileField(label = "Preferred Stages (Comma sep)", value = investorStages, isEditing = isEditing, icon = TablerIcons.Bulb) { investorStages = it }
                            ProfileField(label = "Investment Style", value = investorType, isEditing = isEditing, icon = TablerIcons.Briefcase) { investorType = it }
                            ProfileField(label = "Website / LinkedIn", value = investorWebsite, isEditing = isEditing, icon = TablerIcons.World) { investorWebsite = it }
                        }
                        "mentor" -> {
                            ProfileField(label = "Job Title", value = mentorTitle, isEditing = isEditing, icon = TablerIcons.Id) { mentorTitle = it }
                            ProfileField(label = "Organization", value = mentorOrg, isEditing = isEditing, icon = TablerIcons.Building) { mentorOrg = it }
                            ProfileField(label = "Experience (Years)", value = mentorExperience, isEditing = isEditing, icon = TablerIcons.Clock) { mentorExperience = it }
                            ProfileField(label = "Expertise (Comma sep)", value = mentorExpertise, isEditing = isEditing, icon = TablerIcons.Certificate) { mentorExpertise = it }
                            ProfileField(label = "Topics to Teach", value = mentorTopics, isEditing = isEditing, icon = TablerIcons.Book) { mentorTopics = it }
                        }
                    }
                    
                    if (!isEditing) {
                         Spacer(modifier = Modifier.height(48.dp))
                         // Logout Option
                         TextButton(
                             onClick = onLogout,
                             modifier = Modifier.fillMaxWidth().height(50.dp),
                             colors = ButtonDefaults.textButtonColors(contentColor = Color.Red.copy(alpha=0.8f))
                         ) {
                             Icon(TablerIcons.Logout, contentDescription = null, modifier = Modifier.size(20.dp))
                             Spacer(modifier = Modifier.width(8.dp))
                             Text("Logout", fontSize = 16.sp)
                         }
                    }
                }
            }
            
            // FAB for Edit/Save
             FloatingActionButton(
                onClick = {
                    if (isEditing) {
                        // SAVE Logic
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
                                            "description" to userBio,
                                            "founderNames" to startupFounderNames,
                                            "organization" to startupOrg,
                                            "supportNeeded" to startupSupportNeeded,
                                            "website" to startupWebsite,
                                            "socialLinks" to startupSocial
                                        ))
                                        "investor" -> firestoreRepository.updateInvestor(user.uid, mapOf(
                                            "name" to userName,
                                            "firmName" to investorFirm,
                                            "ticketSizeMin" to investorTicketMin,
                                            "sectorsOfInterest" to investorSectors.split(",").map{it.trim()},
                                            "preferredStages" to investorStages.split(",").map{it.trim()},
                                            "investmentType" to investorType,
                                            "website" to investorWebsite,
                                            "bio" to userBio
                                        ))
                                        "mentor" -> firestoreRepository.updateMentor(user.uid, mapOf(
                                            "name" to userName,
                                            "title" to mentorTitle,
                                            "organization" to mentorOrg,
                                            "expertiseAreas" to mentorExpertise.split(",").map{it.trim()},
                                            "experienceYears" to mentorExperience,
                                            "topicsToTeach" to mentorTopics.split(",").map{it.trim()},
                                            "bio" to userBio
                                        ))
                                    }
                                    Toast.makeText(context, "Profile Saved", Toast.LENGTH_SHORT).show()
                                    isEditing = false
                                } catch(e: Exception) {
                                    Toast.makeText(context, "Failed to load profile. Please refresh.", Toast.LENGTH_LONG).show()
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
                contentColor = Color.White,
                shape = CircleShape
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
                    modifier = Modifier.align(Alignment.BottomEnd).padding(bottom = 90.dp, end = 32.dp),
                    containerColor = Color(0xFFE5E7EB),
                    contentColor = Color(0xFF4B5563),
                    shape = CircleShape
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
    icon: ImageVector? = null,
    onValueChange: (String) -> Unit
) {
    if (isEditing) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            modifier = Modifier.fillMaxWidth().padding(vertical=4.dp),
            minLines = minLines,
            leadingIcon = if (icon != null) { { Icon(icon, contentDescription=null, tint=AtmiyaSecondary) } } else null,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = AtmiyaSecondary,
                focusedLabelColor = AtmiyaSecondary
            ),
            shape = RoundedCornerShape(12.dp)
        )
    } else {
        // Reuse shared DetailRow for consistent viewing experience
        DetailRow(label = label, value = value, icon = icon)
    }
}
