package com.atmiya.innovation.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.atmiya.innovation.data.Mentor
import com.atmiya.innovation.repository.FirestoreRepository
import com.atmiya.innovation.ui.components.SoftScaffold
import com.atmiya.innovation.ui.theme.AtmiyaPrimary
import com.atmiya.innovation.ui.theme.AtmiyaSecondary
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MentorDetailScreen(
    mentorId: String,
    onBack: () -> Unit
) {
    val repository = remember { FirestoreRepository() }
    var mentor by remember { mutableStateOf<Mentor?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(mentorId) {
        try {
            mentor = repository.getMentor(mentorId)
        } catch (e: Exception) {
            android.util.Log.e("MentorDetailScreen", "Error fetching mentor", e)
        } finally {
            isLoading = false
        }
    }

    SoftScaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mentor Profile", fontWeight = FontWeight.Bold) },
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
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = AtmiyaPrimary)
            }
        } else if (mentor == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Mentor not found.")
            }
        } else {
            val m = mentor!!
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Profile Photo
                if (m.profilePhotoUrl != null) {
                    AsyncImage(
                        model = m.profilePhotoUrl,
                        contentDescription = null,
                        modifier = Modifier.size(120.dp).clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier.size(120.dp).clip(CircleShape).background(AtmiyaSecondary.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(m.name.take(1), style = MaterialTheme.typography.displayMedium, color = AtmiyaPrimary)
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(m.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(m.title, style = MaterialTheme.typography.titleMedium, color = AtmiyaPrimary)
                Text(m.organization, style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        MentorSectionHeader("Expertise")
                        Text(m.expertiseAreas.joinToString(", "))
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        MentorSectionHeader("Experience")
                        Text("${m.experienceYears} Years")
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        MentorSectionHeader("Bio")
                        Text(m.bio)
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Button(
                    onClick = { /* TODO: Request Mentorship */ },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = AtmiyaSecondary)
                ) {
                    Text("Request Mentorship")
                }
            }
        }
    }
}

@Composable
private fun MentorSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = Color.Gray,
        modifier = Modifier.padding(bottom = 4.dp)
    )
}
