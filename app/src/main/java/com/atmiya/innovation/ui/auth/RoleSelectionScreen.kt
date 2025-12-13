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
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.vector.ImageVector
import compose.icons.TablerIcons
import compose.icons.tablericons.Rocket
import compose.icons.tablericons.ChartLine
import compose.icons.tablericons.School

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
                color = Color(0xFFE3F2FD),
                icon = TablerIcons.Rocket,
                onClick = { onRoleSelected("startup", "edp") }
            )
            Spacer(modifier = Modifier.height(16.dp))
            RoleCard(
                title = "Accelerator",
                description = "Growth Stage / Revenue / Operational",
                color = Color(0xFFE3F2FD),
                icon = TablerIcons.Rocket,
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
                color = Color(0xFFE3F2FD),
                icon = TablerIcons.Rocket,
                onClick = { showStartupOptions = true }
            )
            
            Spacer(modifier = Modifier.height(16.dp))

            RoleCard(
                title = "Investor",
                description = "I want to fund innovative startups.",
                color = Color(0xFFFFF3E0),
                icon = TablerIcons.ChartLine,
                onClick = { onRoleSelected("investor", null) }
            )

            Spacer(modifier = Modifier.height(16.dp))

            RoleCard(
                title = "Mentor",
                description = "I want to guide and teach.",
                color = Color(0xFFE8F5E9),
                icon = TablerIcons.School,
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
    icon: ImageVector? = null,
    textColor: Color = Color.Black,
    isSelected: Boolean = false,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .padding(vertical = 8.dp)
            .clickable { onClick() }
            .border(
                width = if (isSelected) 2.dp else 0.dp,
                color = if (isSelected) AtmiyaPrimary else Color.Transparent,
                shape = RoundedCornerShape(16.dp)
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = color),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 4.dp else 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            if (icon != null) {
                // Icon Box
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(Color.White.copy(alpha = 0.5f), shape = RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = textColor.copy(alpha = 0.8f)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = textColor.copy(alpha = 0.7f),
                    lineHeight = 20.sp
                )
            }
        }
    }
}
