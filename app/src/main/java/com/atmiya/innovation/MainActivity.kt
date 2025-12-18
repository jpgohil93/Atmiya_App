package com.atmiya.innovation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.atmiya.innovation.repository.FirestoreRepository
import com.atmiya.innovation.ui.theme.AtmiyaInnovationTheme
import com.google.firebase.auth.FirebaseAuth
import android.content.Intent
import android.net.Uri
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleNavigationIntent(intent)
    }

    // Mutable state to hold navigation destination from intent
    private val navigationState = mutableStateOf<Pair<String?, String?>>(null to null)

    private fun handleNavigationIntent(intent: Intent?) {
        if (intent == null) return
        
        var startDest = intent.getStringExtra("navigation_destination")
        var navId = intent.getStringExtra("navigation_id")

        // Handle Deep Link URI if extras are missing
        if (startDest == null && intent.data != null) {
            val uri = intent.data
            if (uri != null) {
                // Scenario 1: Direct Deep Link (https://netfund.app/wall_post/123)
                if (uri.pathSegments.size >= 2 && uri.pathSegments[0] == "wall_post") {
                    startDest = "wall_post"
                    navId = uri.pathSegments[1]
                }
                
                // Scenario 2: Play Store Referrer (https://play.google.com/store/apps/details?id=...&referrer=wall_post/123)
                // When FDL opens the app, it usually passes the 'link' parameter as the data.
                // If the 'link' param was the Play Store URL, we need to extract 'referrer'.
                if (uri.host == "play.google.com" || (uri.getQueryParameter("link")?.contains("play.google.com") == true)) {
                    // Check if 'referrer' exists directly
                    val referrer = uri.getQueryParameter("referrer") ?: Uri.parse(uri.getQueryParameter("link") ?: "").getQueryParameter("referrer")
                    
                    if (referrer != null && referrer.startsWith("wall_post/")) {
                         val parts = referrer.split("/")
                         if (parts.size >= 2) {
                             startDest = "wall_post"
                             navId = parts[1]
                         }
                    }
                }
            }
        }
        
        if (startDest != null) {
            android.util.Log.d("MainActivity", "Handling Intent: dest=$startDest id=$navId")
            navigationState.value = startDest to navId
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        
        // Handle initial intent
        handleNavigationIntent(intent)
        
        val themeManager = com.atmiya.innovation.ui.theme.ThemeManager(this)
        
        // Notification Permission Launcher (Android 13+)
        val requestPermissionLauncher = registerForActivityResult(
            androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                // Permission granted
                com.google.firebase.messaging.FirebaseMessaging.getInstance().subscribeToTopic("all_posts")
            } else {
                // Permission denied
            }
        }

        // Ask for permission on Valid Session
        fun askNotificationPermission() {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                if (androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) !=
                    android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                } else {
                     com.google.firebase.messaging.FirebaseMessaging.getInstance().subscribeToTopic("all_posts")
                }
            } else {
                 com.google.firebase.messaging.FirebaseMessaging.getInstance().subscribeToTopic("all_posts")
            }
        }
        
        setContent {
            val themePreference by themeManager.themeFlow.collectAsState(initial = "system")
            val isDarkTheme = when (themePreference) {
                "light" -> false
                "dark" -> true
                else -> androidx.compose.foundation.isSystemInDarkTheme()
            }

            AtmiyaInnovationTheme(darkTheme = isDarkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val auth = FirebaseAuth.getInstance()
                    var currentScreen by remember { mutableStateOf("splash") }
                    var selectedRole by remember { mutableStateOf("") }
                    
                    // Repository instance (Manual dependency injection for now)
                    val firestoreRepository = remember { FirestoreRepository() }
                    
                    // Force Update Logic
                    var showForceUpdate by remember { mutableStateOf(false) }
                    var appConfig by remember { mutableStateOf(com.atmiya.innovation.data.AppConfig()) }

                    LaunchedEffect(Unit) {
                        try {
                            val config = firestoreRepository.getAppConfig()
                            appConfig = config
                            val currentVersion = com.atmiya.innovation.BuildConfig.VERSION_CODE
                            
                            
                            android.util.Log.d("ForceUpdate", "Status: Checked. Current=$currentVersion, Min=${config.minVersionCode}")

                            if (currentVersion < config.minVersionCode) {
                                showForceUpdate = true
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("ForceUpdate", "Error", e)
                        }
                    }

                    if (showForceUpdate) {
                         com.atmiya.innovation.ui.components.ForceUpdateScreen(appConfig)
                    } else {
                        // Helper to check session and route
                        fun checkSessionAndNavigate() {
                            val uid = auth.currentUser?.uid
                            if (uid != null) {
                                lifecycleScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                                    try {
                                        val start = System.currentTimeMillis()
                                        var user = firestoreRepository.getUser(uid)
                                        
                                        // Auto-Claim Logic for Bulk Users (If Auth UID != Bulk UUID)
                                        if (user == null) {
                                            var phone = auth.currentUser?.phoneNumber
                                            val email = auth.currentUser?.email
                                            
                                            // Fallback: Extract phone from synthetic email if phone property is null
                                            if (phone == null && email != null && email.endsWith("@atmiya.com")) {
                                                phone = email.removeSuffix("@atmiya.com")
                                                android.util.Log.d("SessionCheck", "Extracted phone from email: $phone")
                                            }
                                            
                                            if (phone != null) {
                                                android.util.Log.d("SessionCheck", "User null. Checking for bulk invite for phone $phone")
                                                // Try formats: With and without +91 and raw
                                                // The repo expects the document ID to match exactly what was stored.
                                                // We try raw phone first.
                                                // If phone starts with +91, try removing it.
                                                // If phone doesn't start with +91, try adding it.
                                                
                                                var bulkUid = firestoreRepository.getBulkInviteUid(phone)
                                                
                                                if (bulkUid == null) {
                                                     if (phone.startsWith("+91")) {
                                                         bulkUid = firestoreRepository.getBulkInviteUid(phone.substring(3))
                                                     } else {
                                                         bulkUid = firestoreRepository.getBulkInviteUid("+91$phone")
                                                     }
                                                }
                                                
                                                if (bulkUid != null) {
                                                    android.util.Log.d("SessionCheck", "Found bulk invite UID: $bulkUid. Linking to $uid")
                                                    try {
                                                        firestoreRepository.linkBulkUserToAuth(bulkUid, uid)
                                                        user = firestoreRepository.getUser(uid) // Refetch new user
                                                        android.util.Log.d("SessionCheck", "Link success. Refetched user: ${user?.uid}")
                                                    } catch (e: Exception) {
                                                        android.util.Log.e("SessionCheck", "Failed to link bulk user", e)
                                                    }
                                                } else {
                                                    android.util.Log.d("SessionCheck", "No bulk invite found for phone $phone")
                                                }
                                            } else {
                                                android.util.Log.d("SessionCheck", "Phone is null, cannot do lookup.")
                                            }
                                        }

                                        android.util.Log.d("Perf", "SessionCheck: getUser took ${System.currentTimeMillis() - start} ms")
                                        
                                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                            if (user != null && user.isBlocked) {
                                                android.util.Log.w("SessionCheck", "User ${user.uid} is blocked. Signing out.")
                                                auth.signOut()
                                                android.widget.Toast.makeText(this@MainActivity, "Your account has been blocked by Admin.", android.widget.Toast.LENGTH_LONG).show()
                                                currentScreen = "login"
                                                return@withContext
                                            }
    
                                            // DEBUG ONLY: Auto-promote specific user to Admin for testing
                                            val phoneNumber = auth.currentUser?.phoneNumber
                                            if (com.atmiya.innovation.BuildConfig.DEBUG && user != null && phoneNumber?.endsWith("9999999999") == true) {
                                                if (user.role != "admin") {
                                                    android.util.Log.d("DebugAdmin", "Promoting user ${user.uid} to admin for testing")
                                                    // Update Firestore (Launch in IO scope)
                                                    lifecycleScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                                                        firestoreRepository.updateUser(user.uid, mapOf("role" to "admin"))
                                                    }
                                                    // Update local state
                                                    selectedRole = "admin"
                                                    currentScreen = "dashboard"
                                                } else {
                                                    android.util.Log.d("SessionCheck", "Navigating to Dashboard. Role: ${user.role}")
                                                    selectedRole = user.role
                                                    currentScreen = "dashboard"
                                                }
                                                selectedRole = user.role
                                                currentScreen = "dashboard"
                                            } else {
                                                // Check for Pending Role Details (Bulk Users - not yet onboarded)
                                                android.util.Log.d("SessionCheck", "User check: uid=$uid, role=${user?.role}, hasCompletedRoleDetails=${user?.hasCompletedRoleDetails}")
                                                
                                                if (user == null) {
                                                    // User not found - Auto-claim handled above. If still null, Signup.
                                                    android.util.Log.d("SessionCheck", "User is null => Signup")
                                                    currentScreen = "signup"
                                                } else {
                                                    // User Exists. Determine destination.
                                                    
                                                    // 1. Check if Role is missing (New User)
                                                    if (user.role.isEmpty()) {
                                                        android.util.Log.d("SessionCheck", "No Role => Signup")
                                                        currentScreen = "signup"
                                                    } 
                                                    // 2. Check if Startup needs to complete details
                                                    // Applies to BOTH Bulk users and normal Startups who haven't finished this step.
                                                    else if (user.role == "startup" && !user.hasCompletedRoleDetails) {
                                                        android.util.Log.d("SessionCheck", "Startup incomplete details => Pending Details")
                                                        currentScreen = "pending_details"
                                                    }
                                                    // 3. Fallback for incomplete onboarding if we want to catch general cases?
                                                    // But effectively, if they have a role, they go to dashboard unless trapped above.
                                                    else {
                                                        android.util.Log.d("SessionCheck", "Role ${user.role} ready => Dashboard")
                                                        selectedRole = user.role
                                                        currentScreen = "dashboard"
                                                    }
                                                }
                                            }
                                            
                                            // Ask for permission after successful login check
                                            askNotificationPermission()
                                            
                                            // Update FCM Token for notifications
                                            lifecycleScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                                                user?.let { firestoreRepository.updateFcmToken(it.uid) }
                                            }
                                        }
                                    } catch (e: Exception) {
                                        android.util.Log.e("SessionCheck", "Error: ${e.message}", e)
                                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                            currentScreen = "login"
                                        }
                                    }
                                }
                            } else {
                                currentScreen = "login"
                            }
                        }
    
    
                        when (currentScreen) {
                            "splash" -> {
                                com.atmiya.innovation.ui.auth.SplashScreen(
                                    onSessionValid = { checkSessionAndNavigate() },
                                    onSessionInvalid = { currentScreen = "login" }
                                )
                            }
                        "login" -> {
                            com.atmiya.innovation.ui.auth.LoginScreen(
                                onLoginSuccess = { checkSessionAndNavigate() },
                                onSignupClick = { currentScreen = "signup" }
                            )
                        }
                        "signup" -> {
                            com.atmiya.innovation.ui.onboarding.SignupScreen(
                                onSignupComplete = { role ->
                                    selectedRole = role
                                    currentScreen = "dashboard"
                                    askNotificationPermission()
                                },
                                onLogout = {
                                    auth.signOut()
                                    currentScreen = "login"
                                }
                            )
                        }
                        "pending_details" -> {
                            com.atmiya.innovation.ui.onboarding.PendingStartupDetailsScreen(
                                onComplete = {
                                    selectedRole = "startup"
                                    currentScreen = "dashboard"
                                },
                                onLogout = {
                                    auth.signOut()
                                    currentScreen = "login"
                                }
                            )
                        }
                        "dashboard" -> {
                            val (navStateDest, navStateId) = navigationState.value
                            
                            // Subscribe to topics if Startup
                            LaunchedEffect(selectedRole) {
                                if (selectedRole == "startup") {
                                    com.google.firebase.messaging.FirebaseMessaging.getInstance().subscribeToTopic("startups")
                                    val user = firestoreRepository.getUser(auth.currentUser?.uid ?: "")
                                    if (user != null && user.startupCategory.isNotEmpty()) {
                                        val topic = "startup_category_${user.startupCategory.replace(" ", "_")}"
                                        com.google.firebase.messaging.FirebaseMessaging.getInstance().subscribeToTopic(topic)
                                        android.util.Log.d("FCM", "Subscribed to topic: $topic")
                                    }
                                }
                                // Ensure global topic subscription here too just in case
                                com.google.firebase.messaging.FirebaseMessaging.getInstance().subscribeToTopic("all_posts")
                            }

                            // Pass navigation state to Dashboard
                            // And clear it once consumed is handled inside Dashboard or we just pass it reactively
                            // For simplicity, we pass state directly. 
                            // DashboardScreen needs to react to changes.
                            // To ensure we don't re-navigate on config changes if we already did, correct way is to consume it.
                            // But since we use rememberSaveable or similar inside Dashboard for nav controller, it might be tricky.
                            // Better approach: Pass the values, and let Dashboard `LaunchedEffect` handle it.
                            // We need to clear it so it doesn't trigger again?
                            // Actually, onNewIntent updates the state, triggering recomposition.
                            // Dashboard LaunchedEffect(navStateDest, navStateId) will run.
                            
                            // To prevent loop/double nav, we can pass a "consume" callback?
                            // Or just let the state sit there. LaunchedEffect keys will handle only changes.

                            com.atmiya.innovation.ui.dashboard.DashboardScreen(
                                role = selectedRole,
                                startDestination = navStateDest,
                                startId = navStateId,
                                onLogout = {
                                    auth.signOut()
                                    currentScreen = "login"
                                }
                            )
                            
                            // Reset navigation state after passing it to Dashboard is risky if Dashboard doesn't consume it immediately.
                            // Instead, we trust DashboardScreen's LaunchedEffect to react to changes in [startDestination, startId].
                        }
                    }
                }
                }
            }
        }
    }
}

