package com.atmiya.innovation.ui.dashboard.startup

import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.pow

// --- Data Models ---

enum class StartupStage(val label: String, val hasRevenue: Boolean) {
    IDEA_PRE_REVENUE("Idea / Pre-Revenue", false),
    MVP_BUILDING("MVP Building", false),
    PILOT_USERS("Pilot Users", false), // Can be pre or tiny revenue, but we handle "revenue" toggle in UI
    EARLY_REVENUE("Early Revenue", true)
}

data class OneTimeCostItem(
    val id: Long = System.currentTimeMillis(),
    val name: String,
    val amount: Double,
    val monthOffset: Int, // 0 to 12
    val isOptional: Boolean = true
)

data class HirePlanItem(
    val id: Long = System.currentTimeMillis(),
    val role: String,
    val monthlyCost: Double,
    val startMonthOffset: Int // 0 to 23
)

enum class ValidationStatus {
    ACHIEVABLE, AT_RISK, NOT_ACHIEVABLE, NA
}

data class LeverAction(
    val description: String,
    val runwayDelta: Int, // Additional months
    val type: LeverType
)

enum class LeverType {
    REDUCE_FIXED, REDUCE_VARIABLE, REDUCE_BUDGET, DELAY_COST, CASH_INJECTION
}

data class CalculationResult(
    val runwayMonths: Int, // 24 = 24+ (max)
    val zeroCashMonthApprox: String, // "Dec 2025"
    val monthlyBurnBreakdown: BurnBreakdown,
    val validationStatus: ValidationStatus,
    val scenarios: Scenarios,
    val levers: List<LeverAction>,
    val cashFlowHistory: List<Double> // For chart if needed, 0..runway
)

data class BurnBreakdown(
    val grossBurn: Double,
    val netBurn: Double,
    val oneTimeTotal: Double,
    val hireMonthlyAdditions: Double
)

data class Scenarios(
    val conservative: Int,
    val aggressive: Int
)

data class InputState(
    val stage: StartupStage = StartupStage.IDEA_PRE_REVENUE,
    
    // Common
    val cashInBank: Double = 0.0,
    val fixedMonthlyCosts: Double = 0.0,
    val variableMonthlyCosts: Double = 0.0,
    val bufferPercent: Int = 10,
    
    // Revenue (Pilot/Early)
    val hasRevenue: Boolean = false, // Toggle for Pilot
    val currentMonthlyRevenue: Double = 0.0,
    val grossMarginPercent: Double? = null,
    val cogsPercent: Double? = null,
    val revenueGrowthRate: Double = 0.0,
    val collectionDelayDays: Int = 0, // 0, 7, 15, 30
    
    // Stage Specific Budgets
    val validationTargetDays: Int? = null, // 30, 60, 90
    val monthlyResearchBudget: Double = 0.0,
    val monthlyLandingPageBudget: Double = 0.0,
    val monthlyPrototypeBudget: Double = 0.0,
    
    val mvpTargetDays: Int? = null, // 30, 60, 90
    val mvpMonthlySpend: Double = 0.0,
    val infraMonthlyCost: Double = 0.0,
    val pilotMarketingBudget: Double = 0.0,
    
    val pilotOpsCost: Double = 0.0,
    val customerAcquisitionBudget: Double = 0.0,
    
    // Lists
    val oneTimeCosts: List<OneTimeCostItem> = emptyList(),
    val plannedHires: List<HirePlanItem> = emptyList()
)

// --- Engine ---

object RunwayCalculatorEngine {

    fun calculate(inputs: InputState): CalculationResult {
        // 1. Base Simulation
        val baseRunway = simulateRunway(inputs)
        
        // 2. Scenarios
        val conservativeInputs = applyConservativeScenario(inputs)
        val conservativeRunway = simulateRunway(conservativeInputs)
        
        val aggressiveInputs = applyAggressiveScenario(inputs)
        val aggressiveRunway = simulateRunway(aggressiveInputs)
        
        // 3. Validation Status
        val validationStatus = calculateValidationStatus(inputs, baseRunway)
        
        // 4. Levers
        val levers = generateLevers(inputs, baseRunway)
        
        // 5. Breakdown (Snapshot at Month 0)
        val breakdown = calculateBreakdown(inputs)
        
        // 6. Zero Cash Date
        // Simple approx: Current Month + runway
        // We will return a relative string or we can let UI format it.
        // For engine, let's return "+X months"
        val zeroCashText = if (baseRunway >= 24) "> 24 Months" else "+ $baseRunway Months"

        return CalculationResult(
            runwayMonths = baseRunway,
            zeroCashMonthApprox = zeroCashText,
            monthlyBurnBreakdown = breakdown,
            validationStatus = validationStatus,
            scenarios = Scenarios(conservativeRunway, aggressiveRunway),
            levers = levers,
            cashFlowHistory = emptyList() // Not needed for specific UI requirements yet
        )
    }

    private fun simulateRunway(inputs: InputState): Int {
        var cash = inputs.cashInBank
        val maxMonths = 24
        
        // Monthly Recurring Burn (Base)
        val bufferMultiplier = 1 + (inputs.bufferPercent / 100.0)
        val adjustedFixed = inputs.fixedMonthlyCosts * bufferMultiplier
        val adjustedVariable = inputs.variableMonthlyCosts * bufferMultiplier
        
        // Stage specific ongoing burn adjustments
        // Validation/MVP specific budgets usually last for the target duration, 
        // but for simplicity and safety, we assume they might continue or user wants to see runway IF they continue.
        // However, requirements say "Validation monthly budgets... Add to netBurn during those months."
        
        for (month in 0 until maxMonths) {
            // 1. Calculate Revenue for this month
            val monthlyRevenueNet = calculateNetRevenue(inputs, month)
            
            // 2. Calculate Base Burn
            var monthOut = adjustedFixed + adjustedVariable
            
            // 3. Add Stage Specific Temporary Budgets
            monthOut += getStageSpecificMonthlySpend(inputs, month)
            
            // 4. Add Planned Hires
            inputs.plannedHires.forEach { hire ->
                if (month >= hire.startMonthOffset) {
                    monthOut += hire.monthlyCost
                }
            }
            
            // 5. Net Month Flow (before one-time)
            val netOpBurn = max(0.0, monthOut - monthlyRevenueNet)
            
            // 6. One Time Costs
            val oneTimeForMonth = inputs.oneTimeCosts
                .filter { it.monthOffset == month }
                .sumOf { it.amount }
                
            val totalMonthBurn = netOpBurn + oneTimeForMonth
            
            cash -= totalMonthBurn
            
            if (cash < 0) {
                return month // Ran out during this month
            } else if (cash == 0.0) {
                return month + 1 // Just finished this month exactly
            }
        }
        
        return maxMonths // Survived 24+ months
    }
    
    private fun calculateNetRevenue(inputs: InputState, month: Int): Double {
        if (!inputs.hasRevenue && inputs.stage != StartupStage.EARLY_REVENUE) return 0.0
        
        // Growth logic
        val growthRate = inputs.revenueGrowthRate / 100.0
        val revenue = inputs.currentMonthlyRevenue * (1 + growthRate).pow(month.toDouble())
        
        // Margin logic
        return if (inputs.grossMarginPercent != null) {
            revenue * (inputs.grossMarginPercent!! / 100.0)
        } else if (inputs.cogsPercent != null) {
            revenue * (1 - (inputs.cogsPercent!! / 100.0))
        } else {
            revenue // Default to 100% margin if not specified (should be validated in UI)
        }
    }
    
    private fun getStageSpecificMonthlySpend(inputs: InputState, month: Int): Double {
        var extra = 0.0
        
        // IDEA stage validaton budgets
        if (inputs.stage == StartupStage.IDEA_PRE_REVENUE) {
            val targetDays = inputs.validationTargetDays ?: 0
            val targetMonths = ceil(targetDays / 30.0).toInt()
            if (month < targetMonths) {
                extra += inputs.monthlyResearchBudget
                extra += inputs.monthlyLandingPageBudget
                extra += inputs.monthlyPrototypeBudget
            }
        }
        
        // MVP stage budgets
        if (inputs.stage == StartupStage.MVP_BUILDING) {
            val targetDays = inputs.mvpTargetDays ?: 0
            val targetMonths = ceil(targetDays / 30.0).toInt()
            if (month < targetMonths) {
                extra += inputs.mvpMonthlySpend
                extra += inputs.pilotMarketingBudget
            }
            // Infra usually ongoing? Assuming ongoing for MVP
            extra += inputs.infraMonthlyCost
        }
        
        // Pilot stage
        if (inputs.stage == StartupStage.PILOT_USERS) {
            // Ongoing pilot ops
            extra += inputs.pilotOpsCost
            extra += inputs.customerAcquisitionBudget
        }
        
        // Early Revenue
        // No specific extra budgets defined in requirements as "temporary", usually captured in burn.
        
        return extra
    }

    private fun calculateBreakdown(inputs: InputState): BurnBreakdown {
        val bufferMultiplier = 1 + (inputs.bufferPercent / 100.0)
        val gross = (inputs.fixedMonthlyCosts + inputs.variableMonthlyCosts) * bufferMultiplier
        val oneTime = inputs.oneTimeCosts.sumOf { it.amount }
        
        // Net at month 0
        val rev = calculateNetRevenue(inputs, 0)
        val stageSpend = getStageSpecificMonthlySpend(inputs, 0)
        val net = max(0.0, gross + stageSpend - rev)
        
        // Hires (none at month 0 usually, but if offset 0)
        val hires = inputs.plannedHires.filter { it.startMonthOffset == 0 }.sumOf { it.monthlyCost }
        
        return BurnBreakdown(gross + stageSpend, net, oneTime, hires)
    }
    
    private fun calculateValidationStatus(inputs: InputState, runway: Int): ValidationStatus {
        val targetDays = when (inputs.stage) {
            StartupStage.IDEA_PRE_REVENUE -> inputs.validationTargetDays
            StartupStage.MVP_BUILDING -> inputs.mvpTargetDays
            else -> null
        } ?: return ValidationStatus.NA
        
        val targetMonths = ceil(targetDays / 30.0).toInt()
        
        return when {
            runway >= targetMonths + 2 -> ValidationStatus.ACHIEVABLE // Comfortable
            runway >= targetMonths -> ValidationStatus.AT_RISK // Just enough
            else -> ValidationStatus.NOT_ACHIEVABLE
        }
    }
    
    // --- Scenarios ---
    
    private fun applyConservativeScenario(inputs: InputState): InputState {
        return inputs.copy(
            bufferPercent = (inputs.bufferPercent + 5).coerceAtMost(25),
            variableMonthlyCosts = max(0.0, inputs.variableMonthlyCosts * 0.9),
            // Reduce stage budgets by 10%
            monthlyResearchBudget = inputs.monthlyResearchBudget * 0.9,
            monthlyLandingPageBudget = inputs.monthlyLandingPageBudget * 0.9,
            monthlyPrototypeBudget = inputs.monthlyPrototypeBudget * 0.9,
            mvpMonthlySpend = inputs.mvpMonthlySpend * 0.9,
            pilotMarketingBudget = inputs.pilotMarketingBudget * 0.9,
            customerAcquisitionBudget = inputs.customerAcquisitionBudget * 0.9
        )
    }
    
    private fun applyAggressiveScenario(inputs: InputState): InputState {
        // Increase budgets
        val factor = 1.15
        
        // Reorder One-time (move largest non-optional 1 month earlier)
        // Requirements: "move the largest one-time cost 1 month earlier ONLY if that cost is marked “non-optional” is false" -> Wait vs Prompt
        // Prompt says: "Optional earlier one-time spend... ONLY if that cost is marked “non-optional” is false" -> Double negative? 
        // "marked 'non-optional' is false" means it IS optional? 
        // Usually aggressive means spending faster.
        // Let's assume we pull forward largest optional cost.
        
        val newOneTime = inputs.oneTimeCosts.map { it.copy() }.toMutableList()
        val largestOptional = newOneTime.filter { it.isOptional && it.monthOffset > 0 }.maxByOrNull { it.amount }
        
        if (largestOptional != null) {
            val index = newOneTime.indexOf(largestOptional)
            if (index != -1) {
                newOneTime[index] = largestOptional.copy(monthOffset = max(0, largestOptional.monthOffset - 1))
            }
        }

        return inputs.copy(
            bufferPercent = max(0, inputs.bufferPercent - 5),
            monthlyResearchBudget = inputs.monthlyResearchBudget * factor,
            monthlyLandingPageBudget = inputs.monthlyLandingPageBudget * factor,
            monthlyPrototypeBudget = inputs.monthlyPrototypeBudget * factor,
            mvpMonthlySpend = inputs.mvpMonthlySpend * factor,
            pilotMarketingBudget = inputs.pilotMarketingBudget * factor,
            customerAcquisitionBudget = inputs.customerAcquisitionBudget * factor,
            oneTimeCosts = newOneTime
        )
    }

    // --- Levers ---
    
    private fun generateLevers(inputs: InputState, baseRunway: Int): List<LeverAction> {
        val levers = mutableListOf<LeverAction>()
        
        // 1. Reduce Fixed Costs by 10%
        val reducedFixed = inputs.copy(fixedMonthlyCosts = inputs.fixedMonthlyCosts * 0.9)
        val r1 = simulateRunway(reducedFixed)
        if (r1 > baseRunway) {
            levers.add(LeverAction("Reduce fixed costs by 10%", r1 - baseRunway, LeverType.REDUCE_FIXED))
        }
        
        // 2. Reduce Variable Costs by 20%
        if (inputs.variableMonthlyCosts > 0) {
            val reducedVar = inputs.copy(variableMonthlyCosts = inputs.variableMonthlyCosts * 0.8)
            val r2 = simulateRunway(reducedVar)
            if (r2 > baseRunway) {
                levers.add(LeverAction("Reduce variable costs by 20%", r2 - baseRunway, LeverType.REDUCE_VARIABLE))
            }
        }
        
        // 3. Reduce Stage Budgets by 20%
        // Only if stage has them
        val reducedBudgets = inputs.copy(
            monthlyResearchBudget = inputs.monthlyResearchBudget * 0.8,
            monthlyLandingPageBudget = inputs.monthlyLandingPageBudget * 0.8,
            monthlyPrototypeBudget = inputs.monthlyPrototypeBudget * 0.8,
            mvpMonthlySpend = inputs.mvpMonthlySpend * 0.8,
            pilotMarketingBudget = inputs.pilotMarketingBudget * 0.8
        )
        val r3 = simulateRunway(reducedBudgets)
        if (r3 > baseRunway) {
            levers.add(LeverAction("Trim validation/MVP budgets by 20%", r3 - baseRunway, LeverType.REDUCE_BUDGET))
        }
        
        // 4. Delay largest one-time cost
        val largestOneTime = inputs.oneTimeCosts.maxByOrNull { it.amount }
        if (largestOneTime != null && largestOneTime.monthOffset < 12) {
            val adjustedList = inputs.oneTimeCosts.toMutableList()
            val index = adjustedList.indexOf(largestOneTime)
            adjustedList[index] = largestOneTime.copy(monthOffset = largestOneTime.monthOffset + 1)
            
            val r4 = simulateRunway(inputs.copy(oneTimeCosts = adjustedList))
            if (r4 > baseRunway) {
                levers.add(LeverAction("Delay ${largestOneTime.name} by 1 month", r4 - baseRunway, LeverType.DELAY_COST))
            }
        }
        
        // 5. Cash Injection
        // If runway is critical (<3), suggest small injection. If > 3, suggest scaling injection.
        // Let's just try a fixed amount relative to burn. e.g. 3 months of burn.
        val burnEstimate = inputs.fixedMonthlyCosts + inputs.variableMonthlyCosts
        if (burnEstimate > 0) {
            val injection = burnEstimate * 3
            val r5 = simulateRunway(inputs.copy(cashInBank = inputs.cashInBank + injection))
            if (r5 > baseRunway) {
                val formattedAmt = (injection / 100000).toInt() // In Lakhs approx
                levers.add(LeverAction("Cash injection of ₹${formattedAmt}L", r5 - baseRunway, LeverType.CASH_INJECTION))
            }
        }
        
        return levers.take(5)
    }
}
