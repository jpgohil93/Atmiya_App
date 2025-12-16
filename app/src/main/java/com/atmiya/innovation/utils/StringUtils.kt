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

    fun getAvatarColor(name: String?): androidx.compose.ui.graphics.Color {
        val colors = listOf(
            androidx.compose.ui.graphics.Color(0xFFEF5350),
            androidx.compose.ui.graphics.Color(0xFFEC407A),
            androidx.compose.ui.graphics.Color(0xFFAB47BC),
            androidx.compose.ui.graphics.Color(0xFF7E57C2),
            androidx.compose.ui.graphics.Color(0xFF5C6BC0),
            androidx.compose.ui.graphics.Color(0xFF42A5F5),
            androidx.compose.ui.graphics.Color(0xFF29B6F6),
            androidx.compose.ui.graphics.Color(0xFF26C6DA),
            androidx.compose.ui.graphics.Color(0xFF26A69A),
            androidx.compose.ui.graphics.Color(0xFF66BB6A),
            androidx.compose.ui.graphics.Color(0xFF9CCC65),
            androidx.compose.ui.graphics.Color(0xFFD4E157),
            androidx.compose.ui.graphics.Color(0xFFFFEE58),
            androidx.compose.ui.graphics.Color(0xFFFFCA28),
            androidx.compose.ui.graphics.Color(0xFFFFA726),
            androidx.compose.ui.graphics.Color(0xFFFF7043)
        )
        if (name.isNullOrBlank()) return androidx.compose.ui.graphics.Color.Gray
        val hash = name.hashCode()
        return colors[Math.abs(hash) % colors.size]
    }

    fun getInitials(name: String?): String {
        if (name.isNullOrBlank()) return ""
        val trimmed = name.trim()
        val parts = trimmed.split(" ").filter { it.isNotBlank() }
        return when {
            parts.isEmpty() -> ""
            parts.size == 1 -> parts[0].take(1).uppercase()
            else -> (parts[0].take(1) + parts[1].take(1)).uppercase()
        }
    }
}
