package com.atmiya.innovation.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import compose.icons.TablerIcons
import compose.icons.tablericons.User
import compose.icons.tablericons.ArrowLeft
import compose.icons.tablericons.Bell
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.atmiya.innovation.ui.theme.AtmiyaPrimary
import com.atmiya.innovation.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommonTopBar(
    onOpenDrawer: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    userPhotoUrl: String?,
    userName: String? = null
) {
    CenterAlignedTopAppBar(
        title = {
            // Netfund Logo
            AsyncImage(
                model = R.drawable.netfund_logo,
                contentDescription = "Netfund",
                modifier = Modifier.height(60.dp),
                contentScale = ContentScale.Fit
            )
        },
        navigationIcon = {
            // Custom Stylish Burger Menu
            IconButton(
                onClick = onOpenDrawer,
                modifier = Modifier
                    .padding(start = 8.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .size(40.dp)
            ) {
                Column(
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(modifier = Modifier.width(20.dp).height(2.dp).background(AtmiyaPrimary, RoundedCornerShape(1.dp)))
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(modifier = Modifier.width(14.dp).height(2.dp).background(AtmiyaPrimary, RoundedCornerShape(1.dp))) // Asymmetric look
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(modifier = Modifier.width(20.dp).height(2.dp).background(AtmiyaPrimary, RoundedCornerShape(1.dp)))
                }
            }
        },
        actions = {
            // Bell Icon (Notification Center)
             IconButton(
                onClick = onNavigateToNotifications,
                modifier = Modifier.padding(end = 8.dp)
            ) {
                Icon(
                    TablerIcons.Bell,
                    contentDescription = "Notifications",
                    tint = AtmiyaPrimary,
                    modifier = Modifier.size(28.dp)
                )
            }

            // Larger Profile Photo
            UserAvatar(
                model = userPhotoUrl,
                name = userName ?: "Profile",
                modifier = Modifier
                    .padding(end = 16.dp)
                    .clickable { onNavigateToProfile() },
                size = 44.dp
            )

        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    )
}
