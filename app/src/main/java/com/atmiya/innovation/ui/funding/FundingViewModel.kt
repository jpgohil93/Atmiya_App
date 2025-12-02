package com.atmiya.innovation.ui.funding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.atmiya.innovation.data.FundingCall
import com.atmiya.innovation.repository.FirestoreRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class FundingViewModel : ViewModel() {
    private val repository = FirestoreRepository()
    private val auth = FirebaseAuth.getInstance()

    private val _fundingCalls = MutableStateFlow<List<FundingCall>>(emptyList())
    val fundingCalls: StateFlow<List<FundingCall>> = _fundingCalls.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _selectedCall = MutableStateFlow<FundingCall?>(null)
    val selectedCall: StateFlow<FundingCall?> = _selectedCall.asStateFlow()

    // Filters
    private val _filterType = MutableStateFlow("all") // "all", "my_calls", "sector"
    val filterType: StateFlow<String> = _filterType.asStateFlow()

    init {
        loadFundingCalls()
    }

    fun setFilter(type: String) {
        _filterType.value = type
        loadFundingCalls()
    }

    fun loadFundingCalls() {
        val user = auth.currentUser ?: return
        viewModelScope.launch {
            _isLoading.value = true
            
            // Determine user role and sector (this would ideally come from a UserSession or similar)
            val userProfile = repository.getUser(user.uid)
            val isAdmin = userProfile?.role == "admin"
            val userSector = if (userProfile?.role == "startup") userProfile.startupCategory else null

            // If startup, default to sector filter if "all" is selected? 
            // Or just show all active? Let's stick to explicit filters for now.
            // Requirement: "For startups: list of relevant funding calls (e.g., matching sector)."
            
            val effectiveFilter = if (userProfile?.role == "startup" && _filterType.value == "all") "sector" else _filterType.value
            val effectiveSector = if (effectiveFilter == "sector") userSector else null

            repository.getFundingCalls(
                filterType = _filterType.value,
                userId = user.uid,
                sector = effectiveSector,
                isAdmin = isAdmin
            ).collect {
                _fundingCalls.value = it
                _isLoading.value = false
            }
        }
    }
    
    fun selectCall(callId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _selectedCall.value = repository.getFundingCall(callId)
            _isLoading.value = false
        }
    }
}
