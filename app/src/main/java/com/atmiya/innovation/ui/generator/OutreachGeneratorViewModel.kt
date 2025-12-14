package com.atmiya.innovation.ui.generator

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.atmiya.innovation.data.Startup
import com.atmiya.innovation.repository.FirestoreRepository
import com.atmiya.innovation.repository.OutreachRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class OutreachGeneratorViewModel : ViewModel() {

    private val outreachRepository = OutreachRepository()
    private val firestoreRepository = FirestoreRepository()
    private val auth = FirebaseAuth.getInstance()

    // Data State
    private val _startup = MutableStateFlow<Startup?>(null)
    val startup: StateFlow<Startup?> = _startup.asStateFlow()
    
    private val _userCity = MutableStateFlow("")
    private val _founderName = MutableStateFlow("")

    // UI State
    private val _generatedMessage = MutableStateFlow("")
    val generatedMessage: StateFlow<String> = _generatedMessage.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    // Inputs
    private val _selectedType = MutableStateFlow(OutreachRepository.OutreachType.COLD_EMAIL)
    val selectedType = _selectedType.asStateFlow()
    
    private val _selectedStyle = MutableStateFlow(OutreachRepository.OutreachStyle.PROFESSIONAL)
    val selectedStyle = _selectedStyle.asStateFlow()

    init {
        fetchData()
    }

    private fun fetchData() {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Fetch Startup Profile
                val startupProfile = firestoreRepository.getStartup(uid)
                if (startupProfile != null) {
                    _startup.value = startupProfile
                }
                
                // Fetch User Profile for City and Name
                val userProfile = firestoreRepository.getUser(uid)
                if (userProfile != null) {
                    _userCity.value = userProfile.city
                    _founderName.value = userProfile.name
                }

            } catch (e: Exception) {
                _error.value = "Failed to load profile: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun onTypeSelected(type: OutreachRepository.OutreachType) {
        _selectedType.value = type
    }
    
    fun onStyleSelected(style: OutreachRepository.OutreachStyle) {
        _selectedStyle.value = style
    }

    fun generateMessage() {
        val currentStartup = _startup.value
        if (currentStartup == null) {
            _error.value = "Startup profile not loaded."
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _generatedMessage.value = "" // Clear previous
            _error.value = null
            
            val message = outreachRepository.generatePitch(
                startup = currentStartup,
                type = _selectedType.value,
                style = _selectedStyle.value,
                city = _userCity.value,
                founderName = _founderName.value
            )
            
            _generatedMessage.value = message
            _isLoading.value = false
        }
    }
}
