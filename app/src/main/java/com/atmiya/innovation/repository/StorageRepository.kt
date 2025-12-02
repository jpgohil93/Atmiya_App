package com.atmiya.innovation.repository

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import com.atmiya.innovation.utils.StorageUtils
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageException
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StorageRepository {

    // Explicitly use the bucket from google-services.json to avoid any default config issues
    private val storage = FirebaseStorage.getInstance("gs://atmiya-eacdf.firebasestorage.app")
    private val TAG = "StorageUpload"

    private fun logPerf(operation: String, durationMs: Long) {
        android.util.Log.d("Perf", "StorageRepo: $operation took $durationMs ms")
    }

    suspend fun uploadProfilePhoto(context: Context, userId: String, uri: Uri): String {
        val start = System.currentTimeMillis()
        val path = StorageUtils.profilePhoto(userId)
        val ref = storage.reference.child(path)
        
        Log.d(TAG, "Starting profile photo upload. Path: $path, Bucket: ${ref.bucket}")

        return try {
            ref.putFile(uri).continueWithTask { task ->
                if (!task.isSuccessful) {
                    task.exception?.let { throw it }
                }
                ref.downloadUrl
            }.await().toString().also {
                Log.d(TAG, "Profile photo upload success. URL: $it")
                logPerf("uploadProfilePhoto", System.currentTimeMillis() - start)
            }
        } catch (e: StorageException) {
            Log.e(TAG, "Profile photo upload failed: ${e.message}", e)
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Profile photo upload failed (Generic): ${e.message}", e)
            throw e
        }
    }

    suspend fun uploadPitchDeck(context: Context, userId: String, uri: Uri, isPdf: Boolean): String {
        val start = System.currentTimeMillis()
        val fileSize = getFileSize(context, uri)
        val maxSizeBytes = if (isPdf) 10 * 1024 * 1024L else 20 * 1024 * 1024L
        
        if (fileSize > maxSizeBytes) {
            throw IllegalArgumentException("File too large. Max size: ${if (isPdf) "10MB" else "20MB"}")
        }

        val filename = getFileName(context, uri) ?: UUID.randomUUID().toString()
        val path = if (isPdf) {
            StorageUtils.startupPitchDeckPdf(userId, filename)
        } else {
            StorageUtils.startupPitchDeckPpt(userId, filename)
        }
        
        val ref = storage.reference.child(path)
        Log.d(TAG, "Starting pitch deck upload. Path: $path, Bucket: ${ref.bucket}")

        return try {
            ref.putFile(uri).continueWithTask { task ->
                if (!task.isSuccessful) {
                    task.exception?.let { throw it }
                }
                ref.downloadUrl
            }.await().toString().also {
                Log.d(TAG, "Pitch deck upload success. URL: $it")
                logPerf("uploadPitchDeck", System.currentTimeMillis() - start)
            }
        } catch (e: StorageException) {
            Log.e(TAG, "Pitch deck upload failed: ${e.message}", e)
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Pitch deck upload failed (Generic): ${e.message}", e)
            throw e
        }
    }

    suspend fun uploadMentorVideoThumbnail(context: Context, mentorId: String, uri: Uri): String {
        val start = System.currentTimeMillis()
        val filename = UUID.randomUUID().toString() + ".jpg"
        val path = StorageUtils.mentorVideoThumbnail(mentorId, filename)
        val ref = storage.reference.child(path)

        Log.d(TAG, "Starting thumbnail upload. Path: $path")

        return try {
            ref.putFile(uri).continueWithTask { task ->
                if (!task.isSuccessful) {
                    task.exception?.let { throw it }
                }
                ref.downloadUrl
            }.await().toString().also {
                Log.d(TAG, "Thumbnail upload success. URL: $it")
                logPerf("uploadMentorVideoThumbnail", System.currentTimeMillis() - start)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Thumbnail upload failed", e)
            throw e
        }
    }

    suspend fun uploadStartupLogo(context: Context, userId: String, uri: Uri): String {
        val start = System.currentTimeMillis()
        val fileSize = getFileSize(context, uri)
        val maxSizeBytes = 5 * 1024 * 1024L // 5MB

        if (fileSize > maxSizeBytes) {
            throw IllegalArgumentException("Logo too large. Max size: 5MB")
        }

        val path = "startup_logos/$userId/logo.png" // Matches storage.rules
        val ref = storage.reference.child(path)

        Log.d(TAG, "Starting logo upload. Path: $path")

        return try {
            ref.putFile(uri).continueWithTask { task ->
                if (!task.isSuccessful) {
                    task.exception?.let { throw it }
                }
                ref.downloadUrl
            }.await().toString().also {
                Log.d(TAG, "Logo upload success. URL: $it")
                logPerf("uploadStartupLogo", System.currentTimeMillis() - start)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Logo upload failed", e)
            throw e
        }
    }

    suspend fun uploadWallMedia(context: Context, postId: String, uri: Uri, isVideo: Boolean): String {
        val start = System.currentTimeMillis()
        val fileSize = getFileSize(context, uri)
        val maxSizeBytes = if (isVideo) 50 * 1024 * 1024L else 10 * 1024 * 1024L // 50MB Video, 10MB Image (generous)

        if (fileSize > maxSizeBytes) {
            throw IllegalArgumentException("File too large. Max size: ${if (isVideo) "50MB" else "10MB"}")
        }

        val filename = UUID.randomUUID().toString() + if (isVideo) ".mp4" else ".jpg"
        val path = if (isVideo) "wallVideos/$postId/$filename" else "wallImages/$postId/$filename"
        val ref = storage.reference.child(path)

        Log.d(TAG, "Starting wall media upload. Path: $path")

        return try {
            ref.putFile(uri).continueWithTask { task ->
                if (!task.isSuccessful) {
                    task.exception?.let { throw it }
                }
                ref.downloadUrl
            }.await().toString().also {
                Log.d(TAG, "Wall media upload success. URL: $it")
                logPerf("uploadWallMedia", System.currentTimeMillis() - start)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Wall media upload failed", e)
            throw e
        }
    }

    suspend fun uploadFundingAttachment(context: Context, callId: String, uri: Uri, isPdf: Boolean): String {
        val start = System.currentTimeMillis()
        val fileSize = getFileSize(context, uri)
        val maxSizeBytes = if (isPdf) 10 * 1024 * 1024L else 20 * 1024 * 1024L // 10MB PDF, 20MB PPT

        if (fileSize > maxSizeBytes) {
            throw IllegalArgumentException("File too large. Max size: ${if (isPdf) "10MB" else "20MB"}")
        }

        val filename = getFileName(context, uri) ?: UUID.randomUUID().toString()
        val path = "fundingAttachments/$callId/$filename"
        val ref = storage.reference.child(path)

        Log.d(TAG, "Starting funding attachment upload. Path: $path")

        return try {
            ref.putFile(uri).continueWithTask { task ->
                if (!task.isSuccessful) {
                    task.exception?.let { throw it }
                }
                ref.downloadUrl
            }.await().toString().also {
                Log.d(TAG, "Funding attachment upload success. URL: $it")
                logPerf("uploadFundingAttachment", System.currentTimeMillis() - start)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Funding attachment upload failed", e)
            throw e
        }
    }

    suspend fun uploadMentorVideo(context: Context, mentorId: String, uri: Uri): String {
        val start = System.currentTimeMillis()
        val fileSize = getFileSize(context, uri)
        val maxSizeBytes = 50 * 1024 * 1024L // 50MB

        if (fileSize > maxSizeBytes) {
            throw IllegalArgumentException("Video too large. Max size: 50MB")
        }

        val filename = UUID.randomUUID().toString() + ".mp4"
        val path = "mentorVideos/$filename"
        val ref = storage.reference.child(path)

        Log.d(TAG, "Starting mentor video upload. Path: $path")

        return try {
            ref.putFile(uri).continueWithTask { task ->
                if (!task.isSuccessful) {
                    task.exception?.let { throw it }
                }
                ref.downloadUrl
            }.await().toString().also {
                Log.d(TAG, "Mentor video upload success. URL: $it")
                logPerf("uploadMentorVideo", System.currentTimeMillis() - start)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Mentor video upload failed", e)
            throw e
        }
    }

    fun validateWallMedia(context: Context, uri: Uri, isVideo: Boolean) {
        val fileSize = getFileSize(context, uri)
        val maxSizeBytes = if (isVideo) 50 * 1024 * 1024L else 10 * 1024 * 1024L
        if (fileSize > maxSizeBytes) {
            throw IllegalArgumentException("File too large. Max size: ${if (isVideo) "50MB" else "10MB"}")
        }
    }

    suspend fun uploadImportCsv(context: Context, uri: Uri, role: String, importId: String): String {
        val filename = "$importId.csv"
        // Path: adminImports/{adminId}/{role}/{filename}
        // We need adminId here, but repository doesn't know auth state easily.
        // Let's pass it or just use a simpler path for now since rules will check auth.
        // Better path: adminImports/{role}/{filename} and rely on metadata or just path.
        // Actually, let's use: imports/{role}/{filename}
        
        val path = "imports/$role/$filename"
        val ref = storage.reference.child(path)
        
        return try {
            ref.putFile(uri).await()
            path // Return path, not URL, as Cloud Function reads from Storage path
        } catch (e: Exception) {
            throw e
        }
    }

    private fun getFileSize(context: Context, uri: Uri): Long {
        return try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (cursor.moveToFirst() && sizeIndex != -1) {
                    cursor.getLong(sizeIndex)
                } else 0L
            } ?: 0L
        } catch (e: Exception) {
            Log.e(TAG, "Error getting file size", e)
            0L
        }
    }

    private fun getFileName(context: Context, uri: Uri): String? {
        return try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (cursor.moveToFirst() && nameIndex != -1) {
                    cursor.getString(nameIndex)
                } else null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting file name", e)
            null
        }
    }
}
