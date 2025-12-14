package com.atmiya.innovation.ui.dashboard.startup

import org.junit.Assert.*
import org.junit.Test

class RunwayCalculatorEngineTest {

    @Test
    fun `test base runway calculation - simple fixed burn`() {
        val input = InputState(
            cashInBank = 24000.0,
            fixedMonthlyCosts = 2000.0,
            bufferPercent = 0 // simple math
        )
        // 24000 / 2000 = 12 months
        val result = RunwayCalculatorEngine.calculate(input)
        assertEquals(12, result.runwayMonths)
    }
    
    @Test
    fun `test buffer impact`() {
        val input = InputState(
            cashInBank = 22000.0,
            fixedMonthlyCosts = 2000.0,
            bufferPercent = 10 // Burn becomes 2200
        )
        // 22000 / 2200 = 10 months
        val result = RunwayCalculatorEngine.calculate(input)
        assertEquals(10, result.runwayMonths)
    }
    
    @Test
    fun `test one-time cost`() {
        // 24k cash, 2k/mo fixed burn. Normally 12 mo.
        // Add 4k one-time cost at month 2.
        // Month 0: 24k - 2k = 22k
        // Month 1: 22k - 2k = 20k
        // Month 2: 20k - 2k - 4k = 14k
        // ... continuing with 2k/mo
        // remaining 14k / 2k = 7 more months.
        // Total = 3 (0,1,2 used) + 7 = 10 months?
        // Let's trace:
        // Start: 24k
        // m0: 22k
        // m1: 20k
        // m2: 20 - 2 - 4 = 14k
        // m3: 12k
        // m4: 10k
        // m5: 8k
        // m6: 6k
        // m7: 4k
        // m8: 2k
        // m9: 0k -> Empty at end of month 9. So 10 months of survival? 
        // Engine returns index where cash <= 0.
        // If cash becomes 0 at end of m9, loop check "cash <= 0" returns 9? No.
        // Loop:
        // m0: cash=22. >0.
        // ...
        // m9: cash=0. Returns 9. 
        // Index 9 means it survived 9 full months? Or fails in month 9?
        // "runwayDetails: first month index where cash <= 0".
        // If index is 9, it means we ran out IN month 9. i.e. we had funds for 0..8 (9 months).
        // Let's check logic.
        
        val input = InputState(
            cashInBank = 24000.0,
            fixedMonthlyCosts = 2000.0,
            bufferPercent = 0,
            oneTimeCosts = listOf(OneTimeCostItem(name = "Laptop", amount = 4000.0, monthOffset = 2))
        )
        val result = RunwayCalculatorEngine.calculate(input)
        assertEquals(10, result.runwayMonths) // 24000 - 4000 = 20000. 20000/2000 = 10. Math holds if burn is constant.
    }
    
    @Test
    fun `test validation status`() {
        // Idea stage, need 90 days (3 months).
        // Cash for only 2 months. 
        val input = InputState(
            stage = StartupStage.IDEA_PRE_REVENUE,
            cashInBank = 2000.0,
            fixedMonthlyCosts = 1000.0,
            bufferPercent = 0,
            validationTargetDays = 90 // 3 months
        )
        // Runway = 2 months. Target = 3 months.
        // Status should be NOT_ACHIEVABLE
        val result = RunwayCalculatorEngine.calculate(input)
        assertEquals(2, result.runwayMonths)
        assertEquals(ValidationStatus.NOT_ACHIEVABLE, result.validationStatus)
        
        // Cash for 3 months exactly
        val input2 = input.copy(cashInBank = 3000.0)
        val result2 = RunwayCalculatorEngine.calculate(input2)
        assertEquals(3, result2.runwayMonths)
        assertEquals(ValidationStatus.AT_RISK, result2.validationStatus) // "within +1 month"
        
        // Cash for 5 months
        val input3 = input.copy(cashInBank = 5000.0)
        val result3 = RunwayCalculatorEngine.calculate(input3)
        assertEquals(5, result3.runwayMonths)
        assertEquals(ValidationStatus.ACHIEVABLE, result3.validationStatus)
    }
    
    @Test
    fun `test scenarios`() {
        val input = InputState(
            cashInBank = 10000.0,
            fixedMonthlyCosts = 1000.0,
            bufferPercent = 10 // Base Burn = 1100. Runway ~9.
        )
        val result = RunwayCalculatorEngine.calculate(input)
        
        // Conservative: buffer +5 = 15%. Burn = 1150. Runway = 10000/1150 = 8.69 -> 8 months.
        // Aggressive: buffer -5 = 5%. Burn = 1050. Runway = 10000/1050 = 9.5 -> 9 months.
        
        assertEquals(9, result.runwayMonths)
        assertEquals(8, result.scenarios.conservative)
        assertEquals(9, result.scenarios.aggressive) // 9.5 truncates to 9 in this engine logic loop?
        // Let's optimize engine test if needed. 9 full subtractions leaves 550. 10th fails. So 9 is correct.
    }
    
    @Test
    fun `test levers`() {
        val input = InputState(
            cashInBank = 5000.0,
            fixedMonthlyCosts = 1000.0,
            variableMonthlyCosts = 0.0,
            bufferPercent = 0
        )
        // Base: 5 months.
        
        val result = RunwayCalculatorEngine.calculate(input)
        
        // Lever: Reduce fixed by 10% -> 900/mo. 5000/900 = 5.55 -> 5 months.
        // Improvement? 5 -> 5. No change in integer month count.
        // Let's try larger cash to see integer jump.
        // 9500 cash. 1000/mo -> 9mo (rem 500).
        // 900/mo -> 10mo (rem 500). +1 month.
        
        val input2 = input.copy(cashInBank = 9500.0)
        val result2 = RunwayCalculatorEngine.calculate(input2)
        val fixedLever = result2.levers.find { it.type == LeverType.REDUCE_FIXED }
        assertNotNull(fixedLever)
        assertEquals(1, fixedLever?.runwayDelta)
    }
}
