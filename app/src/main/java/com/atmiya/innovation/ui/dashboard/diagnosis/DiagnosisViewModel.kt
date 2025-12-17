package com.atmiya.innovation.ui.dashboard.diagnosis

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.atmiya.innovation.data.Startup
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class DiagnosisUiState {
    object Idle : DiagnosisUiState()
    object Loading : DiagnosisUiState()
    data class Success(val data: DiagnosisResponse) : DiagnosisUiState()
    data class Error(val message: String) : DiagnosisUiState()
}

class DiagnosisViewModel(
    private val service: DiagnosisService = GeminiDiagnosisService()
) : ViewModel() {
    private val _uiState = MutableStateFlow<DiagnosisUiState>(DiagnosisUiState.Idle)
    val uiState: StateFlow<DiagnosisUiState> = _uiState.asStateFlow()

    private val _adviceState = MutableStateFlow<String>("")
    val adviceState: StateFlow<String> = _adviceState.asStateFlow()

    private val _isGeneratingAdvice = MutableStateFlow(false)
    val isGeneratingAdvice: StateFlow<Boolean> = _isGeneratingAdvice.asStateFlow()
    
    fun generateDiagnosis(startup: Startup) {
        viewModelScope.launch {
            _uiState.value = DiagnosisUiState.Loading
            
            try {
                val userId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: ""
                val diagnosis = service.generateDiagnosis(startup, userId)
                _uiState.value = DiagnosisUiState.Success(diagnosis)
            } catch (e: Exception) {
                // e.g. JSON parse error or API error
                _uiState.value = DiagnosisUiState.Error("Failed to generate diagnosis: ${e.message}")
            }
        }
    }

    fun generateAdvice(startup: Startup, diagnosis: DiagnosisResponse) {
        viewModelScope.launch {
            _isGeneratingAdvice.value = true
            try {
                val userId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: ""
                val advice = service.generateAdvice(startup, diagnosis, userId)
                _adviceState.value = advice
            } catch (e: Exception) {
                _adviceState.value = "Error generating advice: ${e.message}"
            } finally {
                _isGeneratingAdvice.value = false
            }
        }
    }
    
    fun clearState() {
        _uiState.value = DiagnosisUiState.Idle
        _adviceState.value = ""
    }
}
