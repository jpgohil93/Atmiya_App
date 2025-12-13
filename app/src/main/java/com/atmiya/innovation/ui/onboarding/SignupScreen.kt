package com.atmiya.innovation.ui.onboarding

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import compose.icons.TablerIcons
import compose.icons.tablericons.Check
import compose.icons.tablericons.Lock
import compose.icons.tablericons.Eye
import compose.icons.tablericons.EyeOff
import compose.icons.tablericons.Id
import compose.icons.tablericons.Upload
import compose.icons.tablericons.Rocket
import compose.icons.tablericons.ChartLine
import compose.icons.tablericons.School
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.atmiya.innovation.data.*
import com.atmiya.innovation.repository.FirestoreRepository
import com.atmiya.innovation.repository.StorageRepository
import com.atmiya.innovation.ui.components.*
import com.atmiya.innovation.ui.theme.AtmiyaPrimary
import com.atmiya.innovation.ui.theme.AtmiyaSecondary
import com.atmiya.innovation.ui.theme.AtmiyaAccent
import com.atmiya.innovation.ui.auth.RoleCard
import com.google.firebase.Timestamp
import kotlinx.coroutines.tasks.await
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignupScreen(
    onSignupComplete: (String) -> Unit,
    onLogout: () -> Unit
) {
    val auth = FirebaseAuth.getInstance()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val firestoreRepository = remember { FirestoreRepository() }
    val storageRepository = remember { StorageRepository() }

    // Steps: 
    // 1: Phone Verification
    // 2: Role Selection (with Startup Type)
    // 3: Basic Details (Common)
    // 4: Role Specific Details
    // 5: Create Password
    var currentStep by remember { mutableIntStateOf(1) }
    val totalSteps = 5
    
    // -- State Variables --

    // Step 1: Phone Verification
    var phoneNumber by remember { mutableStateOf("") }
    var otpValue by remember { mutableStateOf("") }
    var verificationId by remember { mutableStateOf<String?>(null) }
    var isOtpSent by remember { mutableStateOf(false) }
    var isOtpError by remember { mutableStateOf(false) }
    var phoneError by remember { mutableStateOf<String?>(null) }
    var ticks by remember { mutableLongStateOf(60L) }
    var isTimerRunning by remember { mutableStateOf(false) }

    // Step 2: Role Selection
    var selectedRole by remember { mutableStateOf<String?>(null) } // "startup", "investor", "mentor"
    var selectedTrack by remember { mutableStateOf<String?>(null) } // "EDP", "ACC" (Only for Startup)
    
    // Step 3: Basic Details
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var state by remember { mutableStateOf("") }
    var organization by remember { mutableStateOf("") } // Used for Startup/Student
    var profilePhotoUri by remember { mutableStateOf<Uri?>(null) }

    // Step 4: Role Specific Details
    // Startup
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
    
    // Investor
    var firmName by remember { mutableStateOf("") }
    var ticketSize by remember { mutableStateOf<String?>(null) }
    var investmentSectors by remember { mutableStateOf<List<String>>(emptyList()) } // Multi-select
    var preferredStages by remember { mutableStateOf<List<String>>(emptyList()) }
    var investmentStyle by remember { mutableStateOf("") }
    
    // Mentor
    var expertiseArea by remember { mutableStateOf<String?>(null) }
    var topicsToTeach by remember { mutableStateOf("") }
    var experienceYears by remember { mutableStateOf("") }
    var bio by remember { mutableStateOf("") }

    // Step 5: Create Password
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordError by remember { mutableStateOf<String?>(null) }

    // General
    var isLoading by remember { mutableStateOf(false) }
    var showErrors by remember { mutableStateOf(false) }

    // -- Launchers --
    // -- Launchers --
    val cropImageLauncher = rememberLauncherForActivityResult(
        contract = com.canhub.cropper.CropImageContract()
    ) { result ->
        if (result.isSuccessful) {
            val uri = result.uriContent
            if (uri != null) profilePhotoUri = uri
        } else {
            val exception = result.error
            if (exception != null) {
                Toast.makeText(context, "Crop failed: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val photoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            val cropOptions = com.canhub.cropper.CropImageOptions().apply {
                imageSourceIncludeGallery = true
                imageSourceIncludeCamera = true
                cropShape = com.canhub.cropper.CropImageView.CropShape.OVAL
                aspectRatioX = 1
                aspectRatioY = 1
                fixAspectRatio = true
                guidelines = com.canhub.cropper.CropImageView.Guidelines.ON
            }
            cropImageLauncher.launch(
                com.canhub.cropper.CropImageContractOptions(uri, cropOptions)
            )
        }
    }
    val logoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) logoUri = uri
    }
    val pitchDeckLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) pitchDeckUri = uri
    }

    // -- Timer Logic --
    LaunchedEffect(isTimerRunning) {
        if (isTimerRunning) {
            val startTime = System.currentTimeMillis()
            while (ticks > 0) {
                val elapsed = (System.currentTimeMillis() - startTime) / 1000
                ticks = 60L - elapsed
                delay(1000)
            }
            isTimerRunning = false
        }
    }

    // -- Helper Functions --

    fun sendOtp(phone: String, isResend: Boolean = false) {
        if (isLoading && !isResend) return
        
        val activity = context.findActivity()
        if (activity == null) return

        isLoading = true
        val fullNumber = "+91$phone"
        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(fullNumber)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                    // Auto-verification
                    auth.signInWithCredential(credential).addOnCompleteListener { task ->
                        isLoading = false
                        if (task.isSuccessful) {
                            currentStep = 2 // Proceed to Role Selection
                        }
                    }
                }

                override fun onVerificationFailed(e: com.google.firebase.FirebaseException) {
                    isLoading = false
                    Toast.makeText(context, "Verification Failed: ${e.message}", Toast.LENGTH_LONG).show()
                }

                override fun onCodeSent(vId: String, token: PhoneAuthProvider.ForceResendingToken) {
                    isLoading = false
                    verificationId = vId
                    isOtpSent = true
                    ticks = 60L
                    isTimerRunning = true
                    Toast.makeText(context, "OTP Sent", Toast.LENGTH_SHORT).show()
                }
            })
            .build()
        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    fun verifyOtp(otp: String) {
        // --- REMOVED MANUAL BYPASS ---
        // We rely on Firebase Test Phone Numbers for "123456"
        
        val vid = verificationId ?: return
        isLoading = true
        val credential = PhoneAuthProvider.getCredential(vid, otp)
        
        auth.signInWithCredential(credential).addOnCompleteListener { task ->
            isLoading = false
            if (task.isSuccessful) {
                currentStep = 2 // Proceed to Role Selection
            } else {
                isOtpError = true
                otpValue = ""
                Toast.makeText(context, "Invalid OTP", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun submitSignup() {
        isLoading = true
        scope.launch {
            try {
                var user = auth.currentUser
                
                if (user == null) {
                    // Should not happen if Phone Auth succeeded, but handle edge case
                    throw Exception("User not authenticated")
                }

                // Link Password
                val syntheticEmail = "$phoneNumber@atmiya.com"
                val credential = EmailAuthProvider.getCredential(syntheticEmail, password)
                try {
                    user.linkWithCredential(credential).await()
                } catch (e: Exception) {
                    // If already linked or email exists, maybe just update password
                    try {
                        user.updatePassword(password).await()
                    } catch (e2: Exception) {
                        // Ignore if password update fails (e.g. recent login required), but we just logged in
                    }
                }

                // Upload Photos & Files
                val photoUrl = profilePhotoUri?.let { storageRepository.uploadProfilePhoto(context, user!!.uid, it) } ?: ""
                val uploadedLogoUrl = logoUri?.let { storageRepository.uploadStartupLogo(context, user!!.uid, it) }
                val uploadedPitchDeckUrl = pitchDeckUri?.let { storageRepository.uploadPitchDeck(context, user!!.uid, it, true) }

                // Generate Participant ID
                val participantId = if (selectedRole == "startup") {
                    val prefix = if (selectedTrack == "EDP") "AIF-EDP-" else "AIF-ACC-"
                    val randomNum = (100..999).random()
                    "$prefix$randomNum"
                } else null

                // Create User Record in Firestore
                val newUser = User(
                    uid = user!!.uid,
                    phoneNumber = phoneNumber,
                    role = selectedRole ?: "",
                    name = name,
                    email = email,
                    city = city,
                    region = state,
                    profilePhotoUrl = photoUrl,
                    participantId = participantId,
                    isOnboardingComplete = true,
                    createdAt = Timestamp.now(),
                    updatedAt = Timestamp.now()
                )
                firestoreRepository.createUser(newUser)

                // Create Role Specific Record
                when (selectedRole) {
                    "startup" -> {
                        val startup = Startup(
                            uid = user.uid,
                            startupType = if (selectedTrack == "EDP") "edp" else "accelerator",
                            track = selectedTrack ?: "",
                            startupName = startupName,
                            sector = startupSector ?: "",
                            stage = startupStage ?: "",
                            fundingAsk = fundingAsk,
                            teamSize = "", 
                            dpiitNumber = "", 
                            pitchDeckUrl = uploadedPitchDeckUrl,
                            logoUrl = uploadedLogoUrl,
                            description = supportNeeded, 
                            website = websiteUrl,
                            demoVideoUrl = demoLink,
                            socialLinks = socialLinks
                        )
                        firestoreRepository.createStartup(startup)
                    }
                    "investor" -> {
                        val investor = Investor(
                            uid = user.uid,
                            name = name,
                            firmName = firmName,
                            ticketSizeMin = ticketSize ?: "",
                            city = city,
                            bio = bio,
                            profilePhotoUrl = photoUrl,
                            sectorsOfInterest = investmentSectors,
                            preferredStages = preferredStages
                        )
                        firestoreRepository.createInvestor(investor)
                    }
                    "mentor" -> {
                        val mentor = Mentor(
                            uid = user.uid,
                            name = name,
                            expertiseAreas = expertiseArea?.let { listOf(it) } ?: emptyList(),
                            experienceYears = experienceYears,
                            city = city,
                            bio = bio,
                            profilePhotoUrl = photoUrl,
                            topicsToTeach = if (topicsToTeach.isNotBlank()) listOf(topicsToTeach) else emptyList()
                        )
                        firestoreRepository.createMentor(mentor)
                    }
                }
                
                onSignupComplete(selectedRole ?: "startup")
                
            } catch (e: Exception) {
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                isLoading = false
            }
        }
    }

    // -- UI --
    SoftScaffold(
        topBar = {
            Column {
                SmallTopAppBar(
                    title = { 
                        Text(
                            when (currentStep) {
                                1 -> "Verification"
                                2 -> "Role Selection"
                                3 -> "Basic Details"
                                4 -> "Role Details"
                                5 -> "Set Password"
                                else -> "Sign Up"
                            }
                        ) 
                    },
                    actions = {
                        BackToLoginButton(onClick = onLogout)
                    },
                    colors = TopAppBarDefaults.smallTopAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
                )
                // Progress Bar
                LinearProgressIndicator(
                    progress = currentStep / totalSteps.toFloat(),
                    modifier = Modifier.fillMaxWidth(),
                    color = AtmiyaPrimary,
                    trackColor = AtmiyaPrimary.copy(alpha = 0.2f)
                )
            }
        },
        bottomBar = {
            if (currentStep > 1) {
                SoftCard(modifier = Modifier.fillMaxWidth(), radius = 0.dp, elevation = 16.dp) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        SimpleBackButton(onClick = { currentStep-- }, enabled = !isLoading)

                        SoftButton(
                            onClick = {
                                showErrors = true
                                var isValid = false
                                when (currentStep) {
                                    2 -> {
                                        isValid = selectedRole != null
                                        if (selectedRole == "startup") {
                                            isValid = isValid && selectedTrack != null
                                        }
                                    }
                                    3 -> isValid = name.isNotBlank() && email.contains("@") && city.isNotBlank() && state.isNotBlank() && profilePhotoUri != null
                                    4 -> {
                                        isValid = when (selectedRole) {
                                            "startup" -> startupName.isNotBlank() && startupSector != null && startupStage != null && pitchDeckUri != null
                                            "investor" -> firmName.isNotBlank() && investmentSectors.isNotEmpty() && preferredStages.isNotEmpty()
                                            "mentor" -> expertiseArea != null && experienceYears.isNotBlank()
                                            else -> false
                                        }
                                    }
                                    5 -> {
                                        isValid = password.length >= 8 && password == confirmPassword
                                        if (!isValid) {
                                            if (password.length < 8) passwordError = "Min 8 chars"
                                            else if (password != confirmPassword) passwordError = "Passwords do not match"
                                        } else {
                                            passwordError = null
                                        }
                                    }
                                }
                                
                                if (isValid) {
                                    showErrors = false
                                    if (currentStep < 5) {
                                        currentStep++
                                    } else {
                                        submitSignup()
                                    }
                                }
                            },
                            text = if (currentStep == 5) "Submit" else "Next",
                            isLoading = isLoading,
                            modifier = Modifier.width(120.dp)
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            when (currentStep) {
                1 -> {
                    // Step 1: Phone Verification
                    Text("Verify Phone Number", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("We'll send you a verification code.", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                    Spacer(modifier = Modifier.height(32.dp))

                    if (!isOtpSent) {
                        SoftTextField(
                            value = phoneNumber,
                            onValueChange = { if (it.length <= 10 && it.all { c -> c.isDigit() }) phoneNumber = it },
                            label = "Mobile Number",
                            placeholder = "9876543210",
                            leadingIcon = { Text("+91", fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 16.dp, end = 8.dp)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            isError = phoneError != null
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        SoftButton(
                            onClick = {
                                if (phoneNumber.length == 10) {
                                    sendOtp(phoneNumber)
                                } else {
                                    phoneError = "Invalid number"
                                }
                            },
                            text = "Send OTP",
                            isLoading = isLoading,
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        Text("Enter OTP sent to +91 $phoneNumber", style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        OtpInput(otpValue, { 
                            otpValue = it 
                            if (it.length == 6) verifyOtp(it)
                        }, isOtpError)
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        if (isLoading) CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                            if (ticks > 0) {
                                Text("Resend in ${ticks}s", color = Color.Gray)
                            } else {
                                TextButton(onClick = { sendOtp(phoneNumber, true) }) { Text("Resend OTP") }
                            }
                        }
                        TextButton(
                            onClick = { isOtpSent = false; otpValue = "" },
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        ) { Text("Change Number") }
                    }
                }
                2 -> {
                    // Step 2: Role Selection
                    Text("Select your Role", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Choose the profile that best fits you.", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Startup Card
                    RoleCard(
                        title = "Startup", 
                        description = "I have an idea or business", 
                        color = Color(0xFFE3F2FD), // Pastel Blue
                        icon = TablerIcons.Rocket,
                        textColor = Color.Black,
                        isSelected = selectedRole == "startup",
                        onClick = { 
                            selectedRole = "startup"
                            if (selectedTrack == null) selectedTrack = "EDP" // Default to EDP if not set
                        }
                    )
                    
                    // Startup Type Selection (Visible only if Startup selected)
                    androidx.compose.animation.AnimatedVisibility(visible = selectedRole == "startup") {
                        Column(modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 16.dp)) {
                            Text("Select Program Type:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            // EDP Option
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedTrack = "EDP" }
                                    .background(
                                        if (selectedTrack == "EDP") AtmiyaPrimary.copy(alpha = 0.1f) else Color.Transparent, 
                                        RoundedCornerShape(8.dp)
                                    )
                                    .border(
                                        1.dp, 
                                        if (selectedTrack == "EDP") AtmiyaPrimary else Color.LightGray, 
                                        RoundedCornerShape(8.dp)
                                    )
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = selectedTrack == "EDP",
                                    onClick = { selectedTrack = "EDP" },
                                    colors = RadioButtonDefaults.colors(selectedColor = AtmiyaPrimary)
                                )
                                Column {
                                    Text("EDP (Idea Stage)", fontWeight = FontWeight.SemiBold)
                                    Text("For new ideas & prototypes", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            // Accelerator Option
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedTrack = "ACC" }
                                    .background(
                                        if (selectedTrack == "ACC") AtmiyaPrimary.copy(alpha = 0.1f) else Color.Transparent, 
                                        RoundedCornerShape(8.dp)
                                    )
                                    .border(
                                        1.dp, 
                                        if (selectedTrack == "ACC") AtmiyaPrimary else Color.LightGray, 
                                        RoundedCornerShape(8.dp)
                                    )
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = selectedTrack == "ACC",
                                    onClick = { selectedTrack = "ACC" },
                                    colors = RadioButtonDefaults.colors(selectedColor = AtmiyaPrimary)
                                )
                                Column {
                                    Text("Accelerator (Growth)", fontWeight = FontWeight.SemiBold)
                                    Text("For existing businesses", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                }
                            }
                        }
                    }

                    // Investor Card
                    RoleCard(
                        title = "Investor", 
                        description = "I want to fund startups", 
                        color = Color(0xFFFFF3E0), // Pastel Orange
                        icon = TablerIcons.ChartLine,
                        textColor = Color.Black,
                        isSelected = selectedRole == "investor",
                        onClick = { selectedRole = "investor" }
                    )
                    
                    // Mentor Card
                    RoleCard(
                        title = "Mentor", 
                        description = "I want to guide", 
                        color = Color(0xFFE8F5E9), // Pastel Green
                        icon = TablerIcons.School,
                        textColor = Color.Black,
                        isSelected = selectedRole == "mentor",
                        onClick = { selectedRole = "mentor" }
                    )
                    
                    if (showErrors && selectedRole == null) {
                        Text("Please select a role", color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp))
                    }
                }
                3 -> {
                    // Step 3: Basic Details
                    Text("Basic Details", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    PhotoUploadField(profilePhotoUri, { photoLauncher.launch("image/*") }, null)
                    if (showErrors && profilePhotoUri == null) {
                        Text("Profile photo is mandatory", color = MaterialTheme.colorScheme.error, modifier = Modifier.align(Alignment.CenterHorizontally))
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    ValidatedTextField(name, { name = it }, "Full Name", errorMessage = if (showErrors && name.isBlank()) "Required" else null)
                    Spacer(modifier = Modifier.height(16.dp))
                    ValidatedTextField(email, { email = it }, "Email Address", keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), errorMessage = if (showErrors && (email.isBlank() || !email.contains("@"))) "Valid email required" else null)
                    Spacer(modifier = Modifier.height(16.dp))
                    ValidatedTextField(city, { city = it }, "City", errorMessage = if (showErrors && city.isBlank()) "Required" else null)
                    Spacer(modifier = Modifier.height(16.dp))
                    DropdownField("Region", listOf("Saurashtra", "North Gujarat", "Central Gujarat", "South Gujarat", "Kutch", "Other"), state, { state = it }, if (showErrors && state.isBlank()) "Required" else null)
                    
                    if (selectedRole == "startup") {
                         Spacer(modifier = Modifier.height(16.dp))
                         ValidatedTextField(organization, { organization = it }, "Organization / College (Optional)")
                    }
                }
                4 -> {
                    // Step 4: Role Details
                    Text("Role Specific Details", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    when (selectedRole) {
                        "startup" -> {
                            Text("Startup Information", style = MaterialTheme.typography.titleMedium)
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            ValidatedTextField(startupName, { startupName = it }, "Startup / Project Name", errorMessage = if (showErrors && startupName.isBlank()) "Required" else null)
                            Spacer(modifier = Modifier.height(16.dp))
                            ValidatedTextField(founderNames, { founderNames = it }, "Founder Name(s)")
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
                            ValidatedTextField(demoLink, { demoLink = it }, "Product Demo Link (Optional)")
                            Spacer(modifier = Modifier.height(16.dp))
                            ValidatedTextField(websiteUrl, { websiteUrl = it }, "Website URL (Optional)")
                            Spacer(modifier = Modifier.height(16.dp))
                            ValidatedTextField(socialLinks, { socialLinks = it }, "Social Media Handles (Optional)")

                            Spacer(modifier = Modifier.height(24.dp))
                            Text("Preferences", style = MaterialTheme.typography.titleMedium)
                            Spacer(modifier = Modifier.height(8.dp))
                            ValidatedTextField(fundingAsk, { fundingAsk = it }, "Funding Requirement (Range/Amount)")
                            Spacer(modifier = Modifier.height(16.dp))
                            ValidatedTextField(supportNeeded, { supportNeeded = it }, "Type of Support Needed")
                        }
                        "investor" -> {
                            ValidatedTextField(firmName, { firmName = it }, "Organization / Firm Name", errorMessage = if (showErrors && firmName.isBlank()) "Required" else null)
                            Spacer(modifier = Modifier.height(16.dp))
                            DropdownField("Ticket Size", listOf("< 10L", "10L - 50L", "50L - 2Cr", "> 2Cr"), ticketSize, { ticketSize = it }, null)
                            Spacer(modifier = Modifier.height(16.dp))
                            MultiSelectDropdownField(
                                label = "Sectors of Interest",
                                options = AppConstants.SECTOR_OPTIONS,
                                selectedOptions = investmentSectors,
                                onOptionSelected = { option ->
                                    if (investmentSectors.contains(option)) {
                                        investmentSectors -= option
                                    } else {
                                        investmentSectors += option
                                    }
                                },
                                errorMessage = if (showErrors && investmentSectors.isEmpty()) "Required" else null
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            MultiSelectDropdownField(
                                label = "Preferred Stages",
                                options = AppConstants.STAGE_OPTIONS,
                                selectedOptions = preferredStages,
                                onOptionSelected = { option ->
                                    if (preferredStages.contains(option)) {
                                        preferredStages -= option
                                    } else {
                                        preferredStages += option
                                    }
                                },
                                errorMessage = if (showErrors && preferredStages.isEmpty()) "Required" else null
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            ValidatedTextField(investmentStyle, { investmentStyle = it }, "Investment Style (Equity/Debt)")
                            Spacer(modifier = Modifier.height(16.dp))
                            ValidatedTextField(bio, { bio = it }, "Profile Bio", minLines = 3)
                            Spacer(modifier = Modifier.height(16.dp))
                            ValidatedTextField(websiteUrl, { websiteUrl = it }, "Website / LinkedIn (Optional)")
                        }
                        "mentor" -> {
                            DropdownField("Expertise", listOf("Tech", "Finance", "Marketing", "Legal", "Strategy", "HR"), expertiseArea, { expertiseArea = it }, if (showErrors && expertiseArea == null) "Required" else null)
                            Spacer(modifier = Modifier.height(16.dp))
                            ValidatedTextField(topicsToTeach, { topicsToTeach = it }, "Topics you want to teach")
                            Spacer(modifier = Modifier.height(16.dp))
                            ValidatedTextField(experienceYears, { experienceYears = it }, "Experience (Years)", keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), errorMessage = if (showErrors && experienceYears.isBlank()) "Required" else null)
                            Spacer(modifier = Modifier.height(16.dp))
                            ValidatedTextField(organization, { organization = it }, "Current Role / Organization")
                            Spacer(modifier = Modifier.height(16.dp))
                            ValidatedTextField(websiteUrl, { websiteUrl = it }, "Website / LinkedIn (Optional)")
                        }
                    }
                }
                5 -> {
                    // Step 5: Create Password
                    Text("Create Password", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text("Secure your account with a strong password.", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    PasswordTextField(password, { password = it }, "New Password")
                    Spacer(modifier = Modifier.height(16.dp))
                    PasswordTextField(confirmPassword, { confirmPassword = it }, "Confirm Password")
                    
                    if (passwordError != null) {
                        Text(passwordError!!, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun PasswordTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String
) {
    var passwordVisible by remember { mutableStateOf(false) }
    
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        trailingIcon = {
            val image = if (passwordVisible) TablerIcons.EyeOff else TablerIcons.Eye 
            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                Icon(imageVector = image, contentDescription = null)
            }
        },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = AtmiyaPrimary,
            unfocusedBorderColor = Color.Gray.copy(alpha = 0.5f)
        )
    )
}

// Helper to find Activity from Context
fun android.content.Context.findActivity(): android.app.Activity? {
    var context = this
    while (context is android.content.ContextWrapper) {
        if (context is android.app.Activity) return context
        context = context.baseContext
    }
    return null
}

@Composable
fun OtpInput(
    otpValue: String,
    onOtpChange: (String) -> Unit,
    isError: Boolean
) {
    val focusRequester = remember { FocusRequester() }
    val keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)

    // Hidden TextField to handle input
    BasicTextField(
        value = otpValue,
        onValueChange = {
            if (it.length <= 6 && it.all { char -> char.isDigit() }) {
                onOtpChange(it)
            }
        },
        keyboardOptions = keyboardOptions,
        modifier = Modifier
            .focusRequester(focusRequester)
            .size(1.dp) 
            .alpha(0f)
    )

    // Visible Boxes
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { focusRequester.requestFocus() } // Make the whole row clickable to focus
    ) {
        for (i in 0 until 6) {
            val char = if (i < otpValue.length) otpValue[i].toString() else ""
            val isFocused = i == otpValue.length
            
            Box(
                modifier = Modifier
                    .weight(1f)
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isError) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surface)
                    .border(
                        width = if (isFocused) 2.dp else 1.dp,
                        color = if (isFocused) AtmiyaPrimary else if (isError) MaterialTheme.colorScheme.error else Color.Gray.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = char,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isError) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
    
    // Request focus on start safely
    LaunchedEffect(Unit) {
        try {
            delay(300) 
            focusRequester.requestFocus()
        } catch (e: Exception) {
            // Ignore focus errors
        }
    }
}
