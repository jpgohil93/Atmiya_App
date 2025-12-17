package com.atmiya.innovation.ui.settings

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.atmiya.innovation.BuildConfig
import com.atmiya.innovation.data.Feedback
import com.atmiya.innovation.repository.FirestoreRepository
import com.atmiya.innovation.ui.components.SoftScaffold
import com.atmiya.innovation.ui.theme.AtmiyaPrimary
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import compose.icons.TablerIcons
import compose.icons.tablericons.*
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val auth = FirebaseAuth.getInstance()
    val currentUser = auth.currentUser
    val firestoreRepository = remember { FirestoreRepository() }
    val viewModel: SettingsViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    val currentTheme by viewModel.theme.collectAsState()

    // State
    var userName by remember { mutableStateOf("User") }
    var userPhone by remember { mutableStateOf("") }
    var showFeedbackDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var feedbackText by remember { mutableStateOf("") }
    var isSubmittingFeedback by remember { mutableStateOf(false) }

    // Change Password State
    var showPasswordDialog by remember { mutableStateOf(false) }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var isChangingPassword by remember { mutableStateOf(false) }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var isConfirmPasswordVisible by remember { mutableStateOf(false) }


    
    // Notification Permission State
    var isNotificationAllowed by remember { mutableStateOf(false) }

    // Check Notification Permission
    fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            isNotificationAllowed = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            isNotificationAllowed = true // Roughly true for older versions unless blocked in settings
        }
    }

    // Helper to format phone number
    fun formatPhoneNumber(phone: String): String {
        if (phone.contains("@")) return phone // It's an email
        val digits = phone.filter { it.isDigit() }
        return when {
            digits.length == 10 -> "+91 ${digits.substring(0, 5)} ${digits.substring(5)}"
            digits.length == 12 && digits.startsWith("91") -> "+91 ${digits.substring(2, 7)} ${digits.substring(7)}"
            else -> phone
        }
    }

    LaunchedEffect(Unit) {
        checkNotificationPermission()
        if (currentUser != null) {
            val user = firestoreRepository.getUser(currentUser.uid)
            if (user != null) {
                userName = user.name
                userPhone = user.phoneNumber.ifEmpty { user.email }
            }
        }
    }
    
    // Resume check (in case user comes back from settings)
    val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                checkNotificationPermission()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    SoftScaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.SemiBold, fontSize = 20.sp) },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onSurface)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            
            // --- Appearance Section ---
            SettingsSection(title = "Appearance") {
                SettingsCard(
                    icon = TablerIcons.Palette,
                    title = "Theme",
                    subtitle = when(currentTheme) {
                        "light" -> "Light"
                        "dark" -> "Dark"
                        else -> "System Default"
                    },
                    onClick = { showThemeDialog = true }
                )
            }

            // --- Your Account Section ---
            SettingsSection(title = "Your account") {
                // Account Card
                SettingsCard(
                    icon = TablerIcons.Id,
                    title = "Account",
                    subtitle = if (userPhone.isNotEmpty()) formatPhoneNumber(userPhone) else "Loading...",
                    onClick = { /* No action requested */ }
                )
                
                Spacer(modifier = Modifier.height(12.dp))

                // Change Password
                SettingsCard(
                    icon = TablerIcons.Lock,
                    title = "Change Password",
                    subtitle = "Update your security",
                    onClick = { 
                        newPassword = ""
                        confirmPassword = ""
                        passwordError = null
                        isPasswordVisible = false
                        isConfirmPasswordVisible = false
                        showPasswordDialog = true 
                    }
                )
                

                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Notifications Card
                SettingsCard(
                    icon = TablerIcons.Bell,
                    title = "Notifications",
                    subtitle = if (isNotificationAllowed) "Allowed" else "Not Allowed",
                    onClick = {
                        // Open App Notification Settings
                        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                        }
                        context.startActivity(intent)
                    }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Language Card (Mock)
                SettingsCard(
                    icon = TablerIcons.Language,
                    title = "Language",
                    subtitle = "English",
                    showChevron = false, // Use specific UI for language if needed, else standard
                    trailingContent = {
                        Icon(TablerIcons.CaretDown, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(20.dp))
                    },
                    onClick = { Toast.makeText(context, "Only English supported currently", Toast.LENGTH_SHORT).show() }
                )
            }

            // --- About Us Section ---
            SettingsSection(title = "About us") {
                // Rate Us
                SettingsCard(
                    icon = TablerIcons.ThumbUp,
                    title = "Rate us",
                    subtitle = "Write about us in your store",
                    onClick = {
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=com.atmiya.innovation"))
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "Could not open Play Store", Toast.LENGTH_SHORT).show()
                        }
                    }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Feedback
                SettingsCard(
                    icon = TablerIcons.Message,
                    title = "Feedback",
                    subtitle = "Write your opinion for us",
                    onClick = { showFeedbackDialog = true }
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // --- Logout ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onLogout() }
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Log out",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.error, // Red
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(20.dp)
                )
            }

            // --- Footer Version ---
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                 Text(
                    text = "netfund © 2025 v${BuildConfig.VERSION_NAME}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha=0.6f)
                )
            }
            
            Spacer(modifier = Modifier.height(30.dp))
        }
    }
    
    // --- Feedback Dialog ---
    if (showFeedbackDialog) {
        AlertDialog(
            onDismissRequest = { if (!isSubmittingFeedback) showFeedbackDialog = false },
            title = { Text("Send Feedback") },
            text = {
                Column {
                    Text("We'd love to hear your thoughts!", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = feedbackText,
                        onValueChange = { feedbackText = it },
                        label = { Text("Your opinion") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        maxLines = 5
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (feedbackText.isNotBlank() && currentUser != null) {
                            scope.launch {
                                isSubmittingFeedback = true
                                try {
                                    val feedback = Feedback(
                                        id = UUID.randomUUID().toString(),
                                        userId = currentUser.uid,
                                        userName = userName,
                                        userPhone = userPhone,
                                        message = feedbackText,
                                        createdAt = Timestamp.now()
                                    )
                                    firestoreRepository.addFeedback(feedback)
                                    Toast.makeText(context, "Thank you for your feedback!", Toast.LENGTH_SHORT).show()
                                    showFeedbackDialog = false
                                    feedbackText = ""
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Failed to send feedback. Please try again later.", Toast.LENGTH_SHORT).show()
                                } finally {
                                    isSubmittingFeedback = false
                                }
                            }
                        }
                    },
                    enabled = !isSubmittingFeedback,
                    colors = ButtonDefaults.buttonColors(containerColor = AtmiyaPrimary)
                ) {
                    if (isSubmittingFeedback) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White)
                    } else {
                        Text("Send")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showFeedbackDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // --- Theme Dialog ---
    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { Text("Choose Theme") },
            text = {
                Column {
                    val options = listOf("system" to "System Default", "light" to "Light", "dark" to "Dark")
                    options.forEach { (key, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.setTheme(key)
                                    showThemeDialog = false
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = currentTheme == key,
                                onClick = {
                                    viewModel.setTheme(key)
                                    showThemeDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = label, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showThemeDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showPasswordDialog) {
        AlertDialog(
            onDismissRequest = { if (!isChangingPassword) showPasswordDialog = false },
            title = { Text("Change Password") },
            text = {
                Column {
                    Text("Enter your new password below.", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    OutlinedTextField(
                        value = newPassword,
                        onValueChange = { newPassword = it; passwordError = null },
                        label = { Text("New Password") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        visualTransformation = if (isPasswordVisible) androidx.compose.ui.text.input.VisualTransformation.None else androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        trailingIcon = {
                            val image = if (isPasswordVisible) TablerIcons.Eye else TablerIcons.EyeOff
                            val description = if (isPasswordVisible) "Hide password" else "Show password"
                            IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                Icon(imageVector = image, contentDescription = description)
                            }
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it; passwordError = null },
                        label = { Text("Confirm Password") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        visualTransformation = if (isConfirmPasswordVisible) androidx.compose.ui.text.input.VisualTransformation.None else androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        trailingIcon = {
                            val image = if (isConfirmPasswordVisible) TablerIcons.Eye else TablerIcons.EyeOff
                            val description = if (isConfirmPasswordVisible) "Hide password" else "Show password"
                            IconButton(onClick = { isConfirmPasswordVisible = !isConfirmPasswordVisible }) {
                                Icon(imageVector = image, contentDescription = description)
                            }
                        }
                    )
                    
                    if (passwordError != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = passwordError!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newPassword.length < 6) {
                            passwordError = "Password must be at least 6 characters"
                            return@Button
                        }
                        if (newPassword != confirmPassword) {
                            passwordError = "Passwords do not match"
                            return@Button
                        }
                        
                        isChangingPassword = true
                        currentUser?.updatePassword(newPassword)
                            ?.addOnCompleteListener { task ->
                                isChangingPassword = false
                                if (task.isSuccessful) {
                                    Toast.makeText(context, "Password updated successfully", Toast.LENGTH_SHORT).show()
                                    showPasswordDialog = false
                                    newPassword = ""
                                    confirmPassword = ""
                                    isPasswordVisible = false
                                    isConfirmPasswordVisible = false
                                } else {
                                    val error = task.exception?.message ?: "Failed to update password"
                                    if (error.contains("recent authentication", ignoreCase = true)) {
                                        Toast.makeText(context, "Please log out and log in again to change password.", Toast.LENGTH_LONG).show()
                                        showPasswordDialog = false
                                    } else {
                                        passwordError = error
                                    }
                                }
                            }
                    },
                    enabled = !isChangingPassword,
                    colors = ButtonDefaults.buttonColors(containerColor = AtmiyaPrimary)
                ) {
                    if (isChangingPassword) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White)
                    } else {
                        Text("Update")
                    }
                }
            },
            dismissButton = {
                if (!isChangingPassword) {
                     TextButton(onClick = { showPasswordDialog = false }) {
                        Text("Cancel")
                    }
                }
            }
        )
    }
}

@Composable
fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface, // Dark Gray
            modifier = Modifier.padding(bottom = 12.dp, start = 4.dp)
        )
        content()
    }
}

@Composable
fun SettingsCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    showChevron: Boolean = false, // Defaults to false as per screenshot cards usually imply action by tap, but visual reference didn't strictly show arrows everywhere, "Language" had one.
    trailingContent: @Composable (() -> Unit)? = null
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(24.dp), // Highly rounded as per screenshot
        color = MaterialTheme.colorScheme.surface, // Pure White in Light, Dark in Dark
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon in a circle? Screenshot shows simple icon on left, maybe specific background?
            // Screenshot: Icon seems to have a darker gray circle bg.
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest), // Contrast for icon
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant, // Dark Gray Icon
                    modifier = Modifier.size(20.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
            
            if (trailingContent != null) {
                trailingContent()
            } else if (showChevron) {
                 // Nothing as default is false
            }
        }
    }
}
