package com.atmiya.innovation.utils

import java.text.NumberFormat
import java.util.Locale

object CurrencyUtils {
    fun formatIndianRupee(amount: Any?): String {
        if (amount == null) return "N/A"
        
        val amountStr = amount.toString()
        if (amountStr.isBlank()) return "N/A"
        
        // Return if non-numeric
        val doubleVal = amountStr.replace(",", "").toDoubleOrNull() ?: return amountStr
        
        val format = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
        format.maximumFractionDigits = 0
        // return format.format(doubleVal) // This usually adds "₹", let's ensure or strip if needed.
        // The user wants the icon *and* the formatted number. 
        // If we use the icon separately, we might just want the number part.
        // But NumberFormat with Locale("en", "IN") returns "₹ 50,00,000".
        // Let's return just the formatted number part if the UI puts the icon explicitly.
        // Or return the full string if we want it self-contained. 
        // The prompt says: "display it with indian ruppee icon... value should be comma sepearated".
        // Since I'm using an Icon component in the UI, I likely just want "50,00,000".
        
        val decimalFormat = NumberFormat.getNumberInstance(Locale("en", "IN"))
        decimalFormat.maximumFractionDigits = 0
        return decimalFormat.format(doubleVal)
    }
}
