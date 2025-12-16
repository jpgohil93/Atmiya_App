package com.atmiya.innovation.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.atmiya.innovation.data.User
import com.atmiya.innovation.repository.FirestoreRepository
import com.atmiya.innovation.ui.components.UserAvatar
import com.atmiya.innovation.ui.theme.AtmiyaPrimary
import com.atmiya.innovation.ui.theme.AtmiyaSecondary

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfileDetailScreen(
    userId: String,
    onBack: () -> Unit,
    onNavigateToChat: (String, String) -> Unit // userId, userName
) {
    val repository = remember { FirestoreRepository() }
    var user by remember { mutableStateOf<User?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    
    // Tabs
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("About", "Activity", "Network")

    LaunchedEffect(userId) {
        try {
            user = repository.getUser(userId)
        } catch (e: Exception) {
            android.util.Log.e("ProfileDetail", "Error", e)
        } finally {
            isLoading = false
        }
    }

    Scaffold(
        containerColor = Color.White
    ) { padding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = AtmiyaPrimary)
            }
        } else if (user == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("User not found.")
                Button(onClick = onBack) { Text("Go Back") }
            }
        } else {
            val u = user!!
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
            ) {
                // --- Header Section ---
                Box(contentAlignment = Alignment.BottomStart) {
                    // Banner
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(AtmiyaPrimary, AtmiyaSecondary)
                                )
                            )
                            .padding(bottom = 40.dp) // Space for overlap
                    )
                    
                    // Back Button
                    IconButton(
                        onClick = onBack, 
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(top = 16.dp, start = 16.dp)
                            .background(Color.Black.copy(alpha=0.2f), CircleShape)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }

                    // Profile Image (Overlapping)
                    Surface(
                        shape = CircleShape,
                        color = Color.White,
                        shadowElevation = 4.dp,
                        modifier = Modifier
                            .padding(start = 24.dp)
                            .size(100.dp)
                            .offset(y = 50.dp) // Push down
                    ) {
                        UserAvatar(
                            model = u.profilePhotoUrl,
                            name = u.name,
                            modifier = Modifier.padding(4.dp),
                            size = 100.dp, // Surface handles size, but passing it explicitly to avatar ensures text scaling uses it? 
                            // Actually UserAvatar handles size.
                            // But Surface 100dp -> Padding 4dp -> Content 92dp approx.
                            // Let's passed explicit size null and match parent or similar.
                            // The UserAvatar with size=100.dp will set size=100.
                            // We have padding(4.dp). So we should probably let UserAvatar fill?
                            // If we set size=null, modifier.fillMaxSize() works.
                        )
                    }
                }
                
                // Actions Row (Right side of profile pic)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp, end = 16.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    IconButton(onClick = { /* Share */ }) {
                        Icon(Icons.Default.Share, contentDescription = "Share", tint = Color.Gray)
                    }
                    IconButton(onClick = { /* More */ }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More", tint = Color.Gray)
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // --- User Info ---
                Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                    Text(
                        text = u.name,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = u.role.replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.Gray
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Rajkot, Gujarat", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Buttons
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(
                            onClick = { /* Connect Logic */ },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = AtmiyaPrimary),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Outlined.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Connect")
                        }
                        
                         OutlinedButton(
                            onClick = { onNavigateToChat(u.uid, u.name) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, AtmiyaPrimary),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = AtmiyaPrimary)
                        ) {
                            Icon(Icons.Outlined.Chat, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Message")
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))

                // --- Tabs ---
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.Transparent,
                    contentColor = AtmiyaPrimary,
                    divider = { Divider(color = Color.LightGray.copy(alpha=0.3f)) },
                    indicator = { tabPositions ->
                        TabRowDefaults.Indicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = AtmiyaPrimary,
                            height = 3.dp
                        )
                    }
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = { Text(title, fontWeight = if(selectedTab == index) FontWeight.Bold else FontWeight.Normal) }
                        )
                    }
                }
                
                // --- Tab Content ---
                Column(modifier = Modifier.padding(24.dp)) {
                    when (selectedTab) {
                        0 -> { // About
                            SectionTitle("About")
                            Text(
                                "Passionate about innovation and technology. Active member of the Atmiya community.",
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color.Gray
                            )
                            
                            Spacer(modifier = Modifier.height(24.dp))
                            
                            SectionTitle("Contact Info")
                            ContactRow(Icons.Default.Email, u.email)
                            if (u.phoneNumber.isNotBlank()) ContactRow(Icons.Default.Phone, com.atmiya.innovation.utils.StringUtils.formatPhoneNumber(u.phoneNumber))
                            
                            Spacer(modifier = Modifier.height(24.dp))
                            
                            SectionTitle("Interests")
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf("Leadership", "Startups", "Networking").forEach { tag ->
                                    Surface(
                                        color = Color(0xFFF5F5F5),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.padding(bottom = 8.dp)
                                    ) {
                                        Text(text = tag, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), color = Color.Gray)
                                    }
                                }
                            }
                        }
                        1 -> { // Activity
                             Text("No recent activity.", color = Color.Gray, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                        }
                        2 -> { // Network
                             Text("Connections hidden.", color = Color.Gray, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                        }
                    }
                }
                
                 Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}

@Composable
fun SectionTitle(title: String) {
    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
}

@Composable
fun ContactRow(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(modifier = Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = AtmiyaPrimary, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}
