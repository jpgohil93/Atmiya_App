package com.atmiya.innovation.repository

import com.atmiya.innovation.data.ExecutionPhase
import com.atmiya.innovation.data.GeneratorInputs
import com.atmiya.innovation.data.StartupIdea
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IdeaGeneratorRepository {

    // Ideally this key should be in BuildConfig or secure storage
    private val apiKey = "AIzaSyCPIMO09xrujmVdn0bFkHAY7SAzspfLHGg" 

    private val generativeModel = GenerativeModel(
        modelName = "gemini-2.5-flash",
        apiKey = apiKey,
        generationConfig = com.google.ai.client.generativeai.type.generationConfig {
            maxOutputTokens = 8192
        }
    )

    suspend fun generateStartupIdeas(inputs: GeneratorInputs): List<StartupIdea> = withContext(Dispatchers.IO) {
        val prompt = buildPrompt(inputs)
        
        // Let exceptions propagate to ViewModel for better error messages
        val response = generativeModel.generateContent(prompt)
        
        // Log token usage for monitoring
        response.usageMetadata?.let { usage ->
            android.util.Log.i("GenAI", "Token Usage - Input: ${usage.promptTokenCount}, Output: ${usage.candidatesTokenCount}, Total: ${usage.totalTokenCount}")
        }
        
        val responseText = response.text ?: return@withContext emptyList()
        parseIdeas(responseText)
    }

    private fun buildPrompt(inputs: GeneratorInputs): String {
        return """
            Generate 1 startup idea as JSON array. Keep responses brief.
            Sectors: ${inputs.selectedSectors.take(3).joinToString(",")}
            Skills: ${inputs.skills.take(50)}
            Budget: ${inputs.budgetRange}
            Solo: ${inputs.isSoloFounder}

            JSON only, no markdown:
            [{"name":"","oneLineSummary":"","problem":"","solution":"","businessModel":"","keyRisks":["",""],"executionPlan":[{"phaseName":"Phase 1","duration":"30 days","tasks":["",""]}]}]
        """.trimIndent()
    }

    private fun parseIdeas(jsonString: String): List<StartupIdea> {
        val ideas = mutableListOf<StartupIdea>()
        android.util.Log.d("GenAI", "Raw Response: $jsonString") // Log for debugging
        try {
            // Robust Regex to find the JSON Array
            val jsonRegex = Regex("\\[.*]", RegexOption.DOT_MATCHES_ALL)
            val matchResult = jsonRegex.find(jsonString)
            val cleanJson = matchResult?.value ?: jsonString // Fallback or use found match

            val gson = com.google.gson.Gson()
            val listType = object : com.google.gson.reflect.TypeToken<List<StartupIdea>>() {}.type
            val parsedList: List<StartupIdea> = gson.fromJson(cleanJson, listType)
            
            // Assign new UUIDs if not present (Gson might leave them null/default if JSON doesn't have them)
            // StartupIdea has default values, but Gson deserialization might bypass them if fields are missing in JSON but present in Class? 
            // Actually data class defaults work with Gson if using a specific instance creator or if the JSON is fully populated.
            // But here the JSON from AI won't have 'id' or 'userId'.
            // Simple sort: Map them to ensure IDs are fresh if needed, though default arg in constructor works if field missing in JSON?
            // Gson typically sets missing fields to null/0/false for primitives if not in JSON, or uses default if no-arg constructor?
            // StartupIdea has all default args so it has a no-arg constructor.
            // But safer to map to be sure.
            
            return parsedList.map { 
                if (it.id.isEmpty()) it.copy(id = java.util.UUID.randomUUID().toString(), generatedAt = com.google.firebase.Timestamp.now()) else it 
            }

        } catch (e: Exception) {
            e.printStackTrace()
            android.util.Log.e("GenAI", "Parsing Error", e)
            throw e // Re-throw so ViewModel sees "Parsing Error" instead of empty list
        }
    }


}
