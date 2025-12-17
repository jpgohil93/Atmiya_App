package com.atmiya.innovation.utils

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Manages rate limiting for Gemini API calls.
 * Enforces a limit of 50 requests per user per day.
 */
object GeminiRateLimitManager {
    
    private const val TAG = "GeminiRateLimitManager"
    private const val COLLECTION_NAME = "gemini_usage"
    private const val DAILY_LIMIT = 50
    
    private val db = FirebaseFirestore.getInstance()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    
    /**
     * Exception thrown when rate limit is exceeded.
     */
    class RateLimitExceededException(
        val currentCount: Int,
        val limit: Int = DAILY_LIMIT
    ) : Exception("Daily AI request limit reached. You have used $currentCount of $limit requests today. Please try again tomorrow.")
    
    /**
     * Checks if the user has remaining quota and increments the usage count.
     * 
     * @param userId The user's unique identifier
     * @return Result.success(remainingCount) if allowed, Result.failure(RateLimitExceededException) if exceeded
     */
    suspend fun checkAndIncrementUsage(userId: String): Result<Int> {
        if (userId.isBlank()) {
            Log.w(TAG, "Empty userId provided, allowing request without tracking")
            return Result.success(DAILY_LIMIT)
        }
        
        return try {
            val docRef = db.collection(COLLECTION_NAME).document(userId)
            val today = dateFormat.format(Date())
            
            val result = db.runTransaction { transaction ->
                val snapshot = transaction.get(docRef)
                
                val storedDate = snapshot.getString("date") ?: ""
                val currentCount = snapshot.getLong("requestCount")?.toInt() ?: 0
                
                // Reset count if it's a new day
                val effectiveCount = if (storedDate != today) 0 else currentCount
                
                if (effectiveCount >= DAILY_LIMIT) {
                    throw RateLimitExceededException(effectiveCount)
                }
                
                val newCount = effectiveCount + 1
                val data = mapOf(
                    "requestCount" to newCount,
                    "date" to today,
                    "updatedAt" to Timestamp.now()
                )
                
                transaction.set(docRef, data)
                
                Log.d(TAG, "User $userId: $newCount/$DAILY_LIMIT requests used today")
                DAILY_LIMIT - newCount // Return remaining count
            }.await()
            
            Result.success(result)
        } catch (e: RateLimitExceededException) {
            Log.w(TAG, "Rate limit exceeded for user $userId: ${e.message}")
            Result.failure(e)
        } catch (e: Exception) {
            Log.e(TAG, "Error checking rate limit for user $userId", e)
            // On error, allow the request to proceed (fail-open for better UX)
            Result.success(DAILY_LIMIT)
        }
    }
    
    /**
     * Gets the current usage count for a user without incrementing.
     * 
     * @param userId The user's unique identifier
     * @return The current request count for today, or 0 if none
     */
    suspend fun getCurrentUsage(userId: String): Int {
        if (userId.isBlank()) return 0
        
        return try {
            val docRef = db.collection(COLLECTION_NAME).document(userId)
            val snapshot = docRef.get().await()
            
            val storedDate = snapshot.getString("date") ?: ""
            val today = dateFormat.format(Date())
            
            if (storedDate == today) {
                snapshot.getLong("requestCount")?.toInt() ?: 0
            } else {
                0 // New day, count is reset
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting current usage for user $userId", e)
            0
        }
    }
    
    /**
     * Gets the remaining quota for a user.
     * 
     * @param userId The user's unique identifier
     * @return The remaining requests available today
     */
    suspend fun getRemainingQuota(userId: String): Int {
        return DAILY_LIMIT - getCurrentUsage(userId)
    }
}
