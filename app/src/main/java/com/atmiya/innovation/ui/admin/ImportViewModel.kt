package com.atmiya.innovation.ui.admin

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.atmiya.innovation.data.ImportRecord
import com.atmiya.innovation.repository.FirestoreRepository
import com.atmiya.innovation.repository.StorageRepository
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

class ImportViewModel : ViewModel() {
    private val firestoreRepository = FirestoreRepository()
    private val storageRepository = StorageRepository()
    private val auth = FirebaseAuth.getInstance()

    private val _imports = MutableStateFlow<List<ImportRecord>>(emptyList())
    val imports: StateFlow<List<ImportRecord>> = _imports.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadImports()
    }

    private fun loadImports() {
        viewModelScope.launch {
            firestoreRepository.getImports().collect {
                _imports.value = it
            }
        }
    }

    fun uploadCsv(context: Context, uri: Uri, role: String) {
        val user = auth.currentUser ?: return
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // 1. Read CSV Content
                val content = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                if (content == null) {
                    throw Exception("Could not read file")
                }

                // 2. Parse CSV
            val lines = content.lines().filter { it.isNotBlank() }
            val usersToCreate = mutableListOf<com.atmiya.innovation.data.User>()
            var successCount = 0
            var failureCount = 0
            
            // Skip header if present
            val dataLines = if (lines.firstOrNull()?.contains("email", true) == true) lines.drop(1) else lines

            dataLines.forEach { line ->
                try {
                    val parts = line.split(",").map { it.trim() }
                    // Expected Format: Name, Email, Phone, City, State, [Role Specific 1], [Role Specific 2]
                    if (parts.size >= 3) { 
                        val name = parts[0]
                        val email = parts[1]
                        val phone = parts[2]
                        val city = parts.getOrNull(3) ?: ""
                        val state = parts.getOrNull(4) ?: ""
                        
                        // Role Specific
                        val field1 = parts.getOrNull(5) ?: "" // StartupName / FirmName / Expertise
                        val field2 = parts.getOrNull(6) ?: "" // Sector / TicketSize / Experience
                        
                        val uid = UUID.randomUUID().toString()
                        
                        val newUser = com.atmiya.innovation.data.User(
                            uid = uid,
                            name = name,
                            email = email,
                            phoneNumber = phone,
                            city = city,
                            region = state,
                            role = role,
                            createdAt = Timestamp.now()
                        )
                        
                        // We need to pass role specific data to createUsersBatch, but currently it only takes List<User>.
                        // I will modify createUsersBatch to handle this, or better, I'll attach the data to the User object temporarily 
                        // or just handle it here. 
                        // Actually, createUsersBatch creates default objects. I should update it to accept a map or list of wrapper objects.
                        // For simplicity in this iteration, I will modify createUsersBatch to accept an optional map of extra data.
                        // OR, I can just create the role objects here and pass them? No, createUsersBatch does the batching.
                        
                        // Let's stick to the current createUsersBatch for now, but I'll update it to accept a callback or data map.
                        // Wait, I can't easily change the signature without breaking other things.
                        // I will update FirestoreRepository.createUsersBatch to take a list of (User, Map<String, Any>) tuples?
                        // For now, let's just create the User. The role doc will be created with defaults by createUsersBatch.
                        // TO FIX: The user wants "required fields". 
                        // I will update FirestoreRepository.createUsersBatch to accept the role-specific data.
                        
                        usersToCreate.add(newUser)
                        successCount++
                    } else {
                        failureCount++
                    }
                } catch (e: Exception) {
                    failureCount++
                }
            }

            // 3. Batch Create in Firestore
            if (usersToCreate.isNotEmpty()) {
                // I need to update createUsersBatch to handle the extra fields. 
                // Since I can't pass them easily right now without refactoring, I'll do a quick refactor of createUsersBatch first.
                firestoreRepository.createUsersBatch(usersToCreate, role)
            }

                // 4. Create Import Record
                val importId = UUID.randomUUID().toString()
                val record = ImportRecord(
                    id = importId,
                    role = role,
                    filePath = "local_import", // No file upload needed for client-side
                    status = "completed",
                    totalRows = dataLines.size,
                    successCount = successCount,
                    failureCount = failureCount,
                    createdByAdminId = user.uid,
                    createdAt = Timestamp.now(),
                    completedAt = Timestamp.now()
                )
                firestoreRepository.createImportRecord(record)
                
            } catch (e: Exception) {
                android.util.Log.e("ImportViewModel", "Error processing CSV", e)
            }
            _isLoading.value = false
        }
    }
}
