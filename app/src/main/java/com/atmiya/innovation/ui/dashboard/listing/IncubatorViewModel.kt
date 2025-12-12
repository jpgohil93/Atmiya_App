package com.atmiya.innovation.ui.dashboard.listing

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.atmiya.innovation.data.Incubator
import com.atmiya.innovation.repository.IncubatorRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class IncubatorViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = IncubatorRepository(application)
    
    private val _allIncubators = MutableStateFlow<List<Incubator>>(emptyList())
    
    private val _filteredIncubators = MutableStateFlow<List<Incubator>>(emptyList())
    val filteredIncubators: StateFlow<List<Incubator>> = _filteredIncubators.asStateFlow()
    
    // Filters
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    
    private val _selectedState = MutableStateFlow<String?>(null)
    val selectedState: StateFlow<String?> = _selectedState.asStateFlow()
    
    private val _selectedSector = MutableStateFlow<String?>(null)
    val selectedSector: StateFlow<String?> = _selectedSector.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    val availableStates = MutableStateFlow<List<String>>(emptyList())
    val availableSectors = MutableStateFlow<List<String>>(emptyList())

    init {
        loadData()
        
        // Combine flows for filtering
        combine(_allIncubators, _searchQuery, _selectedState, _selectedSector) { list, query, state, sector ->
            filterList(list, query, state, sector)
        }.onEach { 
            _filteredIncubators.value = it 
        }.launchIn(viewModelScope)
    }

    private fun loadData() {
        viewModelScope.launch {
            _isLoading.value = true
            repository.getIncubators().collect { list ->
                _allIncubators.value = list
                availableStates.value = list.map { incubator -> incubator.state }.distinct().sorted()
                availableSectors.value = list.map { incubator -> incubator.sector }.distinct().sorted()
                _isLoading.value = false
            }
        }
    }
    
    private fun filterList(
        list: List<Incubator>, 
        query: String, 
        state: String?, 
        sector: String?
    ): List<Incubator> {
        return list.filter { item ->
            val matchesQuery = if (query.isBlank()) true else {
                item.name.contains(query, ignoreCase = true) || 
                item.city.contains(query, ignoreCase = true) ||
                item.state.contains(query, ignoreCase = true) ||
                item.sector.contains(query, ignoreCase = true)
            }
            val matchesState = if (state == null) true else item.state == state
            val matchesSector = if (sector == null) true else item.sector == sector
            
            matchesQuery && matchesState && matchesSector
        }
    }
    
    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }
    
    fun onStateSelected(state: String?) {
        _selectedState.value = state
    }
    
    fun onSectorSelected(sector: String?) {
        _selectedSector.value = sector
    }
    
    fun clearFilters() {
        _selectedState.value = null
        _selectedSector.value = null
        _searchQuery.value = ""
    }
}
