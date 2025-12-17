package com.atmiya.innovation.ui.dashboard.smartquestions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.atmiya.innovation.data.SmartQuestionsResponse
import com.atmiya.innovation.data.Startup
import com.atmiya.innovation.repository.SmartQuestionsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class SmartQuestionsUiState {
    object Idle : SmartQuestionsUiState()
    object Loading : SmartQuestionsUiState()
    data class Success(val data: SmartQuestionsResponse) : SmartQuestionsUiState()
    data class Error(val message: String) : SmartQuestionsUiState()
}

class SmartQuestionsViewModel : ViewModel() {
    private val repository = SmartQuestionsRepository()

    private val _uiState = MutableStateFlow<SmartQuestionsUiState>(SmartQuestionsUiState.Idle)
    val uiState: StateFlow<SmartQuestionsUiState> = _uiState.asStateFlow()

    fun generateQuestions(startup: Startup, pitchSummary: String) {
        viewModelScope.launch {
            _uiState.value = SmartQuestionsUiState.Loading
            
            val userId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: ""
            val result = repository.generateSmartQuestions(startup, pitchSummary, userId)
            
            result.onSuccess { response ->
                // Validate response structure
                if (validateResponse(response)) {
                    _uiState.value = SmartQuestionsUiState.Success(response)
                } else {
                    _uiState.value = SmartQuestionsUiState.Error("Questions generated but format invalid.")
                }
            }.onFailure { exception ->
                val errorMsg = exception.message ?: "Unknown error"
                // Check for common localized errors if needed, but passing raw message is best for debugging now
                // e.g. "GoogleGenerativeAIException: ... API key not valid..."
                _uiState.value = SmartQuestionsUiState.Error("Error: $errorMsg")
            }
        }
    }
    
    private fun validateResponse(response: SmartQuestionsResponse): Boolean {
        // Relaxed validation: Just ensure we have some questions to show
        return response.questions.isNotEmpty()
    }

    fun clearState() {
        _uiState.value = SmartQuestionsUiState.Idle
    }
}
