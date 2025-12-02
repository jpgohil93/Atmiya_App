package com.atmiya.innovation.logic

import org.junit.Assert.*
import org.junit.Test

class OnboardingManagerTest {

    @Test
    fun `test EDP flow sequence`() {
        val manager = OnboardingManager("startup", "edp", isTestMode = true)
        
        // Q1: Name
        var response = manager.processAnswer("John Doe")
        assertEquals("Which city are you from?", response?.text)
        
        // Q2: City
        response = manager.processAnswer("Ahmedabad")
        assertEquals("Select your sector.", response?.text)
        
        // Q3: Sector
        response = manager.processAnswer("Agri-tech")
        assertEquals("Tell me about your idea in a few sentences.", response?.text)
        
        // Q4: Idea
        response = manager.processAnswer("Smart irrigation system.")
        assertEquals("What is your current stage?", response?.text)
        
        // Q5: Stage
        response = manager.processAnswer("Prototype")
        assertTrue(response?.text?.contains("Pitch Deck") == true)
    }

    @Test
    fun `test Accelerator flow sequence`() {
        val manager = OnboardingManager("startup", "accelerator", isTestMode = true)
        
        // Q1: Startup Name
        var response = manager.processAnswer("AgriSmart")
        assertEquals("What is the founder's name?", response?.text)
    }

    @Test
    fun `test Investor flow sequence`() {
        val manager = OnboardingManager("investor", null, isTestMode = true)
        
        // Q1: Name
        var response = manager.processAnswer("Jane Doe")
        assertEquals("What is your firm/organisation name?", response?.text)

        // Q2: Firm Name
        response = manager.processAnswer("Ventures LLP")
        assertEquals("What is your typical ticket size range?", response?.text)
    }

    @Test
    fun `test Edge Case - Empty Answer`() {
        // Note: The current implementation might not handle empty strings explicitly in processAnswer 
        // if the UI blocks it. But let's see what happens if we pass one.
        val manager = OnboardingManager("startup", "edp", isTestMode = true)
        val response = manager.processAnswer("")
        // Assuming it proceeds because validation is currently in UI. 
        // Ideally, manager should validate. 
        assertNotNull(response) 
    }
    
    @Test
    fun `test Completion`() {
        val manager = OnboardingManager("startup", "edp", isTestMode = true)
        manager.processAnswer("A") // Name
        manager.processAnswer("B") // City
        manager.processAnswer("C") // Sector
        manager.processAnswer("D") // Idea
        manager.processAnswer("E") // Stage
        val response = manager.processAnswer("http://file.url") // Pitch Deck
        
        assertNotNull(response)
        assertTrue(response!!.text.startsWith("Thanks!"))
    }
}
