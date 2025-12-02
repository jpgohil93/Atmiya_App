package com.atmiya.innovation.ui.dashboard.home

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Rule
import org.junit.Test

class EventCompanionTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun eventCompanion_displays_all_cards() {
        composeTestRule.setContent {
            EventCompanionSection(
                userName = "Test User",
                participantId = "AIF-EDP-123",
                track = "EDP",
                onOpenMap = {}
            )
        }

        // Digital ID Card
        composeTestRule.onNodeWithText("Test User").assertExists()
        composeTestRule.onNodeWithText("AIF-EDP-123").assertExists()
        composeTestRule.onNodeWithText("EDP Track").assertExists()
        composeTestRule.onNodeWithText("Digital Delegate Pass").assertExists()

        // Schedule Card
        composeTestRule.onNodeWithText("Event Schedule").assertExists()
        composeTestRule.onNodeWithText("17 - 21 Dec").assertExists()

        // Venue Card
        composeTestRule.onNodeWithText("Venue").assertExists()
        composeTestRule.onNodeWithText("Atmiya University").assertExists()
    }
}
