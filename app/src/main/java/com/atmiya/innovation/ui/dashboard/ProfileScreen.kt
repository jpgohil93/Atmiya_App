package com.atmiya.innovation.ui.dashboard

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.atmiya.innovation.R
import com.atmiya.innovation.ui.theme.AtmiyaPrimary

@Composable
fun ProfileScreen() {
    val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
    val user = auth.currentUser

    val context = androidx.compose.ui.platform.LocalContext.current
    val storage = com.google.firebase.storage.FirebaseStorage.getInstance()
    val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
    
    var profileImageUrl by remember { mutableStateOf<String?>(null) }
    
    // Fetch current profile image
    LaunchedEffect(user) {
        if (user != null) {
            db.collection("users").document(user.uid).get()
                .addOnSuccessListener { document ->
                    profileImageUrl = document.getString("profileImageUrl")
                }
        }
    }

    val launcher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        if (uri != null && user != null) {
            val ref = storage.reference.child("profile_images/${user.uid}")
            ref.putFile(uri)
                .addOnSuccessListener {
                    ref.downloadUrl.addOnSuccessListener { downloadUri ->
                        val url = downloadUri.toString()
                        profileImageUrl = url
                        // Update Firestore
                        db.collection("users").document(user.uid)
                            .update("profileImageUrl", url)
                    }
                }
                .addOnFailureListener {
                    android.widget.Toast.makeText(context, "Upload Failed", android.widget.Toast.LENGTH_SHORT).show()
                }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(32.dp))
        
        // Profile Pic
        Box(contentAlignment = Alignment.BottomEnd) {
            Surface(
                modifier = Modifier.size(100.dp),
                shape = CircleShape,
                color = Color.LightGray
            ) {
                if (profileImageUrl != null) {
                    coil.compose.AsyncImage(
                        model = profileImageUrl,
                        contentDescription = "Profile Picture",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = user?.phoneNumber?.takeLast(2) ?: "U",
                            style = MaterialTheme.typography.headlineLarge,
                            color = Color.White
                        )
                    }
                }
            }
            
            // Edit Icon
            SmallFloatingActionButton(
                onClick = { launcher.launch("image/*") },
                containerColor = AtmiyaPrimary,
                contentColor = Color.White,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(Icons.Filled.Edit, contentDescription = "Change Photo", modifier = Modifier.size(16.dp))
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = user?.phoneNumber ?: "User",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        
        Text(
            text = "Startup (EDP)", // TODO: Fetch from DB
            style = MaterialTheme.typography.bodyLarge,
            color = Color.Gray
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Actions
        ProfileOptionItem(icon = Icons.Filled.Edit, title = "Edit Profile") { }
        ProfileOptionItem(icon = Icons.Filled.ExitToApp, title = "Logout") {
            auth.signOut()
            // TODO: Navigate back to Login (Need to handle in MainActivity)
        }
    }
}

@Composable
fun ProfileOptionItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = AtmiyaPrimary)
            Spacer(modifier = Modifier.width(16.dp))
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
        }
    }
}
