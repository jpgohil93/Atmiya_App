package com.atmiya.innovation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.atmiya.innovation.ui.theme.AtmiyaInnovationTheme
import com.google.firebase.auth.FirebaseAuth

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AtmiyaInnovationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val auth = FirebaseAuth.getInstance()
                    var currentScreen by remember { mutableStateOf("login") }
                    var selectedRole by remember { mutableStateOf("") }
                    var selectedStartupType by remember { mutableStateOf<String?>(null) }

                    // Simple navigation logic (Replace with NavController later)
                    if (auth.currentUser != null && currentScreen == "login") {
                        // Check DB for role, if none, go to role selection
                        // For now, force role selection for demo
                        currentScreen = "role_selection"
                    }

                    when (currentScreen) {
                        "login" -> {
                            com.atmiya.innovation.ui.auth.LoginScreen(
                                onLoginSuccess = {
                                    currentScreen = "role_selection"
                                }
                            )
                        }
                        "role_selection" -> {
                            com.atmiya.innovation.ui.auth.RoleSelectionScreen(
                                onRoleSelected = { role, startupType ->
                                    val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                                    val user = auth.currentUser
                                    if (user != null) {
                                        val userData = hashMapOf(
                                            "uid" to user.uid,
                                            "phone" to user.phoneNumber,
                                            "role" to role,
                                            "created_at" to com.google.firebase.Timestamp.now(),
                                            "is_onboarding_complete" to false
                                        )

                                        db.collection("users").document(user.uid)
                                            .set(userData)
                                            .addOnSuccessListener {
                                                if (role == "startup" && startupType != null) {
                                                    val startupData = hashMapOf(
                                                        "uid" to user.uid,
                                                        "type" to startupType
                                                    )
                                                    db.collection("startups").document(user.uid)
                                                        .set(startupData)
                                                        .addOnSuccessListener {
                                                            selectedRole = role
                                                            selectedStartupType = startupType
                                                            currentScreen = "onboarding"
                                                        }
                                                } else {
                                                    selectedRole = role
                                                    selectedStartupType = null
                                                    currentScreen = "onboarding"
                                                }
                                            }
                                            .addOnFailureListener { e ->
                                                android.widget.Toast.makeText(this@MainActivity, "Error saving role: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
                                            }
                                    }
                                }
                            )
                        }
                        "onboarding" -> {
                            com.atmiya.innovation.ui.onboarding.ChatOnboardingScreen(
                                role = selectedRole,
                                startupType = selectedStartupType,
                                onOnboardingComplete = {
                                    currentScreen = "dashboard"
                                }
                            )
                        }
                        "dashboard" -> {
                            val startDest = intent.getStringExtra("navigation_destination")
                            com.atmiya.innovation.ui.dashboard.DashboardScreen(
                                role = selectedRole,
                                startDestination = startDest
                            )
                            // Clear the extra so it doesn't re-trigger on rotation (simple hack)
                            intent.removeExtra("navigation_destination")
                        }
                    }
                }
            }
        }
    }
}
