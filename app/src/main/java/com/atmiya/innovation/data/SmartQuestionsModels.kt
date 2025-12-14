package com.atmiya.innovation.data

import com.google.firebase.Timestamp

data class SmartQuestionsResponse(
    val startupName: String = "",
    val sector: String = "",
    val stage: String = "",
    val questions: Map<String, List<String>> = emptyMap()
)

data class QuestionCategory(
    val name: String,
    val questions: List<String>
)
