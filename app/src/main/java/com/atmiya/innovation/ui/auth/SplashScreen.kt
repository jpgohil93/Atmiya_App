package com.atmiya.innovation.ui.auth

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.atmiya.innovation.R
import com.atmiya.innovation.ui.theme.AtmiyaPrimary
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onSessionValid: () -> Unit,
    onSessionInvalid: () -> Unit
) {
    var startAnimation by remember { mutableStateOf(false) }
    val alphaAnim = animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(durationMillis = 1500), label = "alpha"
    )
    val scaleAnim = animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0.5f,
        animationSpec = tween(durationMillis = 1500, easing = FastOutSlowInEasing), label = "scale"
    )

    LaunchedEffect(Unit) {
        startAnimation = true
        delay(2500) // Wait for animation + a bit more
        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            onSessionValid()
        } else {
            onSessionInvalid()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        // Main Logo
        Image(
            painter = painterResource(id = R.drawable.atmiya_logo_new),
            contentDescription = "Atmiya Innovation Logo",
            modifier = Modifier
                .size(300.dp)
                .scale(scaleAnim.value)
                .alpha(alphaAnim.value)
        )

        // Powered By Section
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 48.dp)
                .alpha(alphaAnim.value), // Fade in with the rest
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Powered by",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                    modifier = Modifier.padding(end = 8.dp)
                )
                Image(
                    painter = painterResource(id = R.drawable.hl_group_logo),
                    contentDescription = "HL Group Logo",
                    modifier = Modifier.size(60.dp) // Smaller logo
                )
            }
        }
    }
}
