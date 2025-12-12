package com.atmiya.innovation.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.atmiya.innovation.data.GovernmentScheme
import com.atmiya.innovation.repository.GovernanceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class GovernanceViewModel : ViewModel() {
    
    private val repository = GovernanceRepository()

    private val _schemes = MutableStateFlow<List<GovernmentScheme>>(emptyList())
    val schemes: StateFlow<List<GovernmentScheme>> = _schemes.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    // Filter State
    private val _selectedCategory = MutableStateFlow("All")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    init {
        fetchSchemes()
    }

    private fun fetchSchemes() {
        viewModelScope.launch {
            _isLoading.value = true
            repository.getSchemes().collect {
                _schemes.value = it
                _isLoading.value = false
            }
        }
    }
    
    fun setCategory(category: String) {
        _selectedCategory.value = category
    }
}
