package com.atmiya.innovation.ui.dashboard

import android.widget.Toast
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.atmiya.innovation.R
import com.atmiya.innovation.repository.FirestoreRepository
import com.atmiya.innovation.repository.StorageRepository
import com.atmiya.innovation.ui.theme.AtmiyaPrimary
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import com.atmiya.innovation.ui.components.SoftScaffold

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onLogout: () -> Unit,
    onEditProfile: () -> Unit,
    onBack: () -> Unit // Added callback
) {
    val auth = FirebaseAuth.getInstance()
    val user = auth.currentUser
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    val firestoreRepository = remember { FirestoreRepository() }
    val storageRepository = remember { StorageRepository() }
    
    var profileImageUrl by remember { mutableStateOf<String?>(null) }
    var userName by remember { mutableStateOf("") }
    var userRole by remember { mutableStateOf("") }
    
    // Fetch current profile
    LaunchedEffect(user) {
        if (user != null) {
            val userProfile = firestoreRepository.getUser(user.uid)
            if (userProfile != null) {
                profileImageUrl = userProfile.profilePhotoUrl
                userName = userProfile.name
                
                if (userProfile.role == "startup") {
                    val startup = firestoreRepository.getStartup(user.uid)
                    val type = startup?.startupType?.uppercase() ?: ""
                    userRole = "Startup ($type)"
                } else {
                    userRole = userProfile.role.replaceFirstChar { it.uppercase() }
                }
            }
        }
    }

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
                        // Update Firestore
                        firestoreRepository.updateUser(user.uid, mapOf("profilePhotoUrl" to url))
                        Toast.makeText(context, "Profile Photo Updated", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        Toast.makeText(context, "Upload Failed: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        } else {
            val exception = result.error
            if (exception != null) {
                Toast.makeText(context, "Crop failed: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val imagePickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        if (uri != null) {
            val cropOptions = com.canhub.cropper.CropImageOptions().apply {
                imageSourceIncludeGallery = true
                imageSourceIncludeCamera = false // Disable camera to prevent crash if FileProvider issue persists or permissions missing
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

    com.atmiya.innovation.ui.components.SoftScaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Profile") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp), // Apply horizontal padding for content
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            
            // Profile Pic
            Box(contentAlignment = Alignment.BottomEnd) {
                Surface(
                    modifier = Modifier.size(100.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    if (profileImageUrl != null) {
                        AsyncImage(
                            model = profileImageUrl,
                            contentDescription = "Profile Picture",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = if (userName.isNotEmpty()) userName.take(1).uppercase() else "U",
                                style = MaterialTheme.typography.headlineLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                
                // Edit Icon
                SmallFloatingActionButton(
                    onClick = { imagePickerLauncher.launch("image/*") },
                    containerColor = AtmiyaPrimary,
                    contentColor = Color.White,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Filled.Edit, contentDescription = "Change Photo", modifier = Modifier.size(16.dp))
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = userName.ifEmpty { user?.phoneNumber ?: "User" },
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Text(
                text = userRole.ifEmpty { "User" },
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Actions
            ProfileOptionItem(icon = Icons.Filled.Edit, title = "Edit Profile") { 
                onEditProfile()
            }
            
            // Theme Toggle
            val themeManager = remember { com.atmiya.innovation.ui.theme.ThemeManager(context) }
            val themePreference by themeManager.themeFlow.collectAsState(initial = "system")
            
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Row(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Brightness4, contentDescription = null, tint = AtmiyaPrimary)
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(text = "Dark Mode", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                    }
                    Switch(
                        checked = themePreference == "dark",
                        onCheckedChange = { isChecked ->
                            scope.launch {
                                themeManager.setTheme(if (isChecked) "dark" else "light")
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = AtmiyaPrimary,
                            checkedTrackColor = AtmiyaPrimary.copy(alpha = 0.5f)
                        )
                    )
                }
            }

            ProfileOptionItem(icon = Icons.Filled.ExitToApp, title = "Logout") {
                onLogout()
            }
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
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
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
            Text(text = title, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}
