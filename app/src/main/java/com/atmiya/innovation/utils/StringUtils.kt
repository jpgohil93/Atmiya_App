package com.atmiya.innovation.utils

object StringUtils {
    /**
     * Formats a phone number to standard +91 XXXXX XXXXX format.
     * Assumes Indian phone numbers (10 digits).
     * If input is null or blank, returns empty string.
     * If input doesn't match expected 10-digit pattern (after cleaning), returns original.
     */
    fun formatPhoneNumber(phone: String?): String {
        if (phone.isNullOrBlank()) return ""

        // Remove all non-digit characters
        val digits = phone.filter { it.isDigit() }

        // Check if valid 10 digit number (or 12 with 91 prefix)
        return when {
            digits.length == 10 -> {
                "+91 ${digits.substring(0, 5)} ${digits.substring(5)}"
            }
            digits.length == 12 && digits.startsWith("91") -> {
                "+91 ${digits.substring(2, 7)} ${digits.substring(7)}"
            }
            // If user entered +91 but maybe with different spacing or chars, we cleaned it.
            // If length is weird, just return original to avoid losing data.
            else -> phone 
        }
    }
}
