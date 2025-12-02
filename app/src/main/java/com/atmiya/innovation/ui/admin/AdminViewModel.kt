package com.atmiya.innovation.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.atmiya.innovation.data.User
import com.atmiya.innovation.repository.FirestoreRepository
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

    private val _selectedRole = MutableStateFlow("startup") // startup, investor, mentor
    val selectedRole: StateFlow<String> = _selectedRole.asStateFlow()

    private val _selectedUser = MutableStateFlow<User?>(null)
    val selectedUser: StateFlow<User?> = _selectedUser.asStateFlow()

    init {
        loadUsers()
    }

    fun setRole(role: String) {
        _selectedRole.value = role
        loadUsers()
    }

    fun loadUsers() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val currentRole = _selectedRole.value
                val loadedUsers = repository.getUsersByRole(currentRole)
                android.util.Log.d("AdminViewModel", "Loaded ${loadedUsers.size} users for role: $currentRole")
                if (loadedUsers.isEmpty()) {
                    android.util.Log.w("AdminViewModel", "No users found for role: $currentRole. Check Firestore collection 'users' and field 'role'.")
                }
                _users.value = loadedUsers
            } catch (e: Exception) {
                android.util.Log.e("AdminViewModel", "Error loading users: ${e.message}", e)
                _users.value = emptyList() // Keep this line to clear list on error
            } finally {
                _isLoading.value = false
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
                // Refresh list or update local state
                loadUsers()
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
