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

    init {
        loadPosts()
    }

    fun setFilter(type: String) {
        _filterType.value = type
        loadPosts()
    }

    fun setSector(sector: String) {
        _selectedSector.value = sector
        loadPosts()
    }

    fun refresh() {
        loadPosts()
    }

    private fun loadPosts() {
        viewModelScope.launch {
            _isLoading.value = true
            firestoreRepository.getWallPosts(_filterType.value, _selectedSector.value).collect {
                _posts.value = it
                _isLoading.value = false
            }
        }
    }

    fun createPost(
        context: Context, 
        content: String, 
        uri: Uri?, 
        isVideo: Boolean,
        pollQuestion: String? = null,
        pollOptions: List<String> = emptyList()
    ) {
        val user = auth.currentUser ?: return
        val postId = UUID.randomUUID().toString()
        
        viewModelScope.launch(Dispatchers.IO) {
            // 0. Pre-check media size
            if (uri != null) {
                try {
                    storageRepository.validateWallMedia(context, uri, isVideo)
                } catch (e: Exception) {
                    android.util.Log.e("WallViewModel", "Validation failed", e)
                    // TODO: Emit error state to UI
                    return@launch
                }
            }

            // 1. Optimistic Update
            val userProfile = firestoreRepository.getUser(user.uid)
            
            val finalPollOptions = pollOptions.map { 
                com.atmiya.innovation.data.PollOption(id = UUID.randomUUID().toString(), text = it, voteCount = 0) 
            }
            
            val optimisticPost = WallPost(
                id = postId,
                authorUserId = user.uid,
                authorName = userProfile?.name ?: "Anonymous",
                authorRole = userProfile?.role ?: "User",
                authorPhotoUrl = userProfile?.profilePhotoUrl,
                content = content,
                mediaType = if (uri != null) (if (isVideo) "video" else "image") else "none",
                mediaUrl = null, // Placeholder or local URI if possible, but null for now
                postType = if (pollQuestion != null) "poll" else "generic",
                pollQuestion = pollQuestion,
                pollOptions = finalPollOptions,
                isActive = true,
                createdAt = Timestamp.now(),
                likesCount = 0,
                commentsCount = 0
            )
            
            // Add to local list immediately (at top)
            _posts.value = listOf(optimisticPost) + _posts.value

            try {
                var mediaUrl: String? = null
                if (uri != null) {
                    mediaUrl = storageRepository.uploadWallMedia(context, postId, uri, isVideo)
                }

                val finalPost = optimisticPost.copy(mediaUrl = mediaUrl)
                android.util.Log.d("WallViewModel", "Creating post: $finalPost")
                firestoreRepository.addWallPost(finalPost)
                
                // Update local list with final post (though listener might handle this)
            } catch (e: Exception) {
                android.util.Log.e("WallViewModel", "Error creating post", e)
                // Revert optimistic update on failure
                _posts.value = _posts.value.filter { it.id != postId }
                
                withContext(Dispatchers.Main) {
                    android.widget.Toast.makeText(context, "Failed to create post: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
                }
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
}
