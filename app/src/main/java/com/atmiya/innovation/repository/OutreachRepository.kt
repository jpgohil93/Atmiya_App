package com.atmiya.innovation.repository

import com.atmiya.innovation.data.Startup
import com.atmiya.innovation.BuildConfig
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Singleton

@Singleton
class OutreachRepository {

    // Helper enums for inputs
    enum class OutreachType(val displayName: String) {
        COLD_EMAIL("Cold Email"),
        WHATSAPP("WhatsApp Intro"),
        LINKEDIN("LinkedIn DM"),
        FOLLOW_UP("Follow-up Message")
    }

    enum class OutreachStyle(val displayName: String, val promptInstruction: String) {
        PROFESSIONAL(
            "Professional",
            "Use standard, professional business English. Be polite, concise, and respectful of time."
        ),
        FOUNDER_TO_FOUNDER(
            "Founder-to-Founder",
            "Use a casual, peer-to-peer tone. Authentic, humble, and relatable. 'Building in public' vibe. Avoid stiff corporate speak."
        ),
        CONFIDENT(
            "Confident & Outcome-Focused",
            "Use bold, direct language. Focus heavily on traction, metrics, and results. Show conviction. Avoid hedging words like 'maybe' or 'trying'."
        ),
        PROBLEM_LED(
            "Problem-Led",
            "Start immediately with the specific user pain point or market gap. Hook the reader with the problem before introducing the solution."
        ),
        NATIVE(
            "Local / Native Language",
            "Write in the regional language popular in the startup's city (detected from 'Location'). IMPORTANT: Use a natural, professional business conversational tone (e.g., proper Hinglish, Gujlish, etc.). Do NOT use archaic, purely formal, or textbook words. Keep core technical terms like 'Startup', 'Revenue', 'Traction', 'Funding', 'Pitch', 'AI' in English. The goal is to sound like a modern local founder, not a translator."
        )
    }

    private val apiKey = BuildConfig.GEMINI_API_KEY

    private val generativeModel = GenerativeModel(
        modelName = "gemini-2.5-flash-lite",
        apiKey = apiKey
    )

    suspend fun generatePitch(
        startup: Startup,
        type: OutreachType,
        style: OutreachStyle,
        investorType: String? = null,
        city: String = "",
        founderName: String = ""
    ): String = withContext(Dispatchers.IO) {
        try {
            val prompt = buildPrompt(startup, type, style, investorType, city, founderName)
            val response = generativeModel.generateContent(prompt)
            return@withContext response.text?.trim() ?: "Error: No response generated."
        } catch (e: Exception) {
            return@withContext "Error generating message: ${e.localizedMessage}"
        }
    }

    private fun buildPrompt(
        startup: Startup,
        type: OutreachType,
        style: OutreachStyle,
        investorType: String?,
        city: String,
        founderName: String
    ): String {
        var instruction = style.promptInstruction
        
        if (style == OutreachStyle.NATIVE && city.isNotBlank()) {
            // Updated logic: Let Gemini detect the language for ANY city.
            instruction += " The startup is based in $city. **DETECTION TASK: First, identify the primary language spoken in $city (e.g., Kannada for Bangalore, Assamese for Guwahati, Odia for Bhubaneswar). Then, write the message primarily in that detected language** (or a natural mix of English and that language). Ensure the sentence structure is grammatically correct for that language but feels modern and usable. Do strictly NOT translate technical business terms."
        }

        return """
            You are an expert startup founder writing a concise, high-conversion outreach message to an investor.
            
            **Startup Details:**
            - Founder Name: ${founderName.ifBlank { "Founder" }}
            - Startup Name: ${startup.startupName}
            - Sector: ${startup.sector}
            - Stage: ${startup.stage}
            - Pitch: ${startup.description}
            - Ask: ${startup.fundingAsk.ifBlank { "Not specified" }}
            - Location: ${if (city.isNotBlank()) city else "Not specified"}
            
            **Task:**
            Write a ${type.displayName} for a ${investorType ?: "Potential Investor"}.
            
            **Style & Tone Instructions:**
            - **Style:** ${style.displayName}
            - **Instruction:** $instruction
            
            **Constraints:**
            - **Intro:** Start with a brief intro using the Founder Name (e.g., "Hi, I'm [Name] from [Startup Name]...").
            - Length: ${if (type == OutreachType.COLD_EMAIL) "Keep it under 150 words (6-8 lines)" else "Keep it under 50 words (3-5 lines)"}
            - Formatting: Plain text only. No subjects, headers, or markdown.
            - Content: Clearly state what problem is solved and why it matters. Include a soft call to action.
            - ${if (type == OutreachType.WHATSAPP) "Allow max 1 emoji." else "No emojis."}
            - Avoid generic buzzwords like "revolutionary" or "disruptive".
            
            Generate ONE good message option directly.
        """.trimIndent()
    }
}
