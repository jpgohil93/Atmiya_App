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
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirestoreRepository {

    private val db = FirebaseFirestore.getInstance()
    
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

    suspend fun getUser(uid: String): User? {
        val start = System.currentTimeMillis()
        val user = db.collection("users").document(uid).get().await().toObject<User>()
        logPerf("getUser", System.currentTimeMillis() - start)
        return user
    }

    suspend fun updateUser(uid: String, data: Map<String, Any>) {
        db.collection("users").document(uid).update(data).await()
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

    fun getWallPosts(filterType: String = "all", sector: String? = null): Flow<List<WallPost>> = callbackFlow {
        log("getWallPosts: filter=$filterType, sector=$sector")
        var query = db.collection("wallPosts")
            .whereEqualTo("active", true)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(20) // Pagination limit

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

        // Admin sees all, even inactive (unless filtered otherwise in UI, but here we fetch base)
        // Others see only active
        if (!isAdmin) {
            query = query.whereEqualTo("active", true)
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

    suspend fun applyToFundingCall(application: FundingApplication) {
        val start = System.currentTimeMillis()
        // Use subcollection: fundingCalls/{callId}/applications/{appId}
        db.collection("fundingCalls").document(application.callId)
            .collection("applications").document(application.id).set(application).await()
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

    suspend fun hasApplied(callId: String, startupId: String): Boolean {
        val snapshot = db.collection("fundingCalls").document(callId)
            .collection("applications")
            .whereEqualTo("startupId", startupId)
            .limit(1)
            .get().await()
        return !snapshot.isEmpty
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
    fun getMyApplications(startupId: String): Flow<List<FundingApplication>> = callbackFlow {
        // Collection Group Query on 'applications' subcollection
        val listener = db.collectionGroup("applications")
            .whereEqualTo("startupId", startupId)
            .orderBy("appliedAt", Query.Direction.DESCENDING)
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

    // --- Chat System ---

    suspend fun createChatChannel(channel: ChatChannel): String {
        // Check if channel already exists between these 2 users
        // This requires a complex query or a deterministic ID.
        // Let's use deterministic ID: sort(uid1, uid2).join("_")
        val sortedIds = channel.participants.sorted()
        val channelId = "${sortedIds[0]}_${sortedIds[1]}"
        
        val existing = db.collection("chats").document(channelId).get().await()
        if (!existing.exists()) {
            val newChannel = channel.copy(id = channelId, createdAt = Timestamp.now())
            db.collection("chats").document(channelId).set(newChannel).await()
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

    suspend fun sendMessage(channelId: String, message: ChatMessage) {
        val msgRef = db.collection("chats").document(channelId).collection("messages").document()
        val finalMessage = message.copy(id = msgRef.id, timestamp = Timestamp.now())
        
        db.runTransaction { transaction ->
            transaction.set(msgRef, finalMessage)
            transaction.update(db.collection("chats").document(channelId), mapOf(
                "lastMessage" to finalMessage.text,
                "lastMessageTimestamp" to finalMessage.timestamp
            ))
        }.await()
    }
}
