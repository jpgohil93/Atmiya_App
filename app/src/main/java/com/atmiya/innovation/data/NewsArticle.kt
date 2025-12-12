package com.atmiya.innovation.data

data class NewsArticle(
    val title: String,
    val description: String?,
    val url: String,
    val imageUrl: String?,
    val sourceName: String,
    val publishedAt: String
)
