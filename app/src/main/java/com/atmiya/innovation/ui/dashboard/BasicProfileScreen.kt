package com.atmiya.innovation.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.atmiya.innovation.data.User
import com.atmiya.innovation.repository.FirestoreRepository
import com.atmiya.innovation.ui.components.UserAvatar
import com.atmiya.innovation.ui.theme.AtmiyaPrimary
import com.atmiya.innovation.ui.theme.AtmiyaSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BasicProfileScreen(
    userId: String,
    onBack: () -> Unit
) {
    val repository = remember { FirestoreRepository() }
    var user by remember { mutableStateOf<User?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    
    LaunchedEffect(userId) {
        try {
            user = repository.getUser(userId)
        } catch (e: Exception) {
            android.util.Log.e("BasicProfile", "Error", e)
        } finally {
            isLoading = false
        }
    }

    Scaffold(
        containerColor = Color.White
    ) { padding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = AtmiyaPrimary)
            }
        } else if (user == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("User not found.")
                Button(onClick = onBack, modifier = Modifier.padding(top = 16.dp)) { Text("Go Back") }
            }
        } else {
            val u = user!!
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
            ) {
                // --- Header Section ---
                Box(contentAlignment = Alignment.BottomStart) {
                    // Banner
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(AtmiyaPrimary, AtmiyaSecondary)
                                )
                            )
                    )
                    
                    // Back Button
                    IconButton(
                        onClick = onBack, 
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(top = 16.dp, start = 16.dp)
                            .background(Color.Black.copy(alpha=0.2f), CircleShape)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }

                    // Profile Image
                    Surface(
                        shape = CircleShape,
                        color = Color.White,
                        shadowElevation = 4.dp,
                        modifier = Modifier
                            .padding(start = 24.dp)
                            .size(100.dp)
                            .offset(y = 50.dp) // Push down
                    ) {
                        UserAvatar(
                            model = u.profilePhotoUrl,
                            name = u.name,
                            modifier = Modifier.padding(4.dp),
                            size = null 
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(60.dp))
                
                // --- User Info ---
                Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                    Text(
                        text = u.name,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = u.role.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() },
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.Gray
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Location
                    if (u.city.isNotBlank() || u.region.isNotBlank()) {
                         Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                listOf(u.city, u.region).filter { it.isNotBlank() }.joinToString(", "),
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color.Gray
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    HorizontalDivider(color = Color.LightGray.copy(alpha=0.3f))
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // No private details (phone/email) for Admin/Basic view as per request
                    // "display the basic details only, no private details should be displayed"
                    
                }
            }
        }
    }
}
