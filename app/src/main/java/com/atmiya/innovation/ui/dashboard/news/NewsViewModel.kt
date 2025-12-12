package com.atmiya.innovation.ui.dashboard.news

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.atmiya.innovation.data.NewsArticle
import com.atmiya.innovation.repository.NewsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class NewsViewModel : ViewModel() {
    private val repository = NewsRepository()

    private val _news = MutableStateFlow<List<NewsArticle>>(emptyList())
    val news: StateFlow<List<NewsArticle>> = _news

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error


    private var currentPage = 1
    private var canLoadMore = true
    private val pageSize = 10

    init {
        fetchNews(reset = true)
    }

    fun fetchNews(reset: Boolean = false) {
        if (_isLoading.value) return
        if (reset) {
            currentPage = 1
            canLoadMore = true
            _news.value = emptyList()
        }
        
        if (!canLoadMore) return

        viewModelScope.launch {
            _isLoading.value = true
            try {
                val newArticles = repository.getStartupNews(page = currentPage, pageSize = pageSize)
                if (newArticles.isEmpty()) {
                    canLoadMore = false
                } else {
                    val currentList = _news.value.toMutableList()
                    currentList.addAll(newArticles)
                    // Remove duplicates just in case API returns overlapping data or rotating keys return same
                    val uniqueList = currentList.distinctBy { it.title } // Deduplicate by Title
                    _news.value = uniqueList
                    currentPage++
                }
                _error.value = null
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to load news"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadMore() {
        if (canLoadMore && !_isLoading.value) {
            fetchNews(reset = false)
        }
    }
}
