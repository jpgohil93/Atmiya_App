package com.atmiya.innovation.ui.dashboard

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.atmiya.innovation.data.Comment
import com.atmiya.innovation.data.WallPost
import com.atmiya.innovation.repository.FirestoreRepository
import com.atmiya.innovation.repository.StorageRepository
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

class WallViewModel : ViewModel() {
    private val firestoreRepository = FirestoreRepository()
    private val storageRepository = StorageRepository()
    private val auth = FirebaseAuth.getInstance()

    private val _posts = MutableStateFlow<List<WallPost>>(emptyList())
    val posts: StateFlow<List<WallPost>> = _posts.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _filterType = MutableStateFlow("all")
    val filterType: StateFlow<String> = _filterType.asStateFlow()

    private val _selectedSector = MutableStateFlow("All")
    val selectedSector: StateFlow<String> = _selectedSector.asStateFlow()

    private val _connections = MutableStateFlow<List<com.atmiya.innovation.data.User>>(emptyList())
    val connections: StateFlow<List<com.atmiya.innovation.data.User>> = _connections.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _postLimit = MutableStateFlow(20L)
    private var loadJob: kotlinx.coroutines.Job? = null

    init {
        loadPosts()
    }

    fun clearError() {
        _error.value = null
    }

    fun setFilter(type: String) {
        _filterType.value = type
        _postLimit.value = 20L // Reset limit on filter change
        if (type == "connections") {
            loadConnections()
        } else {
            loadPosts()
        }
    }

    fun setSector(sector: String) {
        _selectedSector.value = sector
        _postLimit.value = 20L // Reset limit on sector change
        loadPosts()
    }

    fun refresh() {
        _postLimit.value = 20L // Reset limit on refresh
        if (_filterType.value == "connections") {
            loadConnections()
        } else {
            loadPosts()
        }
    }

    fun loadMore() {
        if (_filterType.value != "connections") {
            _postLimit.value += 20
            loadPosts()
        }
    }

    private fun loadPosts() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _isLoading.value = true
            firestoreRepository.getWallPosts(_filterType.value, _selectedSector.value, _postLimit.value).collect {
                _posts.value = it
                _isLoading.value = false
            }
        }
    }

    private fun loadConnections() {
        viewModelScope.launch {
            _isLoading.value = true
            // Simulating connections by fetching all users for now
            // In a real app, this would be firestoreRepository.getConnections(userId)
            val startups = firestoreRepository.getUsersByRole("startup")
            val investors = firestoreRepository.getUsersByRole("investor")
            val mentors = firestoreRepository.getUsersByRole("mentor")
            
            _connections.value = (startups + investors + mentors).distinctBy { it.uid }
            _isLoading.value = false
        }
    }

    fun createPost(
        context: Context, 
        content: String, 
        mediaItems: List<Pair<Uri, Boolean>>, // Changed to list
        pollQuestion: String? = null,
        pollOptions: List<String> = emptyList()
    ) {
        val user = auth.currentUser ?: return
        val postId = UUID.randomUUID().toString()
        
        viewModelScope.launch(Dispatchers.IO) {
            // 0. Pre-check media size (check all)
            for ((uri, isVideo) in mediaItems) {
                try {
                    storageRepository.validateWallMedia(context, uri, isVideo)
                } catch (e: Exception) {
                    android.util.Log.e("WallViewModel", "Validation failed", e)
                    _error.value = e.message ?: "Validation failed"
                    return@launch
                }
            }

            // 1. Optimistic Update (Simplified: wait for upload for multi-media to ensure URLs are ready, or show placeholder)
            // For now, let's just do standard upload then add.
            
            val userProfile = firestoreRepository.getUser(user.uid)
            
            val finalPollOptions = pollOptions.map { 
                com.atmiya.innovation.data.PollOption(id = UUID.randomUUID().toString(), text = it, voteCount = 0) 
            }
            
            try {
                _isLoading.value = true
                
                val attachments = mutableListOf<com.atmiya.innovation.data.PostAttachment>()
                var mainMediaType = "none"
                var mainMediaUrl: String? = null
                var mainThumbnailUrl: String? = null

                // Upload all media
                mediaItems.forEachIndexed { index, (uri, isVideo) ->
                    val url = storageRepository.uploadWallMedia(context, postId + "_$index", uri, isVideo)
                    // TODO: Thumbnails for videos
                    
                    val type = if (isVideo) "video" else "image"
                    attachments.add(com.atmiya.innovation.data.PostAttachment(
                        id = UUID.randomUUID().toString(),
                        type = type,
                        url = url,
                        thumbnailUrl = null // Add logic if needed
                    ))
                    
                    // Set legacy fields for first item
                    if (index == 0) {
                        mainMediaType = type
                        mainMediaUrl = url
                        mainThumbnailUrl = null
                    }
                }

                val finalPost = WallPost(
                    id = postId,
                    authorUserId = user.uid,
                    authorName = userProfile?.name ?: "Anonymous",
                    authorRole = userProfile?.role ?: "User",
                    authorPhotoUrl = userProfile?.profilePhotoUrl,
                    content = content,
                    mediaType = mainMediaType, // Legacy support
                    mediaUrl = mainMediaUrl,   // Legacy support
                    thumbnailUrl = mainThumbnailUrl,
                    attachments = attachments,
                    postType = if (pollQuestion != null) "poll" else "generic",
                    pollQuestion = pollQuestion,
                    pollOptions = finalPollOptions,
                    isActive = true,
                    createdAt = Timestamp.now(),
                    likesCount = 0,
                    commentsCount = 0
                )
                
                android.util.Log.d("WallViewModel", "Creating post: $finalPost")
                firestoreRepository.addWallPost(finalPost)
                
                // Refresh to show new post
                loadPosts()
                
            } catch (e: Exception) {
                android.util.Log.e("WallViewModel", "Error creating post", e)
                _error.value = "Failed to create post: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun deletePost(postId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                firestoreRepository.deleteWallPost(postId)
                // Remove from local list
                _posts.value = _posts.value.filter { it.id != postId }
            } catch (e: Exception) {
                 android.util.Log.e("WallViewModel", "Error deleting post", e)
                _error.value = "Failed to delete post"
            }
        }
    }

    fun voteOnPoll(post: WallPost, optionId: String) {
        val user = auth.currentUser ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                firestoreRepository.voteOnPoll(post.id, user.uid, optionId)
            } catch (e: Exception) {
                android.util.Log.e("WallViewModel", "Error voting", e)
            }
        }
    }

    fun toggleLike(post: WallPost) {
        val user = auth.currentUser ?: return
        
        // Optimistic Update
        // We can't easily update the "isLiked" state here because it's fetched separately per post currently.
        // But we can update the count.
        // Ideally, we should track "likedByMe" in the Post object or a separate map.
        
        viewModelScope.launch(Dispatchers.IO) {
            firestoreRepository.toggleUpvote(post.id, user.uid)
        }
    }

    fun sendConnectionRequest(targetUser: com.atmiya.innovation.data.User, onSuccess: () -> Unit) {
        val currentUser = auth.currentUser ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val senderProfile = firestoreRepository.getUser(currentUser.uid)
                if (senderProfile != null) {
                    firestoreRepository.sendConnectionRequest(
                        sender = senderProfile,
                        receiverId = targetUser.uid,
                        receiverName = targetUser.name,
                        receiverRole = targetUser.role,
                        receiverPhotoUrl = targetUser.profilePhotoUrl
                    )
                    withContext(Dispatchers.Main) {
                        onSuccess()
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("WallViewModel", "Error sending connection request", e)
                _error.value = "Failed to send request: ${e.message}"
            }
        }
    }
}
