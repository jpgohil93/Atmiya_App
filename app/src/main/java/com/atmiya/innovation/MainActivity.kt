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
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
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

                    // Helper to check session and route
                    fun checkSessionAndNavigate() {
                        val uid = auth.currentUser?.uid
                        if (uid != null) {
                            lifecycleScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                                try {
                                    val start = System.currentTimeMillis()
                                    val user = firestoreRepository.getUser(uid)
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
                                        } else if (user != null && user.isOnboardingComplete && user.role.isNotEmpty()) {
                                            android.util.Log.d("SessionCheck", "Navigating to Dashboard. Role: ${user.role}")
                                            selectedRole = user.role
                                            currentScreen = "dashboard"
                                        } else {
                                            android.util.Log.d("SessionCheck", "Navigating to Signup. User null? ${user == null}, Onboarding? ${user?.isOnboardingComplete}, Role? ${user?.role}")
                                            currentScreen = "signup"
                                        }
                                        
                                        // Ask for permission after successful login check
                                        askNotificationPermission()
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
                        "dashboard" -> {
                            var startDest = intent.getStringExtra("navigation_destination")
                            var navId = intent.getStringExtra("navigation_id")

                            // Handle Deep Link URI if extras are missing
                            if (startDest == null && intent.data != null) {
                                val uri = intent.data
                                if (uri != null && uri.pathSegments.size >= 2) {
                                    // Path: /wall_post/{id}
                                    if (uri.pathSegments[0] == "wall_post") {
                                        startDest = "wall_post"
                                        navId = uri.pathSegments[1]
                                    }
                                }
                            }
                            
                            // Subscribe to topics if Startup
                            LaunchedEffect(selectedRole) {
                                if (selectedRole == "startup") {
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

                            com.atmiya.innovation.ui.dashboard.DashboardScreen(
                                role = selectedRole,
                                startDestination = startDest,
                                startId = navId,
                                onLogout = {
                                    auth.signOut()
                                    currentScreen = "login"
                                }
                            )
                            intent.removeExtra("navigation_destination")
                            intent.removeExtra("navigation_id")
                        }
                    }
                }
            }
        }
    }
}
