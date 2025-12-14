package com.atmiya.innovation.ui.dashboard.diagnosis

import com.atmiya.innovation.data.Startup
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations

@OptIn(ExperimentalCoroutinesApi::class)
class DiagnosisViewModelTest {

    private lateinit var viewModel: DiagnosisViewModel
    
    @Mock
    private lateinit var mockService: DiagnosisService

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(testDispatcher)
        viewModel = DiagnosisViewModel(mockService)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `generateDiagnosis success updates state to Success`() = runTest {
        // Arrange
        val startup = Startup(startupName = "Test Startup")
        val mockResponse = DiagnosisResponse(
            missing = listOf("Pitch Deck"),
            failurePoints = listOf("High burn rate"),
            focusNext = listOf("Hire sales team")
        )
        `when`(mockService.generateDiagnosis(startup)).thenReturn(mockResponse)

        // Act
        viewModel.generateDiagnosis(startup)
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert
        assertTrue(viewModel.uiState.value is DiagnosisUiState.Success)
        val successState = viewModel.uiState.value as DiagnosisUiState.Success
        assertEquals(mockResponse, successState.data)
        assertEquals("Pitch Deck", successState.data.missing[0])
    }

    @Test
    fun `generateDiagnosis failure updates state to Error`() = runTest {
        // Arrange
        val startup = Startup(startupName = "Test Startup")
        `when`(mockService.generateDiagnosis(startup)).thenThrow(RuntimeException("API Error"))

        // Act
        viewModel.generateDiagnosis(startup)
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert
        assertTrue(viewModel.uiState.value is DiagnosisUiState.Error)
        val errorState = viewModel.uiState.value as DiagnosisUiState.Error
        assertTrue(errorState.message.contains("API Error"))
    }

    @Test
    fun `generateAdvice success updates advice state`() = runTest {
        // Arrange
        val startup = Startup(startupName = "Test Startup")
        val diagnosis = DiagnosisResponse()
        val adviceText = "Great job, keep going!"
        
        `when`(mockService.generateAdvice(startup, diagnosis)).thenReturn(adviceText)

        // Act
        viewModel.generateAdvice(startup, diagnosis)
        
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert final state
        assertEquals(adviceText, viewModel.adviceState.value)
        assertTrue(!viewModel.isGeneratingAdvice.value)
    }
}
