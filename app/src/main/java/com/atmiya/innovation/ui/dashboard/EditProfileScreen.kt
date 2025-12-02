package com.atmiya.innovation.ui.dashboard

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.atmiya.innovation.repository.FirestoreRepository
import com.atmiya.innovation.ui.components.*
import com.atmiya.innovation.ui.theme.AtmiyaPrimary
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val firestoreRepository = remember { FirestoreRepository() }
    val auth = FirebaseAuth.getInstance()
    val user = auth.currentUser

    var isLoading by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }

    // Fields
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var state by remember { mutableStateOf("") }
    var bio by remember { mutableStateOf("") }
    
    // Role specific
    var role by remember { mutableStateOf("") }
    var startupName by remember { mutableStateOf("") }
    var sector by remember { mutableStateOf<String?>(null) }
    var stage by remember { mutableStateOf<String?>(null) }
    var teamSize by remember { mutableStateOf<String?>(null) }
    var fundingAsk by remember { mutableStateOf("") }
    var firmName by remember { mutableStateOf("") }
    var ticketSize by remember { mutableStateOf<String?>(null) }
    var expertise by remember { mutableStateOf<String?>(null) }
    var experience by remember { mutableStateOf("") }

    LaunchedEffect(user) {
        if (user != null) {
            val userProfile = firestoreRepository.getUser(user.uid)
            if (userProfile != null) {
                name = userProfile.name
                phone = userProfile.phoneNumber
                email = userProfile.email
                city = userProfile.city
                state = userProfile.region
                role = userProfile.role

                when (role) {
                    "startup" -> {
                        val startup = firestoreRepository.getStartup(user.uid)
                        if (startup != null) {
                            startupName = startup.startupName
                            sector = startup.sector.ifBlank { null }
                            stage = startup.stage.ifBlank { null }
                            teamSize = startup.teamSize.ifBlank { null }
                            fundingAsk = startup.fundingAsk
                            bio = startup.description
                        }
                    }
                    "investor" -> {
                        val investor = firestoreRepository.getInvestor(user.uid)
                        if (investor != null) {
                            firmName = investor.firmName
                            ticketSize = investor.ticketSizeMin.ifBlank { null } // Simplified mapping
                            bio = investor.bio
                        }
                    }
                    "mentor" -> {
                        val mentor = firestoreRepository.getMentor(user.uid)
                        if (mentor != null) {
                            expertise = mentor.expertiseAreas.firstOrNull()
                            experience = mentor.experienceYears
                            bio = mentor.bio
                        }
                    }
                }
            }
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = { Text("Edit Profile") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
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
                // Read-only fields
                ValidatedTextField(name, {}, "Full Name", readOnly = true)
                Spacer(modifier = Modifier.height(16.dp))
                ValidatedTextField(phone, {}, "Phone Number", readOnly = true)
                Spacer(modifier = Modifier.height(16.dp))

                // Editable Common Fields
                ValidatedTextField(email, { email = it }, "Email", keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email))
                Spacer(modifier = Modifier.height(16.dp))
                ValidatedTextField(city, { city = it }, "City")
                Spacer(modifier = Modifier.height(16.dp))
                DropdownField("State", listOf("Gujarat", "Maharashtra", "Delhi", "Karnataka", "Other"), state, { state = it })
                Spacer(modifier = Modifier.height(16.dp))

                // Role Specific
                when (role) {
                    "startup" -> {
                        ValidatedTextField(startupName, { startupName = it }, "Startup Name")
                        Spacer(modifier = Modifier.height(16.dp))
                        DropdownField("Sector", listOf("AgriTech", "HealthTech", "FinTech", "EdTech", "CleanTech", "Other"), sector, { sector = it })
                        Spacer(modifier = Modifier.height(16.dp))
                        DropdownField("Stage", listOf("Ideation", "Prototype", "MVP", "Early Traction", "Scaling"), stage, { stage = it })
                        Spacer(modifier = Modifier.height(16.dp))
                        DropdownField("Team Size", listOf("1-5", "6-20", "21-100", "100+"), teamSize, { teamSize = it })
                        Spacer(modifier = Modifier.height(16.dp))
                        ValidatedTextField(fundingAsk, { fundingAsk = it }, "Funding Ask (₹)", keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                    }
                    "investor" -> {
                        ValidatedTextField(firmName, { firmName = it }, "Firm Name")
                        Spacer(modifier = Modifier.height(16.dp))
                        DropdownField("Ticket Size", listOf("< 10L", "10L - 50L", "50L - 2Cr", "> 2Cr"), ticketSize, { ticketSize = it })
                    }
                    "mentor" -> {
                        DropdownField("Expertise", listOf("Tech", "Finance", "Marketing", "Legal"), expertise, { expertise = it })
                        Spacer(modifier = Modifier.height(16.dp))
                        ValidatedTextField(experience, { experience = it }, "Experience (Years)", keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                ValidatedTextField(bio, { bio = it }, "Bio / Description", minLines = 3)
                
                Spacer(modifier = Modifier.height(32.dp))
                
                SoftButton(
                    onClick = {
                        if (user != null) {
                            isSaving = true
                            scope.launch {
                                try {
                                    // Update User
                                    firestoreRepository.updateUser(user.uid, mapOf(
                                        "email" to email,
                                        "city" to city,
                                        "region" to state
                                    ))
                                    
                                    // Update Role Doc
                                    when (role) {
                                        "startup" -> {
                                            val currentStartup = firestoreRepository.getStartup(user.uid)
                                            val startupToSave = currentStartup?.copy(
                                                uid = user.uid,
                                                startupName = startupName,
                                                sector = sector ?: "",
                                                stage = stage ?: "",
                                                teamSize = teamSize ?: "",
                                                fundingAsk = fundingAsk,
                                                description = bio
                                            ) ?: com.atmiya.innovation.data.Startup(
                                                uid = user.uid,
                                                startupName = startupName,
                                                sector = sector ?: "",
                                                stage = stage ?: "",
                                                teamSize = teamSize ?: "",
                                                fundingAsk = fundingAsk,
                                                description = bio
                                            )
                                            firestoreRepository.createStartup(startupToSave)
                                        }
                                        "investor" -> {
                                            val currentInvestor = firestoreRepository.getInvestor(user.uid)
                                            val investorToSave = currentInvestor?.copy(
                                                uid = user.uid,
                                                firmName = firmName,
                                                ticketSizeMin = ticketSize ?: "",
                                                city = city,
                                                bio = bio
                                            ) ?: com.atmiya.innovation.data.Investor(
                                                uid = user.uid,
                                                firmName = firmName,
                                                ticketSizeMin = ticketSize ?: "",
                                                city = city,
                                                bio = bio
                                            )
                                            firestoreRepository.createInvestor(investorToSave)
                                        }
                                        "mentor" -> {
                                            val currentMentor = firestoreRepository.getMentor(user.uid)
                                            val mentorToSave = currentMentor?.copy(
                                                uid = user.uid,
                                                expertiseAreas = expertise?.let { listOf(it) } ?: emptyList(),
                                                experienceYears = experience,
                                                city = city,
                                                bio = bio
                                            ) ?: com.atmiya.innovation.data.Mentor(
                                                uid = user.uid,
                                                expertiseAreas = expertise?.let { listOf(it) } ?: emptyList(),
                                                experienceYears = experience,
                                                city = city,
                                                bio = bio
                                            )
                                            firestoreRepository.createMentor(mentorToSave)
                                        }
                                    }
                                    
                                    Toast.makeText(context, "Profile Updated", Toast.LENGTH_SHORT).show()
                                    onBack()
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                                } finally {
                                    isSaving = false
                                }
                            }
                        }
                    },
                    text = "Save Changes",
                    isLoading = isSaving,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
