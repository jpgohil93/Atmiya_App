package com.atmiya.innovation.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun RoleBadge(role: String) {
    val (backgroundColor, textColor, label) = when (role.lowercase()) {
        "startup" -> Triple(Color(0xFFE3F2FD), Color(0xFF1976D2), "Startup") // Blue
        "investor" -> Triple(Color(0xFFFFF8E1), Color(0xFFFFA000), "Investor") // Gold/Amber
        "mentor" -> Triple(Color(0xFFF3E5F5), Color(0xFF7B1FA2), "Mentor") // Purple
        "admin" -> Triple(Color(0xFFFFEBEE), Color(0xFFD32F2F), "Admin") // Red
        else -> Triple(Color(0xFFF5F5F5), Color(0xFF616161), role.capitalize()) // Grey
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(backgroundColor)
            .padding(horizontal = 6.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = textColor,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

private fun String.capitalize(): String {
    return this.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
}
