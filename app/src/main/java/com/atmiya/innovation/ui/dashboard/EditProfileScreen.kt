package com.atmiya.innovation.ui.dashboard

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import compose.icons.TablerIcons
import compose.icons.tablericons.ArrowLeft
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
                    .padding(padding)
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
                                            "description" to bio
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
                                Toast.makeText(context, "Failed to update profile. Please try again.", Toast.LENGTH_LONG).show()
                            } finally {
                                isSaving = false
                            }
                        }
                    },
                    text = if(isSaving) "Saving..." else "Save Changes",
                    isLoading = isSaving,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
