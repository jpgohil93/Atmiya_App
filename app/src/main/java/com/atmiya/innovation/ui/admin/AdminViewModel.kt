package com.atmiya.innovation.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.atmiya.innovation.data.User
import com.atmiya.innovation.repository.FirestoreRepository
import com.google.firebase.firestore.DocumentSnapshot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AdminViewModel : ViewModel() {
    private val repository = FirestoreRepository()

    private val _users = MutableStateFlow<List<User>>(emptyList())
    val users: StateFlow<List<User>> = _users.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore: StateFlow<Boolean> = _isLoadingMore.asStateFlow()

    private val _selectedRole = MutableStateFlow("startup") // startup, investor, mentor
    val selectedRole: StateFlow<String> = _selectedRole.asStateFlow()

    private val _selectedUser = MutableStateFlow<User?>(null)
    val selectedUser: StateFlow<User?> = _selectedUser.asStateFlow()
    
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    
    private val _hasMoreUsers = MutableStateFlow(true)
    val hasMoreUsers: StateFlow<Boolean> = _hasMoreUsers.asStateFlow()
    
    // Pagination cursor
    private var lastDocumentSnapshot: DocumentSnapshot? = null
    private val PAGE_SIZE = 50L

    init {
        loadUsers()
    }

    fun setRole(role: String) {
        _selectedRole.value = role
        resetAndLoad()
    }
    
    fun setSearchQuery(query: String) {
        _searchQuery.value = query
        // Filter existing list in-memory for instant feedback
        if (query.isNotBlank()) {
            filterLocalUsers(query)
        } else {
            resetAndLoad()
        }
    }
    
    private fun filterLocalUsers(query: String) {
        val lowerQuery = query.lowercase()
        viewModelScope.launch {
            // Re-fetch all without pagination for search
            _isLoading.value = true
            try {
                val allUsers = repository.getUsersByRole(_selectedRole.value)
                _users.value = allUsers.filter { user ->
                    user.name.lowercase().contains(lowerQuery) ||
                    user.email.lowercase().contains(lowerQuery) ||
                    user.phoneNumber.contains(lowerQuery)
                }
            } catch (e: Exception) {
                android.util.Log.e("AdminViewModel", "Search error", e)
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    private fun resetAndLoad() {
        lastDocumentSnapshot = null
        _hasMoreUsers.value = true
        _users.value = emptyList()
        loadUsers()
    }

    fun loadUsers() {
        if (_isLoading.value) return
        
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val (loadedUsers, lastDoc) = repository.getUsersByRolePaginated(
                    role = _selectedRole.value,
                    limit = PAGE_SIZE,
                    lastDocument = null,
                    searchQuery = null
                )
                lastDocumentSnapshot = lastDoc
                _hasMoreUsers.value = loadedUsers.size >= PAGE_SIZE
                _users.value = loadedUsers
                android.util.Log.d("AdminViewModel", "Loaded ${loadedUsers.size} users, hasMore=${_hasMoreUsers.value}")
            } catch (e: Exception) {
                android.util.Log.e("AdminViewModel", "Error loading users: ${e.message}", e)
                _users.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun loadMoreUsers() {
        if (_isLoadingMore.value || !_hasMoreUsers.value || lastDocumentSnapshot == null) return
        if (_searchQuery.value.isNotBlank()) return // No pagination during search
        
        viewModelScope.launch {
            _isLoadingMore.value = true
            try {
                val (moreUsers, lastDoc) = repository.getUsersByRolePaginated(
                    role = _selectedRole.value,
                    limit = PAGE_SIZE,
                    lastDocument = lastDocumentSnapshot,
                    searchQuery = null
                )
                lastDocumentSnapshot = lastDoc
                _hasMoreUsers.value = moreUsers.size >= PAGE_SIZE
                _users.value = _users.value + moreUsers
                android.util.Log.d("AdminViewModel", "Loaded ${moreUsers.size} more users, total=${_users.value.size}")
            } catch (e: Exception) {
                android.util.Log.e("AdminViewModel", "Error loading more users", e)
            } finally {
                _isLoadingMore.value = false
            }
        }
    }

    fun selectUser(userId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _selectedUser.value = repository.getUser(userId)
            _isLoading.value = false
        }
    }

    fun updateUserStatus(userId: String, isBlocked: Boolean? = null, isDeleted: Boolean? = null) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.updateUserStatus(userId, isBlocked, isDeleted)
                // Refresh list
                resetAndLoad()
                if (_selectedUser.value?.uid == userId) {
                    _selectedUser.value = repository.getUser(userId)
                }
            } catch (e: Exception) {
                android.util.Log.e("AdminViewModel", "Error updating user status", e)
            }
            _isLoading.value = false
        }
    }
}
