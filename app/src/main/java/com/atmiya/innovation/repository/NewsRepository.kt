package com.atmiya.innovation.repository

import android.util.Log
import com.atmiya.innovation.data.NewsArticle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

class NewsRepository {

    private val newsApiKeys = listOf(
        "eb85f8a414554ae5af7e35201fbd6bdb",
        "76d617da16a044389be96b2b3d4b27be"
    )
    private val gNewsKey = "29e712ec759739b23d3d3a79163683a8"

    suspend fun getStartupNews(page: Int = 1, pageSize: Int = 10): List<NewsArticle> = withContext(Dispatchers.IO) {
        // Try NewsAPI first with rotation
        for (key in newsApiKeys) {
            try {
                val articles = fetchFromNewsApi(key, page, pageSize)
                if (articles.isNotEmpty()) return@withContext articles
                // If list is empty but no error, maybe end of pagination? Return empty is fine.
            } catch (e: Exception) {
                Log.e("NewsRepository", "NewsAPI failed with key $key: ${e.message}")
                // Continue to next key
            }
        }

        // If all NewsAPI keys fail, try GNews (Fallback mechanism usually for initial load, 
        // but if paginating, GNews might not match 1:1. 
        // For simplicity, if we are on page 1, try GNews. If >1, maybe return empty if NewsAPI fails? 
        // Let's try GNews too.)
        try {
            // GNews doesn't support pageSize > 10 in free tier easily? 
            // It uses 'max' param default 10. 'page' param exists?
            // Docs: https://gnews.io/docs/v4#search-endpoint
            // "max" - number of news to return. Default 10. Max 100.
            // "page" - 1 by default.
            val articles = fetchFromGNews(gNewsKey, page, pageSize)
            if (articles.isNotEmpty()) return@withContext articles
        } catch (e: Exception) {
            Log.e("NewsRepository", "GNews failed: ${e.message}")
        }

        return@withContext emptyList()
    }

    private fun fetchFromNewsApi(apiKey: String, page: Int, pageSize: Int): List<NewsArticle> {
        val query = "startup%20india"
        val urlString = "https://newsapi.org/v2/everything?q=$query&language=en&sortBy=publishedAt&page=$page&pageSize=$pageSize&apiKey=$apiKey"
        
        val json = getJsonFromUrl(urlString)
        val articles = mutableListOf<NewsArticle>()
        
        if (json.has("status") && json.getString("status") == "error") {
             // If rate limited?
            throw Exception(json.optString("message", "Unknown error"))
        }

        val jsonArray = json.optJSONArray("articles") ?: return emptyList()

        for (i in 0 until jsonArray.length()) {
            val item = jsonArray.getJSONObject(i)
            val url = item.optString("url")
            // Skip removed articles and known bad domains
            if (item.optString("title") == "[Removed]" || url.contains("europesays.com")) continue

            articles.add(
                NewsArticle(
                    title = item.optString("title"),
                    description = item.optString("description"),
                    url = url,
                    imageUrl = item.optString("urlToImage").takeIf { it.isNotEmpty() && it != "null" },
                    sourceName = item.optJSONObject("source")?.optString("name") ?: "News",
                    publishedAt = item.optString("publishedAt")
                )
            )
        }
        return articles
    }

    private fun fetchFromGNews(apiKey: String, page: Int, pageSize: Int): List<NewsArticle> {
        val query = "startup india"
        // GNews uses 'max' for count, 'page' for page info is not clearly standard in free tier?
        // Docs say: "page" is available in some plans? 
        // Actually, free plan supports 'max' up to 10?
        // Let's try passing 'page' query param, which is standard in many APIs.
        val urlString = "https://gnews.io/api/v4/search?q=$query&lang=en&max=$pageSize&page=$page&apikey=$apiKey"
        
        val json = getJsonFromUrl(urlString)
        val articles = mutableListOf<NewsArticle>()

        val jsonArray = json.optJSONArray("articles") ?: return emptyList()

        for (i in 0 until jsonArray.length()) {
            val item = jsonArray.getJSONObject(i)
            val url = item.optString("url")
            // Filter out known bad sources or broken redirects
            if (url.contains("europesays.com") || url.contains("removed.com")) continue

            articles.add(
                NewsArticle(
                    title = item.optString("title"),
                    description = item.optString("description"),
                    url = url,
                    imageUrl = item.optString("image").takeIf { it.isNotEmpty() && it != "null" },
                    sourceName = item.optJSONObject("source")?.optString("name") ?: "News",
                    publishedAt = item.optString("publishedAt")
                )
            )
        }
        return articles
    }

    private fun getJsonFromUrl(urlString: String): JSONObject {
        val url = URL(urlString)
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.connectTimeout = 10000
        connection.readTimeout = 10000

        val responseCode = connection.responseCode
        if (responseCode == 429) {
             throw Exception("Rate Limit Exceeded")
        }
        if (responseCode != 200) {
            throw Exception("HTTP Error: $responseCode")
        }

        val reader = BufferedReader(InputStreamReader(connection.inputStream))
        val response = StringBuilder()
        var line: String?
        while (reader.readLine().also { line = it } != null) {
            response.append(line)
        }
        reader.close()
        return JSONObject(response.toString())
    }
}
