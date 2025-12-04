package com.atmiya.innovation.ui.dashboard.startup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.atmiya.innovation.data.AIFEvent
import com.atmiya.innovation.data.FeaturedVideo
import com.atmiya.innovation.data.FundingCall
import com.atmiya.innovation.repository.FirestoreRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.joinAll

class StartupDashboardViewModel(
    private val repository: FirestoreRepository = FirestoreRepository()
) : ViewModel() {

    sealed class UiState<out T> {
        object Loading : UiState<Nothing>()
        data class Success<T>(val data: T) : UiState<T>()
        data class Error(val message: String) : UiState<Nothing>()
    }

    // State for featured videos
    private val _featuredVideos = MutableStateFlow<UiState<List<FeaturedVideo>>>(UiState.Loading)
    val featuredVideos: StateFlow<UiState<List<FeaturedVideo>>> = _featuredVideos.asStateFlow()

    // State for AIF events
    private val _aifEvents = MutableStateFlow<UiState<List<AIFEvent>>>(UiState.Loading)
    val aifEvents: StateFlow<UiState<List<AIFEvent>>> = _aifEvents.asStateFlow()

    // Debug info for events
    private val _eventDebugInfo = MutableStateFlow<String>("")
    val eventDebugInfo: StateFlow<String> = _eventDebugInfo.asStateFlow()

    // Refreshing state
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    // Funding Calls State
    private val _fundingCalls = MutableStateFlow<List<FundingCall>>(emptyList())
    val fundingCalls: StateFlow<List<FundingCall>> = _fundingCalls.asStateFlow()

    private val _recommendedCalls = MutableStateFlow<List<FundingCall>>(emptyList())
    val recommendedCalls: StateFlow<List<FundingCall>> = _recommendedCalls.asStateFlow()
    
    private val _isLoadingCalls = MutableStateFlow(false)
    val isLoadingCalls: StateFlow<Boolean> = _isLoadingCalls.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            
            // Fetch User Profile first to get sector for recommendations
            val userId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
            var userSector = ""
            if (userId != null) {
                try {
                    val user = repository.getUser(userId)
                    userSector = user?.startupCategory ?: ""
                } catch (e: Exception) {
                    android.util.Log.e("StartupViewModel", "Error fetching user for sector", e)
                }
            }

            // Parallel fetching
            val videosJob = launch { fetchFeaturedVideos() }
            val eventsJob = launch { fetchAIFEvents() }
            val callsJob = launch { fetchFundingCalls(userSector) }
            
            joinAll(videosJob, eventsJob, callsJob)
            _isRefreshing.value = false
        }
    }

    private suspend fun fetchFeaturedVideos() {
        _featuredVideos.value = UiState.Loading
        try {
            val videos = repository.getFeaturedVideos()
            _featuredVideos.value = UiState.Success(videos)
        } catch (e: Exception) {
            _featuredVideos.value = UiState.Error(e.message ?: "Unknown error")
        }
    }

    private suspend fun fetchAIFEvents() {
        _aifEvents.value = UiState.Loading
        val userId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: "Not Logged In"
        _eventDebugInfo.value = "Checking collections..."
        try {
            val events = repository.getAIFEvents()
            if (events.isEmpty()) {
                _eventDebugInfo.value = "Fetched 0 events. User: $userId."
            } else {
                _eventDebugInfo.value = "Success: ${events.size} events found. User: $userId"
            }
            _aifEvents.value = UiState.Success(events)
        } catch (e: Exception) {
            _eventDebugInfo.value = "Error: ${e.message}. User: $userId"
            _aifEvents.value = UiState.Error(e.message ?: "Unknown error")
        }
    }
    
    private suspend fun fetchFundingCalls(userSector: String) {
        _isLoadingCalls.value = true
        try {
            // Fetch All Calls
            val allCalls = repository.getFundingCalls(limit = 20)
            _fundingCalls.value = allCalls
            
            // Fetch Recommended
            if (userSector.isNotEmpty()) {
                val recommended = repository.getRecommendedFundingCalls(userSector)
                _recommendedCalls.value = recommended
            } else {
                _recommendedCalls.value = emptyList()
            }
        } catch (e: Exception) {
            android.util.Log.e("StartupViewModel", "Error fetching funding calls", e)
        } finally {
            _isLoadingCalls.value = false
        }
    }

    fun createTestCall() {
        viewModelScope.launch {
            try {
                repository.createTestFundingCall()
                refresh()
            } catch (e: Exception) {
                android.util.Log.e("StartupViewModel", "Error creating test call", e)
            }
        }
    }
}
