package com.atmiya.innovation.ui.admin

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.atmiya.innovation.data.User
import com.atmiya.innovation.ui.theme.AtmiyaPrimary

import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Person
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserListScreen(
    onUserClick: (String) -> Unit,
    viewModel: AdminViewModel = viewModel()
) {
    val users by viewModel.users.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val selectedRole by viewModel.selectedRole.collectAsState()
    
    val roles = listOf("startup", "investor", "mentor")

    Column(modifier = Modifier.fillMaxSize()) {
        // Role Filter
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            roles.forEach { role ->
                FilterChip(
                    selected = selectedRole == role,
                    onClick = { viewModel.setRole(role) },
                    label = { Text(role.capitalize()) }
                )
            }
        }

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = AtmiyaPrimary)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (users.isEmpty()) {
                    item {
                        Text(
                            "No users found.",
                            modifier = Modifier.fillMaxWidth().padding(32.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                } else {
                    items(users) { user ->
                        UserCard(user = user, onClick = { onUserClick(user.uid) })
                    }
                }
            }
        }
    }
}

@Composable
fun UserCard(user: User, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Profile Photo
            if (user.profilePhotoUrl != null) {
                coil.compose.AsyncImage(
                    model = user.profilePhotoUrl,
                    contentDescription = "Profile Photo",
                    modifier = Modifier.size(48.dp).clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier.size(48.dp).clip(CircleShape).background(Color.Gray.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(24.dp), tint = Color.Gray)
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = user.name.ifEmpty { "No Name" },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = user.email.ifEmpty { "No Email" },
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
                 if (user.phoneNumber.isNotEmpty()) {
                    Text(
                        text = user.phoneNumber,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
                if (user.isBlocked) {
                    Text(
                        text = "BLOCKED",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Red,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Icon(
                imageVector = Icons.Filled.ArrowForward,
                contentDescription = null,
                tint = Color.Gray
            )
        }
    }
}

private fun String.capitalize(): String {
    return this.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
}
