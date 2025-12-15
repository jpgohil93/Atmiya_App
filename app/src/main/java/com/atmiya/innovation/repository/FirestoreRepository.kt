package com.atmiya.innovation.repository

import com.atmiya.innovation.data.*
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.Timestamp
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.ListenerRegistration
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirestoreRepository {

    private val db = FirebaseFirestore.getInstance()

    // --- App Config (Force Update) ---
    suspend fun getAppConfig(): AppConfig {
        return try {
            val snapshot = db.collection("app_config").document("android").get().await()
            if (snapshot.exists()) {
                // Manual parsing to avoid any serialization issues
                val minVer = snapshot.getLong("min_version_code")?.toInt() ?: 0
                val forceMsg = snapshot.getString("force_update_message") ?: "Update Required"
                val forceTitle = snapshot.getString("force_update_title") ?: "Update App"
                
                android.util.Log.d("FirestoreRepo", "getAppConfig: Found doc. min_version=$minVer")
                AppConfig(
                    minVersionCode = minVer,
                    forceUpdateMessage = forceMsg,
                    forceUpdateTitle = forceTitle
                )
            } else {
                android.util.Log.w("FirestoreRepo", "getAppConfig: Document app_config/android DOES NOT EXIST")
                AppConfig() // Return default if doc doesn't exist
            }
        } catch (e: Exception) {
            android.util.Log.e("FirestoreRepo", "Error fetching AppConfig", e)
            AppConfig() // Fail gracefully with default (no force update)
        }
    }
    
    // Debugging removed
    private fun log(msg: String) {
        android.util.Log.d("FirestoreRepo", msg)
    }

    private fun logPerf(operation: String, durationMs: Long) {
        android.util.Log.d("Perf", "FirestoreRepo: $operation took $durationMs ms")
    }

    // --- User Operations ---

    suspend fun createUser(user: User) {
        val start = System.currentTimeMillis()
        db.collection("users").document(user.uid).set(user).await()
        logPerf("createUser", System.currentTimeMillis() - start)
    }

    suspend fun updateFcmToken(userId: String) {
        try {
            val token = com.google.firebase.messaging.FirebaseMessaging.getInstance().token.await()
            db.collection("users").document(userId).update("fcmToken", token).await()
        } catch (e: Exception) {
            // Log error or ignore
        }
    }

    suspend fun getUser(uid: String): User? {
        val start = System.currentTimeMillis()
        val snapshot = db.collection("users").document(uid).get().await()
        val user = snapshot.toObject<User>()?.copy(uid = snapshot.id)
        logPerf("getUser", System.currentTimeMillis() - start)
        return user
    }

    fun getUserFlow(uid: String): Flow<User?> = callbackFlow {
        val listener = db.collection("users").document(uid).addSnapshotListener { snapshot, error ->
            if (error != null) {
                log("getUserFlow ERROR: ${error.message}")
                trySend(null)
                return@addSnapshotListener
            }
            if (snapshot != null && snapshot.exists()) {
                trySend(snapshot.toObject<User>())
            } else {
                trySend(null)
            }
        }
        awaitClose { listener.remove() }
    }

    suspend fun updateUser(uid: String, data: Map<String, Any>) {
        db.collection("users").document(uid).update(data).await()
    }

    suspend fun updateStartup(uid: String, data: Map<String, Any>) {
        db.collection("startups").document(uid).update(data).await()
    }

    suspend fun updateInvestor(uid: String, data: Map<String, Any>) {
        db.collection("investors").document(uid).update(data).await()
    }

    suspend fun updateMentor(uid: String, data: Map<String, Any>) {
        db.collection("mentors").document(uid).update(data).await()
    }

    // --- Role Specific Operations ---

    suspend fun createStartup(startup: Startup) {
        db.collection("startups").document(startup.uid).set(startup).await()
    }

    suspend fun getStartup(uid: String): Startup? {
        return db.collection("startups").document(uid).get().await().toObject<Startup>()
    }

    suspend fun createInvestor(investor: Investor) {
        db.collection("investors").document(investor.uid).set(investor).await()
    }

    suspend fun getInvestor(uid: String): Investor? {
        return db.collection("investors").document(uid).get().await().toObject<Investor>()
    }

    suspend fun createMentor(mentor: Mentor) {
        db.collection("mentors").document(mentor.uid).set(mentor).await()
    }

    suspend fun getMentor(uid: String): Mentor? {
        return db.collection("mentors").document(uid).get().await().toObject<Mentor>()
    }

    // --- Data Sync Helper ---
    // --- Data Sync Helper ---
    suspend fun syncUserToRole(uid: String, role: String) {
        try {
            val user = getUser(uid)
            if (user == null) {
                log("syncUserToRole ERROR: User not found for uid: $uid")
                return
            }
            
            val updates = mutableMapOf<String, Any>()
            if (user.profilePhotoUrl != null) updates["profilePhotoUrl"] = user.profilePhotoUrl
            if (user.name.isNotBlank()) updates["name"] = user.name
            
            if (updates.isNotEmpty()) {
                when(role) {
                    "mentor" -> db.collection("mentors").document(uid).update(updates).await()
                    "investor" -> db.collection("investors").document(uid).update(updates).await()
                    "startup" -> {
                        val startupUpdates = mutableMapOf<String, Any>()
                        if (user.profilePhotoUrl != null) startupUpdates["logoUrl"] = user.profilePhotoUrl
                        if (user.name.isNotBlank()) startupUpdates["startupName"] = user.name
                         db.collection("startups").document(uid).update(startupUpdates).await()
                    }
                }
                log("syncUserToRole: Success for $uid as $role")
            } else {
                log("syncUserToRole: No updates needed for $uid")
            }
        } catch (e: Exception) {
            log("syncUserToRole CRITICAL ERROR: ${e.message}")
            android.util.Log.e("FirestoreRepo", "Sync failed", e)
        }
    }

    suspend fun syncAllUsers(role: String) {
        try {
            val collectionName = when(role) {
                "mentor" -> "mentors"
                "investor" -> "investors"
                "startup" -> "startups"
                else -> return
            }

            log("syncAllUsers: Starting batch sync for role: $role")
            
            // Get ALL users with this role
            val usersSnapshot = db.collection("users").whereEqualTo("role", role).get().await()
            log("syncAllUsers: Found ${usersSnapshot.size()} users in master collection")

            // Process in chunks of 400 (limit is 500)
            usersSnapshot.documents.chunked(400).forEach { chunk ->
                val batch = db.batch()
                var count = 0
                
                chunk.forEach { doc ->
                    val user = doc.toObject<User>()
                    if (user != null) {
                        val targetRef = db.collection(collectionName).document(user.uid)
                        val updates = mutableMapOf<String, Any>()
                        
                        // Map fields
                        if (user.profilePhotoUrl != null) {
                            val field = if(role == "startup") "logoUrl" else "profilePhotoUrl"
                            updates[field] = user.profilePhotoUrl ?: ""
                        }
                        if (user.name.isNotBlank()) {
                            val field = if(role == "startup") "startupName" else "name"
                            updates[field] = user.name
                        }
                        
                        // Sync isDeleted status
                        updates["isDeleted"] = user.isDeleted
                        
                        // Use set with merge to create if missing or update if exists
                        if (updates.isNotEmpty()) {
                            batch.set(targetRef, updates, com.google.firebase.firestore.SetOptions.merge())
                            count++
                        }
                    }
                }
                
                if (count > 0) {
                    batch.commit().await()
                    log("syncAllUsers: Committed batch of $count updates")
                }
            }
            log("syncAllUsers: Completed sync for $role")
            
        } catch (e: Exception) {
             log("syncAllUsers ERROR: ${e.message}")
             android.util.Log.e("FirestoreRepo", "Batch sync failed", e)
             throw e // Rethrow to notify UI
        }
    }

    // --- Lists (Real-time Flows) ---
    
    // --- Dynamic Listings (Users + Role Details) ---

    // Helper to get users flow by role (filtered)
    private fun getUsersByRoleFlow(role: String): Flow<List<User>> = callbackFlow {
        log("getUsersByRoleFlow: Starting listener for role: $role")
        val listener = db.collection("users")
            .whereEqualTo("role", role)
            .whereEqualTo("isDeleted", false)
            .whereEqualTo("isBlocked", false) // Ensure blocked users are hidden
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    log("getUsersByRoleFlow ERROR: ${error.message}")
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val users = snapshot?.documents?.mapNotNull { it.toObject<User>() } ?: emptyList()
                log("getUsersByRoleFlow: Found ${users.size} active users for role $role")
                trySend(users)
            }
        awaitClose { listener.remove() }
    }

    fun getMentorsFlow(): Flow<List<Mentor>> {
        val usersFlow = getUsersByRoleFlow("mentor")
        val detailsFlow = callbackFlow {
            val listener = db.collection("mentors").addSnapshotListener { snapshot, _ ->
                val details = snapshot?.documents?.mapNotNull { it.toObject<Mentor>() } ?: emptyList()
                trySend(details)
            }
            awaitClose { listener.remove() }
        }

        return kotlinx.coroutines.flow.combine(usersFlow, detailsFlow) { users, details ->
            users.map { user ->
                val detail = details.find { it.uid == user.uid }
                // Merge User info (source of truth) with specific details
                Mentor(
                    uid = user.uid,
                    name = user.name, // User collection is master for name
                    profilePhotoUrl = if (!user.profilePhotoUrl.isNullOrBlank()) user.profilePhotoUrl else detail?.profilePhotoUrl, // User collection is master for photo
                    city = user.city, // User collection is master for city
                    title = detail?.title ?: "Mentor",
                    organization = detail?.organization ?: "",
                    expertiseAreas = detail?.expertiseAreas ?: emptyList(),
                    topicsToTeach = detail?.topicsToTeach ?: emptyList(),
                    experienceYears = detail?.experienceYears ?: "",
                    bio = detail?.bio ?: "",
                    isDeleted = false // Filtered by users flow already
                )
            }
        }
    }

    fun getInvestorsFlow(): Flow<List<Investor>> {
        val usersFlow = getUsersByRoleFlow("investor")
        val detailsFlow = callbackFlow {
            val listener = db.collection("investors").addSnapshotListener { snapshot, _ ->
                val details = snapshot?.documents?.mapNotNull { it.toObject<Investor>() } ?: emptyList()
                trySend(details)
            }
            awaitClose { listener.remove() }
        }

        return kotlinx.coroutines.flow.combine(usersFlow, detailsFlow) { users, details ->
            users.map { user ->
                val detail = details.find { it.uid == user.uid }
                Investor(
                    uid = user.uid,
                    name = user.name,
                    profilePhotoUrl = if (!user.profilePhotoUrl.isNullOrBlank()) user.profilePhotoUrl else detail?.profilePhotoUrl,
                    city = user.city,
                    firmName = detail?.firmName ?: "",
                    sectorsOfInterest = detail?.sectorsOfInterest ?: emptyList(),
                    preferredStages = detail?.preferredStages ?: emptyList(),
                    ticketSizeMin = detail?.ticketSizeMin ?: "",
                    investmentType = detail?.investmentType ?: "",
                    bio = detail?.bio ?: "",
                    isDeleted = false
                )
            }
        }
    }
    
    fun getStartupsFlow(): Flow<List<Startup>> {
        val usersFlow = getUsersByRoleFlow("startup")
        val detailsFlow = callbackFlow {
            val listener = db.collection("startups").addSnapshotListener { snapshot, _ ->
                val details = snapshot?.documents?.mapNotNull { it.toObject<Startup>() } ?: emptyList()
                trySend(details)
            }
            awaitClose { listener.remove() }
        }

        return kotlinx.coroutines.flow.combine(usersFlow, detailsFlow) { users, details ->
            users.map { user ->
                val detail = details.find { it.uid == user.uid }
                Startup(
                    uid = user.uid,
                    startupName = detail?.startupName.takeIf { !it.isNullOrBlank() } ?: user.name,
                    logoUrl = if (!user.profilePhotoUrl.isNullOrBlank()) user.profilePhotoUrl else detail?.logoUrl,
                    track = detail?.track ?: "",
                    sector = detail?.sector ?: "",
                    stage = detail?.stage ?: "",
                    fundingAsk = detail?.fundingAsk ?: "",
                    isVerified = detail?.isVerified ?: false,
                    description = detail?.description ?: "",
                    isDeleted = false
                )
            }
        }
    }

    // --- Lists (One-time fetch) ---

    suspend fun getAllMentors(): List<Mentor> {
        return db.collection("mentors").get().await().mapNotNull { 
            it.toObject<Mentor>()?.copy(uid = it.id) 
        }
    }

    suspend fun getAllInvestors(): List<Investor> {
        return db.collection("investors").get().await().mapNotNull { 
            it.toObject<Investor>()?.copy(uid = it.id) 
        }
    }
    
    suspend fun getAllStartups(): List<Startup> {
        return db.collection("startups").get().await().mapNotNull { 
            it.toObject<Startup>()?.copy(uid = it.id) 
        }
    }

    // --- Wall (Real-time) ---

    fun getWallPosts(filterType: String = "all", sector: String? = null, limit: Long = 20): Flow<List<WallPost>> = callbackFlow {
        log("getWallPosts: filter=$filterType, sector=$sector, limit=$limit")
        var query = db.collection("wallPosts")
            .whereEqualTo("active", true)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(limit) // Pagination limit

        if (filterType == "funding_call") {
            query = query.whereEqualTo("postType", "funding_call")
            if (sector != null && sector != "All") {
                query = query.whereEqualTo("sector", sector)
            }
        }

        val listener = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                log("ERROR: ${error.message}")
                if (error.code == com.google.firebase.firestore.FirebaseFirestoreException.Code.FAILED_PRECONDITION) {
                    log("CRITICAL: MISSING INDEX! Check Logcat for link.")
                }
                trySend(emptyList())
                return@addSnapshotListener
            }
            
            if (snapshot == null || snapshot.isEmpty) {
                log("Snapshot EMPTY. (Docs: 0)")
                trySend(emptyList())
                return@addSnapshotListener
            }

            log("Snapshot received. Docs: ${snapshot.size()}")

            val posts = snapshot.documents.mapNotNull { doc ->
                try {
                    val post = doc.toObject<WallPost>()?.copy(id = doc.id)
                    if (post == null) {
                        log("Parse FAIL: ${doc.id}")
                    } else {
                        // Validate poll options to catch ClassCastException early
                        if (post.postType == "poll") {
                            post.pollOptions.forEach { _ -> } // Iterate to trigger cast check
                        }
                    }
                    post
                } catch (e: Exception) {
                    log("Parse ERROR: ${doc.id} - ${e.message}")
                    android.util.Log.e("FirestoreRepo", "Failed to parse post ${doc.id}", e)
                    null
                }
            }
            trySend(posts)
        }
        awaitClose { listener.remove() }
    }

    suspend fun addWallPost(post: WallPost) {
        val start = System.currentTimeMillis()
        log("Adding post: ${post.id}")
        db.collection("wallPosts").document(post.id).set(post).await()
        log("Post added successfully: ${post.id}")
        logPerf("addWallPost", System.currentTimeMillis() - start)
    }

    // --- Comments & Upvotes ---

    fun getComments(postId: String): Flow<List<Comment>> = callbackFlow {
        val listener = db.collection("wallPosts").document(postId).collection("comments")
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    android.util.Log.e("FirestoreRepository", "Error in getComments listener", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val comments = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        doc.toObject<Comment>()?.copy(id = doc.id)
                    } catch (e: Exception) {
                        android.util.Log.e("FirestoreRepository", "Failed to parse Comment: ${doc.id}", e)
                        null
                    }
                } ?: emptyList()
                trySend(comments)
            }
        awaitClose { listener.remove() }
    }

    suspend fun addComment(postId: String, comment: Comment) {
        val start = System.currentTimeMillis()
        val postRef = db.collection("wallPosts").document(postId)
        val commentRef = postRef.collection("comments").document(comment.id)

        db.runTransaction { transaction ->
            val snapshot = transaction.get(postRef) // Read first
            val currentCount = snapshot.getLong("commentsCount") ?: 0
            
            transaction.set(commentRef, comment) // Then write
            transaction.update(postRef, "commentsCount", currentCount + 1)
        }.await()
        logPerf("addComment", System.currentTimeMillis() - start)
    }

    suspend fun toggleUpvote(postId: String, userId: String) {
        val start = System.currentTimeMillis()
        if (userId.isBlank() || postId.isBlank()) {
            android.util.Log.w("FirestoreRepository", "Invalid postId or userId for toggleUpvote")
            return
        }
        
        try {
            val postRef = db.collection("wallPosts").document(postId)
            val upvoteRef = postRef.collection("upvotes").document(userId)

            db.runTransaction { transaction ->
                val upvoteSnapshot = transaction.get(upvoteRef)
                val postSnapshot = transaction.get(postRef)
                val currentCount = postSnapshot.getLong("likesCount") ?: 0

                if (upvoteSnapshot.exists()) {
                    // Remove upvote
                    transaction.delete(upvoteRef)
                    transaction.update(postRef, "likesCount", maxOf(0, currentCount - 1))
                } else {
                    // Add upvote
                    transaction.set(upvoteRef, com.atmiya.innovation.data.Upvote(userId, com.google.firebase.Timestamp.now()))
                    transaction.update(postRef, "likesCount", currentCount + 1)
                }
            }.await()
            logPerf("toggleUpvote", System.currentTimeMillis() - start)
        } catch (e: Exception) {
            android.util.Log.e("FirestoreRepository", "Error in toggleUpvote", e)
            throw e // Re-throw to allow UI to handle
        }
    }

    suspend fun hasUserUpvoted(postId: String, userId: String): Boolean {
        val snapshot = db.collection("wallPosts").document(postId)
            .collection("upvotes").document(userId).get().await()
        return snapshot.exists()
    }

    // --- Polls ---

    suspend fun deleteWallPost(postId: String) {
        val start = System.currentTimeMillis()
        db.collection("wallPosts").document(postId).delete().await()
        logPerf("deleteWallPost", System.currentTimeMillis() - start)
    }

    suspend fun voteOnPoll(postId: String, userId: String, optionId: String) {
        val postRef = db.collection("wallPosts").document(postId)
        val voteRef = postRef.collection("poll_votes").document(userId)

        db.runTransaction { transaction ->
            val voteSnapshot = transaction.get(voteRef)
            if (voteSnapshot.exists()) {
                throw Exception("Already voted")
            }

            val postSnapshot = transaction.get(postRef)
            val post = postSnapshot.toObject<WallPost>() ?: throw Exception("Post not found")
            
            val updatedOptions = post.pollOptions.map { 
                if (it.id == optionId) it.copy(voteCount = it.voteCount + 1) else it 
            }

            transaction.update(postRef, "pollOptions", updatedOptions)
            transaction.set(voteRef, mapOf("optionId" to optionId, "votedAt" to Timestamp.now()))
        }.await()
    }

    suspend fun getPollVote(postId: String, userId: String): String? {
        if (userId.isBlank()) return null
        try {
            val snapshot = db.collection("wallPosts").document(postId)
                .collection("poll_votes").document(userId).get().await()
            return if (snapshot.exists()) snapshot.getString("optionId") else null
        } catch (e: Exception) {
            android.util.Log.e("FirestoreRepo", "Error getting poll vote", e)
            return null
        }
    }

    // --- Funding Calls ---

    fun getFundingCalls(
        filterType: String = "all", // "all", "my_calls", "sector"
        userId: String? = null, // For "my_calls"
        sector: String? = null, // For "sector" filter
        isAdmin: Boolean = false
    ): Flow<List<FundingCall>> = callbackFlow {
        log("getFundingCalls: filter=$filterType, user=$userId, sector=$sector, isAdmin=$isAdmin")
        
        var query = db.collection("fundingCalls")
            .orderBy("createdAt", Query.Direction.DESCENDING)

        // Admin sees all, even inactive
        // Others see only active - But relaxing query to avoid index issues.
        // UI filters will handle hiding.
        if (!isAdmin) {
             // query = query.whereEqualTo("isActive", true) // Relaxed
        }

        if (filterType == "my_calls" && userId != null) {
            query = query.whereEqualTo("investorId", userId)
        } else if (filterType == "sector" && sector != null) {
            query = query.whereArrayContains("sectors", sector)
        }

        val listener = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                log("getFundingCalls ERROR: ${error.message}")
                if (error.code == com.google.firebase.firestore.FirebaseFirestoreException.Code.FAILED_PRECONDITION) {
                    log("CRITICAL: MISSING INDEX for FundingCalls! Check Logcat for link.")
                }
                trySend(emptyList()) // Don't close flow, just send empty
                return@addSnapshotListener
            }
            val calls = snapshot?.documents?.mapNotNull { it.toObject<FundingCall>()?.copy(id = it.id) } ?: emptyList()
            trySend(calls)
        }
        awaitClose { listener.remove() }
    }
    
    suspend fun getFundingCall(callId: String): FundingCall? {
        return db.collection("fundingCalls").document(callId).get().await().toObject<FundingCall>()?.copy(id = callId)
    }

    // --- Admin User Management ---

    suspend fun getUsersByRole(role: String): List<User> {
        return db.collection("users")
            .whereEqualTo("role", role)
            // .whereEqualTo("isDeleted", false) // Removed to show all users including deleted
            .get().await()
            .mapNotNull { it.toObject<User>() }
    }

    suspend fun createUsersBatch(users: List<User>, role: String) {
        val batch = db.batch()
        users.forEach { user ->
            val userRef = db.collection("users").document(user.uid)
            batch.set(userRef, user)
            
            // Also create role-specific document
            when (role) {
                "startup" -> {
                    val startup = Startup(uid = user.uid, startupName = user.name, description = "Imported Startup")
                    val startupRef = db.collection("startups").document(user.uid)
                    batch.set(startupRef, startup)
                }
                "investor" -> {
                    val investor = Investor(uid = user.uid, name = user.name, firmName = "Imported Firm")
                    val investorRef = db.collection("investors").document(user.uid)
                    batch.set(investorRef, investor)
                }
                "mentor" -> {
                    val mentor = Mentor(uid = user.uid, name = user.name, title = "Imported Mentor")
                    val mentorRef = db.collection("mentors").document(user.uid)
                    batch.set(mentorRef, mentor)
                }
            }
        }
        batch.commit().await()
    }

    suspend fun updateUserStatus(userId: String, isBlocked: Boolean? = null, isDeleted: Boolean? = null) {
        val updates = mutableMapOf<String, Any>()
        if (isBlocked != null) updates["isBlocked"] = isBlocked
        if (isDeleted != null) updates["isDeleted"] = isDeleted
        
        if (updates.isNotEmpty()) {
            db.collection("users").document(userId).update(updates).await()
        }
    }

    // --- Imports ---

    fun getImports(): Flow<List<ImportRecord>> = callbackFlow {
        val listener = db.collection("imports")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val imports = snapshot?.documents?.mapNotNull { it.toObject<ImportRecord>()?.copy(id = it.id) } ?: emptyList()
                trySend(imports)
            }
        awaitClose { listener.remove() }
    }

    suspend fun createImportRecord(record: ImportRecord) {
        db.collection("imports").document(record.id).set(record).await()
    }
    


    suspend fun createFundingCall(call: FundingCall) {
        val start = System.currentTimeMillis()
        db.collection("fundingCalls").document(call.id).set(call).await()
        logPerf("createFundingCall", System.currentTimeMillis() - start)
    }

    suspend fun updateFundingCall(callId: String, updates: Map<String, Any>) {
        val start = System.currentTimeMillis()
        db.collection("fundingCalls").document(callId).update(updates).await()
        logPerf("updateFundingCall", System.currentTimeMillis() - start)
    }

    suspend fun applyToFundingCall(application: FundingApplication) {
        val start = System.currentTimeMillis()
        // Use subcollection: fundingCalls/{callId}/applications/{appId}
        db.collection("fundingCalls").document(application.callId)
            .collection("applications").document(application.id).set(application).await()
        
        // Trigger Notification if investorId is present
        if (application.investorId.isNotEmpty()) {
            try {
                val notification = Notification(
                    id = java.util.UUID.randomUUID().toString(),
                    userId = application.investorId,
                    type = "funding_application",
                    title = "New Application Received",
                    message = "You received a new application from ${application.startupName}",
                    referenceId = application.id,
                    senderId = application.startupId,
                    senderPhotoUrl = null, 
                    isRead = false,
                    createdAt = Timestamp.now()
                )
                db.collection("users").document(application.investorId)
                    .collection("notifications").document(notification.id).set(notification).await()
            } catch (e: Exception) {
                log("applyToFundingCall: Failed to send notification: ${e.message}")
            }
        }
        logPerf("applyToFundingCall", System.currentTimeMillis() - start)
    }

    fun getApplicationsForCall(callId: String): Flow<List<FundingApplication>> = callbackFlow {
        val listener = db.collection("fundingCalls").document(callId)
        .collection("applications")
        .orderBy("appliedAt", Query.Direction.DESCENDING)
        .addSnapshotListener { snapshot, error ->
            if (error != null) {
                log("getApplicationsForCall ERROR: ${error.message}")
                trySend(emptyList())
                return@addSnapshotListener
            }
            val apps = snapshot?.documents?.mapNotNull { it.toObject<FundingApplication>()?.copy(id = it.id) } ?: emptyList()
            trySend(apps)
        }
        awaitClose { listener.remove() }
    }

    suspend fun getApplication(callId: String, applicationId: String): FundingApplication? {
        return db.collection("fundingCalls").document(callId)
            .collection("applications").document(applicationId)
            .get().await().toObject<FundingApplication>()?.copy(id = applicationId)
    }

    // Fetch single application (requres callId for subcollection path)
    suspend fun getFundingApplication(callId: String, applicationId: String): FundingApplication? {
        return try {
            val snapshot = db.collection("fundingCalls").document(callId)
                .collection("applications").document(applicationId).get().await()
            snapshot.toObject<FundingApplication>()?.copy(id = snapshot.id)
        } catch (e: Exception) {
            log("getFundingApplication Error: ${e.message}")
            null
        }
    }

    suspend fun createTestFundingCall() {
        val id = java.util.UUID.randomUUID().toString()
        val call = FundingCall(
            id = id,
            title = "Seed Funding for AI Startups",
            investorName = "Venture Catalysts",
            description = "Looking for early-stage AI startups focusing on generative models.",
            sectors = listOf("AI", "SaaS"),
            minTicketAmount = "50L",
            maxTicketAmount = "2Cr",
            applicationDeadline = Timestamp(System.currentTimeMillis() / 1000 + 86400 * 7, 0), // 7 days from now
            isActive = true,
            createdAt = Timestamp.now()
        )
        db.collection("fundingCalls").document(id).set(call).await()
    }

    suspend fun hasApplied(callId: String, startupId: String): Boolean {
        try {
            val snapshot = db.collection("fundingCalls").document(callId)
                .collection("applications")
                .whereEqualTo("startupId", startupId)
                .limit(1)
                .get().await()
            return !snapshot.isEmpty
        } catch (e: Exception) {
            // If index is building or fails, assume false to allow screen to load
            android.util.Log.e("FirestoreRepo", "hasApplied check failed (Index building?)", e)
            return false
        }
    }

    fun getMyApplications(startupId: String): Flow<List<FundingApplication>> = callbackFlow {
        // Query the Collection Group "applications" to find all applications by this startup across all FundingCalls
        val listener = db.collectionGroup("applications")
            .whereEqualTo("startupId", startupId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    log("getMyApplications ERROR: ${error.message}")
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val apps = snapshot?.documents?.mapNotNull { it.toObject<FundingApplication>()?.copy(id = it.id) } ?: emptyList()
                trySend(apps)
            }
        awaitClose { listener.remove() }
    }

    suspend fun getFundingCalls(limit: Int = 20): List<FundingCall> {
        val start = System.currentTimeMillis()
        try {
            // Production logic - Relaxed to ensure data visibility
            // Filtering happens on UI side in FundingCallsScreen
            val snapshot = db.collection("fundingCalls")
                 //.whereEqualTo("isActive", true) 
                 .orderBy("createdAt", Query.Direction.DESCENDING) 
                .limit(limit.toLong())
                .get().await()
            logPerf("getFundingCalls", System.currentTimeMillis() - start)
            return snapshot.documents.mapNotNull { doc ->
                doc.toObject<FundingCall>()?.copy(id = doc.id)
            }
        } catch (e: Exception) {
            android.util.Log.e("FirestoreRepo", "Error fetching funding calls", e)
            return emptyList()
        }
    }

    suspend fun getRecommendedFundingCalls(sector: String, limit: Int = 5): List<FundingCall> {
        val start = System.currentTimeMillis()
        try {
            val snapshot = db.collection("fundingCalls")
                .whereEqualTo("isActive", true)
                .whereArrayContains("sectors", sector)
                .limit(limit.toLong())
                .get().await()
            logPerf("getRecommendedFundingCalls", System.currentTimeMillis() - start)
            return snapshot.documents.mapNotNull { doc ->
                doc.toObject<FundingCall>()?.copy(id = doc.id)
            }
        } catch (e: Exception) {
            android.util.Log.w("FirestoreRepo", "Index likely missing for recommended calls: ${e.message}")
            return getFundingCalls(20).filter { it.sectors.contains(sector) }
        }
    }

    // --- Mentor Videos ---

    suspend fun getMentorVideos(mentorId: String? = null): List<MentorVideo> {
        val query = if (mentorId != null) {
            db.collection("mentorVideos").whereEqualTo("mentorId", mentorId)
        } else {
            db.collection("mentorVideos")
        }
        return query.get().await().mapNotNull { it.toObject<MentorVideo>() }
    }
    
    suspend fun addMentorVideo(video: MentorVideo) {
        val start = System.currentTimeMillis()
        db.collection("mentorVideos").document(video.id).set(video).await()
        logPerf("addMentorVideo", System.currentTimeMillis() - start)
    }

    // --- Featured Videos ---

    suspend fun getFeaturedVideos(): List<FeaturedVideo> {
        val start = System.currentTimeMillis()
        val videos = db.collection("featuredVideos")
            .whereEqualTo("isActive", true)
            .orderBy("order")
            .limit(3)
            .get()
            .await()
            .mapNotNull { doc ->
                try {
                    doc.toObject<FeaturedVideo>()?.copy(id = doc.id)
                } catch (e: Exception) {
                    android.util.Log.e("FirestoreRepo", "Failed to parse FeaturedVideo: ${doc.id}", e)
                    null
                }
            }
        logPerf("getFeaturedVideos", System.currentTimeMillis() - start)
        return videos
    }

    // --- AIF Events ---

    suspend fun getAIFEvents(): List<AIFEvent> {
        val start = System.currentTimeMillis()
        try {
            val snapshot = db.collection("aifEvents").get().await()
            logPerf("getAIFEvents", System.currentTimeMillis() - start)
            return snapshot.documents.mapNotNull { doc ->
                doc.toObject<AIFEvent>()?.copy(id = doc.id)
            }
        } catch (e: Exception) {
            android.util.Log.e("FirestoreRepo", "Error fetching aifEvents: ${e.message}")
            return emptyList()
        }
    }



    suspend fun getAIFEvent(eventId: String): AIFEvent? {
        val start = System.currentTimeMillis()
        try {
            val doc = db.collection("aifEvents").document(eventId).get().await()
            if (doc.exists()) {
                logPerf("getAIFEvent", System.currentTimeMillis() - start)
                return doc.toObject<AIFEvent>()?.copy(id = eventId)
            }
        } catch (e: Exception) {
            android.util.Log.e("FirestoreRepo", "Error fetching aifEvent: ${e.message}")
        }
        return null
    }


    // --- Verification ---

    suspend fun getUserByPhone(phone: String): User? {
        // Note: This requires an index on 'phoneNumber' if collection is large, 
        // but for now simple query should work or auto-index creation link will appear in logs.
        val snapshot = db.collection("users")
            .whereEqualTo("phoneNumber", phone)
            .limit(1)
            .get().await()
        return snapshot.documents.firstOrNull()?.toObject<User>()
    }

    suspend fun updateStartupVerification(uid: String, isVerified: Boolean, adminId: String) {
        val updates = mapOf(
            "isVerified" to isVerified,
            "verifiedByAdminId" to adminId,
            "verifiedAt" to Timestamp.now()
        )
        db.collection("startups").document(uid).update(updates).await()
    }

    // --- Application Management ---

    suspend fun updateApplicationStatus(callId: String, applicationId: String, status: String) {
        db.collection("fundingCalls").document(callId)
            .collection("applications").document(applicationId)
            .update("status", status)
            .await()
    }

    // Fetch all applications for calls owned by this investor
    // Note: This requires a Collection Group Query if we want to fetch across all calls efficiently,
    // OR we can fetch calls first then fetch apps.
    // For MVP, we'll fetch applications per call (already implemented: getApplicationsForCall).
    
    // Fetch all applications made by a startup
    fun getMyApplications(startupId: String, onError: (String) -> Unit = {}): Flow<List<FundingApplication>> = callbackFlow {
        // Collection Group Query on 'applications' subcollection
        val listener = db.collectionGroup("applications")
            .whereEqualTo("startupId", startupId)
            .orderBy("appliedAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    log("getMyApplications ERROR: ${error.message}")
                    onError(error.message ?: "Unknown Firestore Error") // EXPOSE ERROR
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val apps = snapshot?.documents?.mapNotNull { it.toObject<FundingApplication>()?.copy(id = it.id) } ?: emptyList()
                trySend(apps)
            }
        awaitClose { listener.remove() }
    }

    // --- Chat System ---

    suspend fun createChatChannel(channel: ChatChannel): String {
        // Use deterministic ID: sort(uid1, uid2).join("_")
        val sortedIds = channel.participants.sorted()
        val channelId = "${sortedIds[0]}_${sortedIds[1]}"
        
        try {
            // Try to create the channel - use set() which creates or updates
            // We don't read first to avoid permission issues
            val newChannel = channel.copy(id = channelId, createdAt = Timestamp.now())
            
            // Use set with merge to not overwrite existing lastMessage/timestamps
            db.collection("chats").document(channelId)
                .set(newChannel, com.google.firebase.firestore.SetOptions.merge())
                .await()
        } catch (e: Exception) {
            // Channel might already exist or permission issue - log and continue
            android.util.Log.d("FirestoreRepo", "createChatChannel: ${e.message}")
        }
        
        return channelId
    }

    fun getChatChannels(userId: String): Flow<List<ChatChannel>> = callbackFlow {
        val listener = db.collection("chats")
            .whereArrayContains("participants", userId)
            .orderBy("lastMessageTimestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    log("getChatChannels ERROR: ${error.message}")
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val channels = snapshot?.documents?.mapNotNull { it.toObject<ChatChannel>()?.copy(id = it.id) } ?: emptyList()
                trySend(channels)
            }
        awaitClose { listener.remove() }
    }

    fun getMessages(channelId: String): Flow<List<ChatMessage>> = callbackFlow {
        val listener = db.collection("chats").document(channelId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    log("getMessages ERROR: ${error.message}")
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val messages = snapshot?.documents?.mapNotNull { it.toObject<ChatMessage>()?.copy(id = it.id) } ?: emptyList()
                trySend(messages)
            }
        awaitClose { listener.remove() }
    }

    suspend fun sendMessage(
        channelId: String, 
        message: ChatMessage, 
        participants: List<String> = emptyList(),
        participantNames: Map<String, String> = emptyMap(),
        participantPhotos: Map<String, String> = emptyMap()
    ) {
        val msgRef = db.collection("chats").document(channelId).collection("messages").document()
        val finalMessage = message.copy(id = msgRef.id, timestamp = Timestamp.now())
        
        val lastMsgPreview = when(message.mediaType) {
            "image" -> "📷 Photo"
            "video" -> "🎥 Video"
            "pdf" -> "📄 Document"
            "audio" -> "🎤 Audio"
            else -> message.text
        }
        
        val chatRef = db.collection("chats").document(channelId)

        db.runTransaction { transaction ->
            // Create message
            transaction.set(msgRef, finalMessage)
            
            // Check if chat exists, if not, create it with provided metadata
            val chatSnapshot = transaction.get(chatRef)
            if (!chatSnapshot.exists()) {
                val newChannel = ChatChannel(
                    id = channelId,
                    participants = participants,
                    participantNames = participantNames,
                    participantPhotos = participantPhotos,
                    lastMessage = lastMsgPreview,
                    lastMessageTimestamp = finalMessage.timestamp,
                    createdAt = Timestamp.now()
                )
                transaction.set(chatRef, newChannel)
            } else {
                // Just update last message
                transaction.update(chatRef, mapOf(
                    "lastMessage" to lastMsgPreview,
                    "lastMessageTimestamp" to finalMessage.timestamp
                ))
            }
        }.await()
    }

    suspend fun pinChat(channelId: String, userId: String, isPinned: Boolean) {
        val chatRef = db.collection("chats").document(channelId)
        if (isPinned) {
            chatRef.update("pinnedBy", com.google.firebase.firestore.FieldValue.arrayUnion(userId)).await()
        } else {
            chatRef.update("pinnedBy", com.google.firebase.firestore.FieldValue.arrayRemove(userId)).await()
        }
    }

    suspend fun archiveChat(channelId: String, userId: String, isArchived: Boolean) {
        val chatRef = db.collection("chats").document(channelId)
        if (isArchived) {
            chatRef.update("archivedBy", com.google.firebase.firestore.FieldValue.arrayUnion(userId)).await()
        } else {
            chatRef.update("archivedBy", com.google.firebase.firestore.FieldValue.arrayRemove(userId)).await()
        }
    }

    suspend fun markAsRead(channelId: String, userId: String) {
        // Simple logic: Reset unread count for this user in the map
        val chatRef = db.collection("chats").document(channelId)
        // Note: Map updates in Firestore for nested fields can be tricky.
        // Dot notation "unreadCounts.userId" works if the key is known and valid.
        chatRef.update("unreadCounts.$userId", 0).await()
    }
    
    // Real File Upload Implementation
    suspend fun uploadFile(uri: android.net.Uri, type: String): String {
        return try {
            val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
            val userId = auth.currentUser?.uid ?: return ""
            
            val storageRef = com.google.firebase.storage.FirebaseStorage.getInstance().reference
            val timestamp = System.currentTimeMillis()
            val fileName = "${timestamp}_${java.util.UUID.randomUUID()}"
            
            // Determine path based on type (mostly for organization, rules use chat_media/{userId})
            val path = "chat_media/$userId/$fileName"
            
            val fileRef = storageRef.child(path)
            
            // Put file
            fileRef.putFile(uri).await()
            
            // Get URL
            val downloadUrl = fileRef.downloadUrl.await()
            downloadUrl.toString()
        } catch (e: Exception) {
            android.util.Log.e("FirestoreRepo", "Upload failed", e)
            ""
        }
    }


    // --- Connection System ---

    // --- Connection System ---

    suspend fun sendConnectionRequest(sender: User, receiverId: String, receiverName: String, receiverRole: String, receiverPhotoUrl: String?) {
        val sortedIds = listOf(sender.uid, receiverId).sorted()
        val requestId = "${sortedIds[0]}_${sortedIds[1]}" // Deterministic ID ensures only ONE request/connection pair exists
        
        // Double check if already connected or pending to prevent overwrites (UI should prevent this, but safety first)
        val doc = db.collection("connectionRequests").document(requestId).get().await()
        if (doc.exists()) {
             val existingStatus = doc.getString("status")
             if (existingStatus == "accepted") throw Exception("Already connected")
             if (existingStatus == "pending") throw Exception("Request already pending")
             // If declined, we might allow re-requesting after check, but here we just overwrite for now as per "allow re-request" logic
        }

        val request = ConnectionRequest(
            id = requestId,
            senderId = sender.uid,
            senderName = sender.name,
            senderRole = sender.role,
            senderPhotoUrl = sender.profilePhotoUrl,
            receiverId = receiverId,
            receiverName = receiverName,
            receiverRole = receiverRole,
            receiverPhotoUrl = receiverPhotoUrl,
            status = "pending",
            createdAt = Timestamp.now(),
            updatedAt = Timestamp.now()
        )
        db.collection("connectionRequests").document(requestId).set(request).await()
    }

    // --- Saved Startup Ideas ---

    suspend fun saveStartupIdea(idea: StartupIdea) {
        val start = System.currentTimeMillis()
        if (idea.userId.isEmpty()) throw Exception("User ID is required to save idea")
        
        db.collection("users").document(idea.userId)
            .collection("saved_ideas").document(idea.id)
            .set(idea)
            .await()
        logPerf("saveStartupIdea", System.currentTimeMillis() - start)
    }

    fun getSavedStartupIdeas(userId: String): Flow<List<StartupIdea>> = callbackFlow {
        val listener = db.collection("users").document(userId)
            .collection("saved_ideas")
            .orderBy("generatedAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val ideas = snapshot?.documents?.mapNotNull { it.toObject<StartupIdea>() } ?: emptyList()
                trySend(ideas)
            }
        awaitClose { listener.remove() }
    }

    suspend fun acceptConnectionRequest(requestId: String) {
        db.collection("connectionRequests").document(requestId)
            .update(mapOf(
                "status" to "accepted",
                "updatedAt" to Timestamp.now()
            )).await()
    }

    suspend fun declineConnectionRequest(requestId: String) {
        db.collection("connectionRequests").document(requestId)
            .update(mapOf(
                "status" to "declined",
                "updatedAt" to Timestamp.now()
            )).await()
    }
    
    suspend fun cancelConnectionRequest(requestId: String) {
         db.collection("connectionRequests").document(requestId)
            .update(mapOf(
                "status" to "cancelled",
                "updatedAt" to Timestamp.now()
            )).await()
    }


    fun getIncomingConnectionRequests(userId: String): Flow<List<ConnectionRequest>> = callbackFlow {
        val listener = db.collection("connectionRequests")
            .whereEqualTo("receiverId", userId)
            .whereEqualTo("status", "pending")
            //.orderBy("createdAt", Query.Direction.DESCENDING) // Removed to avoid Index requirement issues
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val requests = snapshot?.documents?.mapNotNull { it.toObject<ConnectionRequest>() }
                    ?.sortedByDescending { it.createdAt }
                    ?: emptyList()
                trySend(requests)
            }
        awaitClose { listener.remove() }
    }

    suspend fun getSentConnectionRequests(userId: String): List<ConnectionRequest> {
        return db.collection("connectionRequests")
            .whereEqualTo("senderId", userId)
            .whereEqualTo("status", "pending")
            .get().await().toObjects(ConnectionRequest::class.java)
    }

    // Get all accepted connections (where user is either sender or receiver)
    suspend fun getAcceptedConnections(userId: String): List<ConnectionRequest> {
        // Query 1: Where I am sender
        val sent = db.collection("connectionRequests")
            .whereEqualTo("senderId", userId)
            .whereEqualTo("status", "accepted")
            .get().await().toObjects(ConnectionRequest::class.java)

        // Query 2: Where I am receiver
        val received = db.collection("connectionRequests")
            .whereEqualTo("receiverId", userId)
            .whereEqualTo("status", "accepted")
            .get().await().toObjects(ConnectionRequest::class.java)
            
        return sent + received
    }

    // Real-time Flow for Sent Requests
    fun getSentConnectionRequestsFlow(userId: String): Flow<List<ConnectionRequest>> = callbackFlow {
        val listener = db.collection("connectionRequests")
            .whereEqualTo("senderId", userId)
            .whereEqualTo("status", "pending")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val requests = snapshot?.documents?.mapNotNull { it.toObject<ConnectionRequest>() } ?: emptyList()
                trySend(requests)
            }
        awaitClose { listener.remove() }
    }

    // Real-time Flow for Accepted Connections
    fun getAcceptedConnectionsFlow(userId: String): Flow<List<ConnectionRequest>> {
        val sentFlow = callbackFlow {
            val listener = db.collection("connectionRequests")
                .whereEqualTo("senderId", userId)
                .whereEqualTo("status", "accepted")
                .addSnapshotListener { snapshot, error ->
                     val list = snapshot?.documents?.mapNotNull { it.toObject<ConnectionRequest>() } ?: emptyList()
                     trySend(list)
                }
            awaitClose { listener.remove() }
        }

        val receivedFlow = callbackFlow {
            val listener = db.collection("connectionRequests")
                .whereEqualTo("receiverId", userId)
                .whereEqualTo("status", "accepted")
                .addSnapshotListener { snapshot, error ->
                     val list = snapshot?.documents?.mapNotNull { it.toObject<ConnectionRequest>() } ?: emptyList()
                     trySend(list)
                }
            awaitClose { listener.remove() }
        }

        return kotlinx.coroutines.flow.combine(sentFlow, receivedFlow) { sent, received ->
            sent + received
        }
    }
    
    // Check status between two users (One-shot)
    suspend fun checkConnectionStatus(currentUserId: String, targetUserId: String): String {
        // Since we use deterministic IDs (sorted UIDs), we only need to check ONE document!
        val sortedIds = listOf(currentUserId, targetUserId).sorted()
        val requestId = "${sortedIds[0]}_${sortedIds[1]}"
        
        val snapshot = db.collection("connectionRequests").document(requestId).get().await()
        if (snapshot.exists()) {
             val status = snapshot.getString("status") ?: "none"
             val senderId = snapshot.getString("senderId")
             
             return when(status) {
                 "accepted" -> "connected"
                 "pending" -> {
                     if (senderId == currentUserId) "pending_sent" else "pending_received"
                 }
                 "declined" -> {
                     val updatedAt = snapshot.getTimestamp("updatedAt") ?: Timestamp.now()
                     val diffMillis = System.currentTimeMillis() - (updatedAt.seconds * 1000)
                     val days = diffMillis / (1000 * 60 * 60 * 24)
                     if (days < 7) "declined" else "none"
                 }
                 else -> "none"
             }
        }
        
        return "none"
    }


    // --- Feedback ---

    suspend fun addFeedback(feedback: Feedback) {
        val start = System.currentTimeMillis()
        db.collection("feedback").document(feedback.id).set(feedback).await()
        logPerf("addFeedback", System.currentTimeMillis() - start)
    }

    fun getFeedbackFlow(): Flow<List<Feedback>> = callbackFlow {
        val listener = db.collection("feedback")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val list = snapshot?.documents?.mapNotNull { it.toObject<Feedback>()?.copy(id = it.id) } ?: emptyList()
                trySend(list)
            }
        awaitClose { listener.remove() }
    }

    // --- Missing Investor Methods ---

    suspend fun hasAppliedToInvestor(investorId: String, startupId: String): Boolean {
        if (investorId.isEmpty() || startupId.isEmpty()) return false
        try {
            // 1. Fast Path: Check using the new optimized index
            val fastSnapshot = db.collectionGroup("applications")
                .whereEqualTo("investorId", investorId)
                .whereEqualTo("startupId", startupId)
                .limit(1)
                .get().await()
            
            if (!fastSnapshot.isEmpty) return true

            // 2. Slow Path (Fallback for Legacy Data): 
            // Fetch all applications by this startup and check the parent Funding Call's investor
            val allAppsSnapshot = db.collectionGroup("applications")
                .whereEqualTo("startupId", startupId)
                .get().await()

            for (doc in allAppsSnapshot.documents) {
                // Manually map to avoid crash if fields missing
                val callId = doc.getString("callId") ?: continue
                val appInvestorId = doc.getString("investorId") ?: ""
                
                // If investorId is present but didn't match in Fast Path, it's not us (or index lag). 
                // Skip if explicit mismatch.
                if (appInvestorId.isNotEmpty() && appInvestorId != investorId) continue

                // If investorId is missing (Legacy), fetch the call
                if (appInvestorId.isEmpty()) {
                    val call = getFundingCall(callId)
                    if (call != null && call.investorId == investorId) {
                        // Match found! Self-heal this document for future fast access
                        try {
                            doc.reference.update("investorId", investorId)
                        } catch (e: Exception) {
                            // Ignore update failure, just return true
                        }
                        return true
                    }
                }
            }
            
            return false
        } catch (e: Exception) {
            android.util.Log.e("FirestoreRepository", "hasAppliedToInvestor error", e)
            return false
        }
    }

    fun getFundingCallsForInvestorFlow(investorId: String): Flow<List<FundingCall>> = callbackFlow {
        val listener = db.collection("fundingCalls")
            .whereEqualTo("investorId", investorId)
            .addSnapshotListener { snapshot: QuerySnapshot?, error: FirebaseFirestoreException? ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val calls = snapshot?.documents?.mapNotNull { it.toObject<FundingCall>()?.copy(id = it.id) } 
                    ?.sortedByDescending { it.createdAt }
                    ?: emptyList()
                trySend(calls)
            }
        awaitClose { listener.remove() }
    }

    fun getApplicationsForCallsFlow(callIds: List<String>, onError: (String) -> Unit = {}): Flow<List<FundingApplication>> = callbackFlow {
        if (callIds.isEmpty()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        val activeIds = callIds.take(10)
        val listener = db.collectionGroup("applications")
            .whereIn("callId", activeIds)
            .addSnapshotListener { snapshot: QuerySnapshot?, error: FirebaseFirestoreException? ->
                if (error != null) {
                    onError(error.message ?: "Unknown Firestore Error") // EXPOSE ERROR
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val apps = snapshot?.documents?.mapNotNull { it.toObject<FundingApplication>()?.copy(id = it.id) } ?: emptyList()
                trySend(apps)
            }
        awaitClose { listener.remove() }
    }

    suspend fun getFundingApplication(id: String): FundingApplication? {
        val snapshot = db.collectionGroup("applications")
            .whereEqualTo("id", id)
            .limit(1)
            .get().await()
        return snapshot.documents.firstOrNull()?.toObject<FundingApplication>()?.copy(id = id)
    }

    suspend fun updateFundingApplicationStatus(id: String, status: String) {
        val snapshot = db.collectionGroup("applications")
            .whereEqualTo("id", id)
            .limit(1)
            .get().await()
        val doc = snapshot.documents.firstOrNull()
        if (doc != null) {
            doc.reference.update("status", status).await()
        }
    }
}
