package com.atmiya.innovation.repository

import android.util.Log
import com.atmiya.innovation.data.SmartQuestionsResponse
import com.atmiya.innovation.data.Startup
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.generationConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Singleton
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

@Singleton
class SmartQuestionsRepository {

    private val apiKey = com.atmiya.innovation.BuildConfig.GEMINI_API_KEY

    private val generativeModel = GenerativeModel(
        modelName = "gemini-2.5-flash-lite",
        apiKey = apiKey,
        generationConfig = generationConfig {
            responseMimeType = "application/json"
        }
    )

    suspend fun generateSmartQuestions(startup: Startup, pitchSummary: String): Result<SmartQuestionsResponse> = withContext(Dispatchers.IO) {
        val prompt = buildPrompt(startup, pitchSummary)
        
        try {
            val response = generativeModel.generateContent(prompt)
            val responseText = response.text
                ?: return@withContext Result.failure(Exception("Gemini response was empty."))
            
            Log.d("SmartQuestions", "Raw Response: $responseText")
            
            val parsed = parseResponse(responseText)
            if (parsed != null) {
                Result.success(parsed)
            } else {
                Result.failure(Exception("Failed to parse JSON response."))
            }
        } catch (e: Exception) {
            Log.e("SmartQuestions", "Generation Error", e)
            Result.failure(e)
        }
    }

    private fun buildPrompt(startup: Startup, pitchSummary: String): String {
        return """
            You are an expert venture capitalist assistant. Analyze the following startup and generate smart due diligence questions for an investor.

            **Startup Context:**
            - Name: ${startup.startupName}
            - Sector: ${startup.sector}
            - Stage: ${startup.stage}
            - Description/Pitch: $pitchSummary
            
            **Task:**
            Generate exactly 10 high-quality, relevant questions grouped into 5 strict categories.
            
            **Categories:**
            1. Market
            2. Moat
            3. Revenue
            4. Execution Risk
            5. Founder Clarity
            
            **Constraints:**
            - Exactly 2 questions per category.
            - Questions must be specific to the startup's context (sector/stage) if possible.
            - Questions must be direct and non-generic.
            
            **Output Format:**
            Return strictly valid JSON matching this schema:
            {
              "startupName": "${startup.startupName}",
              "sector": "${startup.sector}",
              "stage": "${startup.stage}",
              "questions": {
                "Market": ["Q1", "Q2"],
                "Moat": ["Q1", "Q2"],
                "Revenue": ["Q1", "Q2"],
                "Execution Risk": ["Q1", "Q2"],
                "Founder Clarity": ["Q1", "Q2"]
              }
            }
        """.trimIndent()
    }

    private fun parseResponse(jsonString: String): SmartQuestionsResponse? {
        return try {
            val gson = Gson()
            val type = object : TypeToken<SmartQuestionsResponse>() {}.type
            
            // 1. Try simple cleanup
            var cleanJson = jsonString.replace("```json", "").replace("```", "").trim()
            
            // 2. If simple cleanup fails or leaves garbage, try Regex extraction
            if (!cleanJson.startsWith("{")) {
                 val jsonRegex = Regex("\\{.*\\}", RegexOption.DOT_MATCHES_ALL)
                 val matchResult = jsonRegex.find(jsonString)
                 if (matchResult != null) {
                     cleanJson = matchResult.value
                 }
            }

            gson.fromJson(cleanJson, type)
        } catch (e: Exception) {
            Log.e("SmartQuestions", "JSON Parsing Error for: $jsonString", e)
            null
        }
    }
}
