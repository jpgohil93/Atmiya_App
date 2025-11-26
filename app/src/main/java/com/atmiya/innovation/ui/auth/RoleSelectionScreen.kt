package com.atmiya.innovation.ui.auth

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.atmiya.innovation.ui.theme.AtmiyaPrimary
import com.atmiya.innovation.ui.theme.AtmiyaSecondary
import com.atmiya.innovation.ui.theme.AtmiyaAccent

@Composable
fun RoleSelectionScreen(
    onRoleSelected: (String, String?) -> Unit // role, startupType
) {
    var showStartupOptions by remember { mutableStateOf(false) }

    if (showStartupOptions) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Select Startup Program",
                style = MaterialTheme.typography.headlineMedium,
                color = AtmiyaPrimary,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(32.dp))
            
            RoleCard(
                title = "EDP Startup",
                description = "Early Stage / Idea / Prototype",
                color = AtmiyaPrimary,
                onClick = { onRoleSelected("startup", "edp") }
            )
            Spacer(modifier = Modifier.height(16.dp))
            RoleCard(
                title = "Accelerator",
                description = "Growth Stage / Revenue / Operational",
                color = AtmiyaPrimary,
                onClick = { onRoleSelected("startup", "accelerator") }
            )
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Choose Your Role",
                style = MaterialTheme.typography.headlineMedium,
                color = AtmiyaPrimary,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(32.dp))

            RoleCard(
                title = "Startup",
                description = "I have an idea or a running business.",
                color = AtmiyaPrimary,
                onClick = { showStartupOptions = true }
            )
            
            Spacer(modifier = Modifier.height(16.dp))

            RoleCard(
                title = "Investor",
                description = "I want to fund innovative startups.",
                color = AtmiyaSecondary,
                onClick = { onRoleSelected("investor", null) }
            )

            Spacer(modifier = Modifier.height(16.dp))

            RoleCard(
                title = "Mentor",
                description = "I want to guide and teach.",
                color = AtmiyaAccent,
                textColor = Color.Black,
                onClick = { onRoleSelected("mentor", null) }
            )
        }
    }
}

@Composable
fun RoleCard(
    title: String,
    description: String,
    color: Color,
    textColor: Color = Color.White,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = color)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = title,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
            Text(
                text = description,
                fontSize = 14.sp,
                color = textColor.copy(alpha = 0.8f)
            )
        }
    }
}
