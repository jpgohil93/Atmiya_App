package com.atmiya.innovation.logic

import com.atmiya.innovation.ui.onboarding.ChatMessage

class OnboardingManager(private val role: String, private val startupType: String?) {

    private var currentStep = 0
    private val answers = mutableMapOf<String, String>()

    // Define questions for each flow
    private val questions: List<Question> = when (role) {
        "startup" -> if (startupType == "edp") edpQuestions else acceleratorQuestions
        "investor" -> investorQuestions
        "mentor" -> mentorQuestions
        else -> emptyList()
    }

    fun getNextQuestion(): ChatMessage? {
        if (currentStep < questions.size) {
            val q = questions[currentStep]
            return ChatMessage(q.text, false, options = q.options)
        }
        return null
    }

    fun processAnswer(answer: String): ChatMessage? {
        if (currentStep < questions.size) {
            val q = questions[currentStep]
            answers[q.key] = answer
            currentStep++
            
            if (currentStep < questions.size) {
                return getNextQuestion()
            } else {
                // Onboarding Complete
                saveDataToFirestore()
                return ChatMessage("Thanks! Your profile is all set up.", false)
            }
        }
        return null
    }

    private fun saveDataToFirestore() {
        // TODO: Save 'answers' map to Firestore 'users' or specific collection
        val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
        val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
        val uid = auth.currentUser?.uid ?: return

        val collection = if (role == "startup") "startups" else "${role}s" // investors, mentors
        
        // Merge with existing data
        db.collection(collection).document(uid)
            .set(answers, com.google.firebase.firestore.SetOptions.merge())
    }

    companion object {
        data class Question(val key: String, val text: String, val options: List<String>? = null)

        val edpQuestions = listOf(
            Question("name", "What is your full name?"),
            Question("city", "Which city are you from?"),
            Question("sector", "Select your sector.", listOf("Agri-tech", "Health-tech", "Ed-tech", "Fin-tech", "Other")),
            Question("idea", "Tell me about your idea in a few sentences."),
            Question("stage", "What is your current stage?", listOf("Idea", "Concept", "Prototype")),
            Question("pitch_deck", "Please upload your Pitch Deck (PDF/PPT).")
        )

        val acceleratorQuestions = listOf(
            Question("startup_name", "What is the name of your startup?"),
            Question("founder_name", "What is the founder's name?"),
            Question("city", "Which city is your HQ?"),
            Question("sector", "Select your sector.", listOf("Agri-tech", "Health-tech", "Ed-tech", "Fin-tech", "Other")),
            Question("revenue", "What is your current monthly revenue (approx)?"),
            Question("team_size", "How many team members do you have?"),
            Question("pitch_deck", "Please upload your Pitch Deck (PDF/PPT).")
        )

        val investorQuestions = listOf(
            Question("name", "What is your full name?"),
            Question("firm_name", "What is your firm/organisation name?"),
            Question("ticket_size", "What is your typical ticket size range?"),
            Question("sectors", "Which sectors are you interested in?")
        )

        val mentorQuestions = listOf(
            Question("name", "What is your full name?"),
            Question("expertise", "What are your core areas of expertise?"),
            Question("experience", "How many years of experience do you have?")
        )
    }
}
