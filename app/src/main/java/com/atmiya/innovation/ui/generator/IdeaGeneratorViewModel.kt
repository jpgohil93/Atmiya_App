package com.atmiya.innovation.ui.generator

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.atmiya.innovation.data.GeneratorInputs
import com.atmiya.innovation.data.StartupIdea
import com.atmiya.innovation.repository.IdeaGeneratorRepository

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import androidx.lifecycle.viewmodel.compose.viewModel // likely not needed here but good to know
// Remove Hilt imports

class IdeaGeneratorViewModel(
    private val repository: IdeaGeneratorRepository = IdeaGeneratorRepository(),
    private val firestoreRepository: com.atmiya.innovation.repository.FirestoreRepository = com.atmiya.innovation.repository.FirestoreRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(IdeaGeneratorState())
    val uiState: StateFlow<IdeaGeneratorState> = _uiState.asStateFlow()

    fun updateInputs(inputs: GeneratorInputs) {
        _uiState.update { it.copy(inputs = inputs) }
    }

    fun onSectorToggle(sector: String) {
        _uiState.update { currentState ->
            val updatedSectors = if (currentState.inputs.selectedSectors.contains(sector)) {
                currentState.inputs.selectedSectors - sector
            } else {
                currentState.inputs.selectedSectors + sector
            }
            currentState.copy(inputs = currentState.inputs.copy(selectedSectors = updatedSectors))
        }
    }

    fun onFieldChange(field: (GeneratorInputs) -> GeneratorInputs) {
        _uiState.update { it.copy(inputs = field(it.inputs)) }
    }

    fun generateIdeas() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val ideas = repository.generateStartupIdeas(_uiState.value.inputs)
                if (ideas.isNotEmpty()) {
                    _uiState.update { it.copy(isLoading = false, ideas = ideas, currentStep = 4) } // Jump to results
                } else {
                    _uiState.update { it.copy(isLoading = false, error = "Could not generate ideas. Please try again.") }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                android.util.Log.e("IdeaGenVM", "Generation failed", e)
                _uiState.update { it.copy(isLoading = false, error = "Failed: ${e.message ?: "Unknown Error"}") }
            }
        }
    }
    
    fun saveIdea(idea: StartupIdea) {
        viewModelScope.launch {
            val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
            if (uid == null) {
                _uiState.update { it.copy(error = "User not logged in") }
                return@launch
            }
            
            val ideaToSave = idea.copy(userId = uid)
            try {
                firestoreRepository.saveStartupIdea(ideaToSave)
                // Optional: Show success message via a one-off event or snackbar (using error field for now for simplicity, or add a message field)
                _uiState.update { it.copy(error = "Idea saved successfully!") } // Re-using error field as "message" for now, ideally separate
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Failed to save: ${e.message}") }
            }
        }
    }

    fun resetGenerator() {
        _uiState.update { IdeaGeneratorState() } // Reset to step 1 empty
    }

    fun nextStep() {
        _uiState.update { 
            if (it.currentStep < 3) it.copy(currentStep = it.currentStep + 1) else it 
        }
    }

    fun prevStep() {
        _uiState.update { 
            if (it.currentStep > 1) it.copy(currentStep = it.currentStep - 1) else it 
        }
    }
}

data class IdeaGeneratorState(
    val currentStep: Int = 1,
    val inputs: GeneratorInputs = GeneratorInputs(),
    val isLoading: Boolean = false,
    val ideas: List<StartupIdea> = emptyList(),
    val error: String? = null
)
