package com.atmiya.innovation.ui.admin

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.atmiya.innovation.data.ImportRecord
import com.atmiya.innovation.repository.FirestoreRepository
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID

data class CsvRow(
    val line: Int,
    val data: Map<String, String>,
    val isValid: Boolean,
    val errors: List<String>
)

data class ValidationSummary(
    val totalRows: Int = 0,
    val validRows: Int = 0,
    val invalidRows: Int = 0,
    val rows: List<CsvRow> = emptyList()
)

data class UploadResult(
    val isComplete: Boolean = false,
    val successCount: Int = 0,
    val failureCount: Int = 0,
    val totalCount: Int = 0,
    val errors: List<String> = emptyList()
)

class ImportViewModel : ViewModel() {
    private val firestoreRepository = FirestoreRepository()

    private val _validationSummary = MutableStateFlow(ValidationSummary())
    val validationSummary: StateFlow<ValidationSummary> = _validationSummary.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    // Store selected URI in ViewModel so it persists across navigation
    private val _selectedUri = MutableStateFlow<android.net.Uri?>(null)
    val selectedUri: StateFlow<android.net.Uri?> = _selectedUri.asStateFlow()
    
    fun setSelectedUri(uri: android.net.Uri?) {
        _selectedUri.value = uri
    }

    private val _uploadProgress = MutableStateFlow(0f) // 0.0 to 1.0
    val uploadProgress: StateFlow<Float> = _uploadProgress.asStateFlow()
    
    // "Processing X of Y"
    private val _uploadStatusText = MutableStateFlow("")
    val uploadStatusText: StateFlow<String> = _uploadStatusText.asStateFlow()

    private val _uploadResult = MutableStateFlow<UploadResult?>(null)
    val uploadResult: StateFlow<UploadResult?> = _uploadResult.asStateFlow()

    fun validateCsv(context: Context, uri: Uri) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Verify File Type
                val mimeType = context.contentResolver.getType(uri)
                val cursor = context.contentResolver.query(uri, null, null, null, null)
                val nameIndex = cursor?.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                cursor?.moveToFirst()
                val fileName = if (nameIndex != null && nameIndex >= 0) cursor.getString(nameIndex) else ""
                cursor?.close()

                val isCsv = mimeType?.contains("csv", true) == true || 
                            mimeType?.contains("excel", true) == true || 
                            mimeType?.contains("spreadsheet", true) == true ||
                            fileName.endsWith(".csv", true) ||
                            fileName.endsWith(".txt", true)

                if (!isCsv) {
                   // Allow it but log warning, or maybe just proceed as text? 
                   // Let's be permissible but safe. 
                   // If it's an image, readText might be bad.
                   if (mimeType?.startsWith("image/") == true || mimeType?.startsWith("video/") == true) {
                       throw Exception("Invalid file type: Please select a CSV file.")
                   }
                }

                val content = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                if (content == null) {
                    throw Exception("Could not read file")
                }

                val lines = content.lines().filter { it.isNotBlank() }
                val rows = mutableListOf<CsvRow>()
                
                // Prefetch existing phones (for uniqueness check)
                val existingPhones = firestoreRepository.getAllUserPhones().toSet()
                val seenPhonesInCsv = mutableSetOf<String>()

                // Skip header if present
                val dataLines = if (lines.firstOrNull()?.contains("email", true) == true) lines.drop(1) else lines

                dataLines.forEachIndexed { index, line ->
                    val lineNum = index + 2 // 1-indexed + header
                    val parts = line.split(",").map { it.trim() }
                    
                    val errors = mutableListOf<String>()
                    val data = mutableMapOf<String, String>()

                    // Parse Columns: Full Name, Phone, Email, City, Region, Organization
                    val name = parts.getOrNull(0)?.trim() ?: ""
                    var rawPhone = parts.getOrNull(1)?.trim() ?: ""
                    val email = parts.getOrNull(2)?.trim() ?: ""
                    val city = parts.getOrNull(3)?.trim() ?: ""
                    val region = parts.getOrNull(4)?.trim() ?: ""
                    val org = parts.getOrNull(5)?.trim() ?: ""

                    // Phone Cleaning
                    // 1. Remove spaces and common separators
                    var phone = rawPhone.replace("\\s".toRegex(), "").replace("-", "")
                    
                    // 2. Handle +91 or 91 prefix
                    if (phone.startsWith("+91")) {
                        phone = phone.substring(3)
                    } else if (phone.startsWith("91") && phone.length == 12) {
                        phone = phone.substring(2)
                    }

                    data["name"] = name
                    data["phone"] = phone
                    data["email"] = email
                    data["city"] = city
                    data["region"] = region
                    data["organization"] = org

                    // Validations
                    if (name.isBlank()) errors.add("Missing Full Name")
                    
                    // Phone Validation
                    // Ensure it contains only digits now
                    if (phone.isBlank()) {
                        errors.add("Missing Phone Number")
                    } else if (!phone.all { it.isDigit() }) {
                        errors.add("Phone contains invalid characters")
                    } else if (phone.length != 10) {
                        errors.add("Phone must be 10 digits")
                    }

                    if (email.isBlank()) {
                        errors.add("Missing Email")
                    } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                        errors.add("Invalid Email Format")
                    }

                    // Strict Phone Uniqueness Check
                    if (phone.isNotBlank()) {
                         if (existingPhones.contains(phone)) {
                            errors.add("Phone already exists (DB)")
                        } else if (seenPhonesInCsv.contains(phone)) {
                            errors.add("Duplicate Phone in CSV")
                        } else {
                            seenPhonesInCsv.add(phone)
                        }
                    }

                    if (city.isBlank()) errors.add("Missing City")
                    if (region.isBlank()) errors.add("Missing Region")

                    rows.add(CsvRow(lineNum, data, errors.isEmpty(), errors))
                }

                val validCount = rows.count { it.isValid }
                val invalidCount = rows.count { !it.isValid }

                _validationSummary.value = ValidationSummary(
                    totalRows = rows.size,
                    validRows = validCount,
                    invalidRows = invalidCount,
                    rows = rows
                )

            } catch (e: Exception) {
                android.util.Log.e("ImportViewModel", "Error validating CSV", e)
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun uploadValidRows() {
        val summary = _validationSummary.value
        val validRows = summary.rows.filter { it.isValid }
        if (validRows.isEmpty()) return

        val adminUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser

        viewModelScope.launch {
            _isLoading.value = true
            _uploadProgress.value = 0f
            _uploadResult.value = null
            
            try {
                // Map CsvRow to Domain Objects
                val uploadData = validRows.map { row ->
                    val uid = UUID.randomUUID().toString() // Note: This UID will be disparate from Auth UID until linked
                    val data = row.data
                    
                    val user = com.atmiya.innovation.data.User(
                        uid = uid,
                        name = data["name"] ?: "",
                        phoneNumber = data["phone"] ?: "",
                        email = data["email"] ?: "",
                        role = "startup",
                        city = data["city"] ?: "",
                        region = data["region"] ?: "",
                        isOnboardingComplete = true, // Basic details done
                        createdVia = "bulk",
                        hasCompletedBasicDetails = true,
                        hasCompletedRoleDetails = false,
                        createdAt = com.google.firebase.Timestamp.now(),
                        updatedAt = com.google.firebase.Timestamp.now()
                    )

                    val startup = com.atmiya.innovation.data.Startup(
                        uid = uid,
                        startupName = data["name"] ?: "", // Default to User Name until updated
                        organization = data["organization"] ?: "",
                        isDeleted = false
                    )
                    
                    Pair(user, startup)
                }

                // Call Repository
                val result = firestoreRepository.createBulkStartups(uploadData) { processed, total ->
                    _uploadProgress.value = processed.toFloat() / total.toFloat()
                    _uploadStatusText.value = "Uploading $processed of $total..."
                }
                
                // Create Import Record Log
                val record = ImportRecord(
                    id = UUID.randomUUID().toString(),
                    role = "startup",
                    status = if (result.failureCount == 0) "completed" else "completed_with_errors",
                    totalRows = summary.totalRows,
                    successCount = result.successCount,
                    failureCount = result.failureCount + summary.invalidRows, // Include validation failures
                    createdByAdminId = adminUser?.uid ?: "unknown",
                    createdAt = com.google.firebase.Timestamp.now(),
                    completedAt = com.google.firebase.Timestamp.now()
                )
                firestoreRepository.createImportRecord(record)
                
                _uploadResult.value = UploadResult(
                    isComplete = true,
                    successCount = result.successCount,
                    failureCount = result.failureCount,
                    totalCount = uploadData.size,
                    errors = result.errors
                )
                
                // Clear validation summary so we switch to Result view
                _validationSummary.value = ValidationSummary()
                
            } catch (e: Exception) {
                android.util.Log.e("ImportViewModel", "Upload Failed", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearValidation() {
        _validationSummary.value = ValidationSummary()
        _uploadResult.value = null
        _uploadProgress.value = 0f
    }
    
    fun deleteBulkData() {
        viewModelScope.launch {
            _isLoading.value = true
            _uploadStatusText.value = "Deleting existing bulk users..."
            try {
                val count = firestoreRepository.deleteBulkUsers { processed, total ->
                     _uploadStatusText.value = "Deleting... $processed / $total"
                }
                _uploadStatusText.value = "Deleted $count users."
                // Re-validate if file is selected? No, let user do it.
                // But we should refresh the "existing emails" cache if we used one?
                // The validateCsv function fetches it fresh every time, so no stale cache issues.
            } catch (e: Exception) {
                _uploadStatusText.value = "Delete failed: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Upload large CSV via Cloud Function instead of client-side processing.
     * Use for > 500 records.
     */
    fun uploadViaCloudFunction(context: Context, uri: Uri) {
        viewModelScope.launch {
            _isLoading.value = true
            _uploadProgress.value = -1f // -1 indicates indeterminate progress
            _uploadStatusText.value = "Preparing upload..."
            
            try {
                // Read CSV content
                val content = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                    ?: throw Exception("Could not read file")
                
                val lineCount = content.lines().filter { it.isNotBlank() }.size - 1 // Minus header
                _uploadStatusText.value = "⚡ Processing $lineCount records on server..."
                _uploadProgress.value = -1f // Keep indeterminate
                
                // Call Cloud Function
                val functions = FirebaseFunctions.getInstance()
                val data = hashMapOf("csvContent" to content)
                
                val result = functions.getHttpsCallable("processBulkUpload").call(data).await()
                
                _uploadProgress.value = 1f
                
                // Parse result
                @Suppress("UNCHECKED_CAST")
                val resultData = result.data as? Map<String, Any> ?: emptyMap()
                val successCount = (resultData["success"] as? Number)?.toInt() ?: 0
                val failedCount = (resultData["failed"] as? Number)?.toInt() ?: 0
                @Suppress("UNCHECKED_CAST")
                val errors = (resultData["errors"] as? List<String>) ?: emptyList()
                
                _uploadResult.value = UploadResult(
                    isComplete = true,
                    successCount = successCount,
                    failureCount = failedCount,
                    totalCount = successCount + failedCount,
                    errors = errors
                )
                
                _uploadStatusText.value = "Cloud upload complete!"
                _validationSummary.value = ValidationSummary()
                
            } catch (e: Exception) {
                android.util.Log.e("ImportViewModel", "Cloud Function upload failed", e)
                _uploadStatusText.value = "Cloud upload failed: ${e.message}"
                _uploadResult.value = UploadResult(
                    isComplete = true,
                    successCount = 0,
                    failureCount = 1,
                    errors = listOf("Cloud Function error: ${e.message}")
                )
            } finally {
                _isLoading.value = false
            }
        }
    }
}
