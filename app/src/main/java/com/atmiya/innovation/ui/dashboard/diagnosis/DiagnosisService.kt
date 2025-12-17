package com.atmiya.innovation.ui.dashboard.diagnosis

import com.atmiya.innovation.BuildConfig
import com.atmiya.innovation.data.Startup
import com.atmiya.innovation.utils.GeminiRateLimitManager
import com.google.ai.client.generativeai.GenerativeModel
import com.google.gson.Gson

interface DiagnosisService {
    suspend fun generateDiagnosis(startup: Startup, userId: String = ""): DiagnosisResponse
    suspend fun generateAdvice(startup: Startup, diagnosis: DiagnosisResponse, userId: String = ""): String
}

class GeminiDiagnosisService : DiagnosisService {
    
    private val generativeModel = GenerativeModel(
        modelName = "gemini-2.5-flash-lite",
        apiKey = BuildConfig.GEMINI_API_KEY
    )

    override suspend fun generateDiagnosis(startup: Startup, userId: String): DiagnosisResponse {
        // Check rate limit before making API call
        val rateLimitResult = GeminiRateLimitManager.checkAndIncrementUsage(userId)
        if (rateLimitResult.isFailure) {
            val error = rateLimitResult.exceptionOrNull()
            throw error ?: Exception("Daily AI request limit reached. Please try again tomorrow.")
        }
        
        val prompt = buildDiagnosisPrompt(startup)
        val response = generativeModel.generateContent(prompt)
        val text = response.text ?: throw Exception("Empty response from AI")
        val cleanJson = cleanJson(text)
        return Gson().fromJson(cleanJson, DiagnosisResponse::class.java)
    }

    override suspend fun generateAdvice(startup: Startup, diagnosis: DiagnosisResponse, userId: String): String {
        // Check rate limit before making API call
        val rateLimitResult = GeminiRateLimitManager.checkAndIncrementUsage(userId)
        if (rateLimitResult.isFailure) {
            val error = rateLimitResult.exceptionOrNull()
            throw error ?: Exception("Daily AI request limit reached. Please try again tomorrow.")
        }
        
        val prompt = buildAdvicePrompt(startup, diagnosis)
        val response = generativeModel.generateContent(prompt)
        return response.text ?: "Could not generate advice."
    }

    private fun cleanJson(text: String): String {
        var cleaned = text.trim()
        if (cleaned.startsWith("```json")) {
            cleaned = cleaned.removePrefix("```json")
        }
        if (cleaned.startsWith("```")) {
            cleaned = cleaned.removePrefix("```")
        }
        if (cleaned.endsWith("```")) {
            cleaned = cleaned.removeSuffix("```")
        }
        return cleaned.trim()
    }

    private fun buildDiagnosisPrompt(startup: Startup): String {
        return """
            You are an expert startup mentor and VC analyst. 
            Analyze the following startup profile and provide a "Startup Diagnosis".
            
            **Startup Profile:**
            - Name: ${startup.startupName}
            - Sector: ${startup.sector}
            - Stage: ${startup.stage}
            - Description: ${startup.description}
            - Funding Ask: ${startup.fundingAsk.ifBlank { "Not specified" }}
            - Pitch Deck: ${if (startup.pitchDeckUrl.isNullOrBlank()) "Missing" else "Uploaded"}
            - Website: ${startup.website.ifBlank { "Not specified" }}
            - Team Size: ${startup.teamSize}
            
            **Instructions:**
            1. Identify **What's Missing** (3-5 items). Focus on missing signals like specific validation, clear GTM, or deck.
            2. Identify **Likely Failure Points** (max 3). Be specific to the sector/stage interact risks.
            3. Recommend **Focus Next** (2-3 items). Actionable, time-bound steps.
            
            **Format:**
            Return strictly a valid JSON object with these keys: "missing", "failure_points", "focus_next".
            Each key should be a list of strings.
            Do not include any conversational text outside the JSON.
        """.trimIndent()
    }

    private fun buildAdvicePrompt(startup: Startup, diagnosis: DiagnosisResponse): String {
        return """
            Context: Startup in ${startup.sector} at ${startup.stage} stage.
            Diagnosis:
            - Missing: ${diagnosis.missing.joinToString()}
            - Risks: ${diagnosis.failurePoints.joinToString()}
            
            Act as a supportive but direct mentor. 
            Write a short paragraph (4-6 sentences) of advice for the founder based on this diagnosis.
            Be constructive, practical, and encourage them to take the next step.
            Do not use buzzwords.
        """.trimIndent()
    }
}
