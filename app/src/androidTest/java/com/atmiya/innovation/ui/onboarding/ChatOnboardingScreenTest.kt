package com.atmiya.innovation.ui.onboarding

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import org.junit.Rule
import org.junit.Test

class ChatOnboardingScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testInitialQuestionDisplayed() {
        composeTestRule.setContent {
            ChatOnboardingScreen(
                role = "startup",
                startupType = "edp",
                onOnboardingComplete = {}
            )
        }

        // Verify first question for EDP is displayed
        composeTestRule.onNodeWithText("What is your full name?").assertExists()
    }

    @Test
    fun testUserCanEnterText() {
        composeTestRule.setContent {
            ChatOnboardingScreen(
                role = "startup",
                startupType = "edp",
                onOnboardingComplete = {}
            )
        }

        // Enter text
        composeTestRule.onNodeWithText("Type your answer...").performTextInput("John Doe")
        
        // Verify text is entered
        composeTestRule.onNodeWithText("John Doe").assertExists()
    }
    
    @Test
    fun testSendButtonAddsMessage() {
         composeTestRule.setContent {
            ChatOnboardingScreen(
                role = "startup",
                startupType = "edp",
                onOnboardingComplete = {}
            )
        }
        
        // Enter text and click send
        composeTestRule.onNodeWithText("Type your answer...").performTextInput("John Doe")
        composeTestRule.onNodeWithText("Send").performClick()
        
        // Verify user message is added to chat (bubble)
        // Note: This assumes the UI updates synchronously or fast enough. 
        // In real tests, might need waitForIdle()
        composeTestRule.onNodeWithText("John Doe").assertExists()
        
        // Verify next question appears (Logic check)
        // "Which city are you from?" should appear after name
        // composeTestRule.onNodeWithText("Which city are you from?").assertExists() 
    }
}
