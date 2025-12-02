package com.atmiya.innovation.ui.dashboard.home

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import com.atmiya.innovation.repository.FirestoreRepository
import com.atmiya.innovation.ui.components.BentoCardType
import com.atmiya.innovation.ui.components.BentoGrid
import com.atmiya.innovation.ui.components.BentoItem
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

@Composable
fun AdminHome(
    onNavigate: (String) -> Unit
) {
    com.atmiya.innovation.ui.admin.AdminDashboardScreen(onNavigate = onNavigate)
}
