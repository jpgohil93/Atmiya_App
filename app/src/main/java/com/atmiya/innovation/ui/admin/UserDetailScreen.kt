package com.atmiya.innovation.ui.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.atmiya.innovation.data.User
import com.atmiya.innovation.repository.FirestoreRepository
import com.atmiya.innovation.ui.theme.AtmiyaPrimary
import com.atmiya.innovation.ui.theme.AtmiyaSecondary
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserDetailScreen(
    userId: String,
    onBack: () -> Unit,
) {
    var user by remember { mutableStateOf<User?>(null) }
    var startup by remember { mutableStateOf<com.atmiya.innovation.data.Startup?>(null) }
    var investor by remember { mutableStateOf<com.atmiya.innovation.data.Investor?>(null) }
    var mentor by remember { mutableStateOf<com.atmiya.innovation.data.Mentor?>(null) }
    
    var isLoading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()
    val firestoreRepository = remember { FirestoreRepository() }

    LaunchedEffect(userId) {
        val fetchedUser = firestoreRepository.getUser(userId)
        user = fetchedUser
        if (fetchedUser != null) {
            when (fetchedUser.role) {
                "startup" -> startup = firestoreRepository.getStartup(userId)
                "investor" -> investor = firestoreRepository.getInvestor(userId)
                "mentor" -> mentor = firestoreRepository.getMentor(userId)
            }
        }
        isLoading = false
    }

    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = { Text("User Details") },
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
        } else if (user != null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Profile Header
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Profile Photo
                    if (user?.profilePhotoUrl != null) {
                        AsyncImage(
                            model = user?.profilePhotoUrl,
                            contentDescription = "Profile Photo",
                            modifier = Modifier.size(80.dp).clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier.size(80.dp).clip(CircleShape).background(Color.Gray.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(40.dp), tint = Color.Gray)
                        }
                    }
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    Column {
                        Text(
                            text = user!!.name,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = AtmiyaPrimary
                        )
                        Text(
                            text = user!!.role.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() },
                            style = MaterialTheme.typography.bodyMedium,
                            color = AtmiyaSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(16.dp))

                // Basic Info
                DetailItem("Email", user!!.email)
                DetailItem("Phone", user!!.phoneNumber)
                DetailItem("City", user!!.city)
                DetailItem("State", user!!.region)
                DetailItem("Status", if (user!!.isBlocked) "Blocked" else "Active", if (user!!.isBlocked) MaterialTheme.colorScheme.error else Color.Green)

                Spacer(modifier = Modifier.height(24.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(16.dp))

                // Role Specific Info
                Text("Profile Details", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))

                when (user!!.role) {
                    "startup" -> {
                        if (startup != null) {
                            DetailItem("Startup Name", startup!!.startupName)
                            DetailItem("Sector", startup!!.sector)
                            DetailItem("Stage", startup!!.stage)
                            DetailItem("Funding Ask", startup!!.fundingAsk)
                            DetailItem("Team Size", startup!!.teamSize)
                            DetailItem("Description", startup!!.description)
                            if (!startup!!.pitchDeckUrl.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Pitch Deck", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                                Text(
                                    text = "View Pitch Deck",
                                    color = AtmiyaSecondary,
                                    modifier = Modifier.clickable { /* Open URL */ }
                                )
                            }
                        } else {
                            Text("No startup profile found.", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                        }
                    }
                    "investor" -> {
                        if (investor != null) {
                            DetailItem("Firm Name", investor!!.firmName)
                            DetailItem("Ticket Size", investor!!.ticketSizeMin)
                            DetailItem("Bio", investor!!.bio)
                        } else {
                            Text("No investor profile found.", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                        }
                    }
                    "mentor" -> {
                        if (mentor != null) {
                            DetailItem("Expertise", mentor!!.expertiseAreas.joinToString(", "))
                            DetailItem("Experience", "${mentor!!.experienceYears} Years")
                            DetailItem("Bio", mentor!!.bio)
                        } else {
                            Text("No mentor profile found.", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Actions
                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Button(
                        onClick = {
                            scope.launch {
                                firestoreRepository.updateUserStatus(userId, isBlocked = !user!!.isBlocked)
                                user = user!!.copy(isBlocked = !user!!.isBlocked)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (user!!.isBlocked) Color.Green else MaterialTheme.colorScheme.error
                        ),
                        modifier = Modifier.weight(1f).height(50.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            if (user!!.isBlocked) Icons.Default.CheckCircle else Icons.Default.Block,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (user!!.isBlocked) "Unblock" else "Block")
                    }
                    
                    if (!user!!.isDeleted) {
                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    firestoreRepository.updateUserStatus(userId, isDeleted = true)
                                    onBack()
                                }
                            },
                            modifier = Modifier.weight(1f).height(50.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Delete")
                        }
                    } else {
                         OutlinedButton(
                            onClick = {
                                scope.launch {
                                    firestoreRepository.updateUserStatus(userId, isDeleted = false)
                                    user = user!!.copy(isDeleted = false)
                                }
                            },
                            modifier = Modifier.weight(1f).height(50.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Restore")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DetailItem(label: String, value: String, color: Color = MaterialTheme.colorScheme.onSurface) {
    if (value.isNotEmpty()) {
        Column(modifier = Modifier.padding(vertical = 4.dp)) {
            Text(text = label, style = MaterialTheme.typography.labelMedium, color = Color.Gray)
            Text(text = value, style = MaterialTheme.typography.bodyLarge)
        }
    }
}
