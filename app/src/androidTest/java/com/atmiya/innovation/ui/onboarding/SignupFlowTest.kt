package com.atmiya.innovation.ui.onboarding

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Rule
import org.junit.Test

class SignupFlowTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun signupFlow_validation_check() {
        // Start the app with SignupScreen
        composeTestRule.setContent {
            SignupScreen(onSignupComplete = {})
        }

        // Step 0: Profile Basics
        // Verify "Next" is present but clicking it shows errors (empty fields)
        composeTestRule.onNodeWithText("Next").performClick()
        composeTestRule.onNodeWithText("Required").assertExists() // Check for error message

        // Fill Basic Info
        composeTestRule.onNodeWithText("Full Name").performTextInput("Test User")
        composeTestRule.onNodeWithText("Email Address").performTextInput("test@example.com")
        composeTestRule.onNodeWithText("City").performTextInput("Rajkot")
        // Note: Dropdowns are harder to test in isolation without specific tags, skipping for basic flow
        composeTestRule.onNodeWithText("Organization / College Name").performTextInput("Atmiya Uni")
        
        // Mock State selection if possible or assume validation might fail on dropdown
        // For this test, we just verify the UI elements exist and accept input
    }

    @Test
    fun roleSelection_displays_correct_options() {
        composeTestRule.setContent {
            RoleTrackStep(selectedRole = null, selectedTrack = null, onSelect = { _, _ -> })
        }

        composeTestRule.onNodeWithText("Startup").assertExists()
        composeTestRule.onNodeWithText("Investor").assertExists()
        composeTestRule.onNodeWithText("Mentor").assertExists()
    }

    @Test
    fun startup_track_selection_shows_chips() {
        composeTestRule.setContent {
            RoleTrackStep(selectedRole = "startup", selectedTrack = null, onSelect = { _, _ -> })
        }

        composeTestRule.onNodeWithText("EDP (Idea)").assertExists()
        composeTestRule.onNodeWithText("Accelerator (Growth)").assertExists()
    }
}
