package com.atmiya.innovation.data

import com.google.firebase.Timestamp

data class GeneratorInputs(
    val selectedSectors: List<String> = emptyList(),
    val skills: String = "",
    val problemsToSolve: String = "",
    val budgetRange: String = "",
    val timeAvailability: String = "",
    val riskAppetite: String = "",
    val preferredModel: String = "",
    val geography: String = "",
    val isSoloFounder: Boolean = false,
    val isTechHeavy: Boolean = false
)

data class StartupIdea(
    val id: String = java.util.UUID.randomUUID().toString(),
    val userId: String = "", 
    val name: String = "",
    val oneLineSummary: String = "",
    val problem: String = "",
    val targetCustomer: String = "",
    val insight: String = "",
    val solution: String = "",
    val businessModel: String = "",
    val mvpDefinition: String = "",
    val executionPlan: List<ExecutionPhase> = emptyList(),
    val keyRisks: List<String> = emptyList(),
    val generatedAt: Timestamp = Timestamp.now()
)

data class ExecutionPhase(
    val phaseName: String = "",
    val duration: String = "",
    val tasks: List<String> = emptyList()
)
