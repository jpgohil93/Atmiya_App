package com.atmiya.innovation.ui.dashboard.startup

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info // Replaced CurrencyRupee, Lightbulb
import androidx.compose.material.icons.filled.Star // Replaced TrendingUp
import androidx.compose.material.icons.filled.DateRange // Replaced DateRange
import androidx.compose.material.icons.filled.Home // Replaced Home
import androidx.compose.material.icons.filled.Build // Replaced Build
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin

// --- Base Container ---

@Composable
fun AnimatedTileContainer(
    modifier: Modifier = Modifier,
    gradientColors: List<Color>,
    onClick: () -> Unit,
    content: @Composable BoxScope.() -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
    )

    // Background Gradient Animation
    val infiniteTransition = rememberInfiniteTransition()
    val gradientShift by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(10000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Surface(
        modifier = modifier
            .scale(scale)
            .height(180.dp) // Generous height for "mini-app" feel
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        tryAwaitRelease()
                        isPressed = false
                        onClick()
                    }
                )
            },
        shape = RoundedCornerShape(24.dp),
        shadowElevation = 8.dp,
        color = Color.Transparent
    ) {
        Box(
            modifier = Modifier
                .background(
                    brush = Brush.linearGradient(
                        colors = gradientColors,
                        start = Offset(0f - gradientShift, 0f),
                        end = Offset(1000f + gradientShift, 1000f)
                    )
                )
        ) {
            content()
        }
    }
}

// --- Specific Tiles ---

@Composable
fun FundingCallTile(
    count: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = listOf(Color(0xFFE0F2F1), Color(0xFFB2DFDB), Color(0xFF80CBC4)) // Green/Teal
    val accentColor = Color(0xFF00695C)

    AnimatedTileContainer(
        modifier = modifier,
        gradientColors = colors,
        onClick = onClick
    ) {
        // Background particles
        FloatingParticles(color = accentColor.copy(alpha = 0.1f))

        Column(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Icon & Animation
            Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                   // Pulsing Icon
                   PulseIcon(icon = Icons.Default.Info, tint = accentColor)
                }
                
                // Active Badge
                Surface(
                    color = accentColor,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "$count Active",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Text
            Column {
                Text(
                    text = "Funding Calls",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.Black.copy(alpha = 0.8f)
                )
                Text(
                    text = "Explore capital & grants",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Black.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
fun InvestorTile(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = listOf(Color(0xFFE8EAF6), Color(0xFFC5CAE9), Color(0xFF9FA8DA)) // Indigo
    val accentColor = Color(0xFF283593)

    AnimatedTileContainer(
        modifier = modifier,
        gradientColors = colors,
        onClick = onClick
    ) {
         // Background Graph Animation
         AnimatedGraphBackground(color = accentColor.copy(alpha = 0.15f))

        Column(
            modifier = Modifier.padding(20.dp).fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
             Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                PulseIcon(icon = Icons.Default.Star, tint = accentColor)
            }

            Column {
                Text(
                    text = "Investors",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.Black.copy(alpha = 0.8f)
                )
                Text(
                    text = "Connect with VCs & Angels",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Black.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
fun MentorTile(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
     val colors = listOf(Color(0xFFF3E5F5), Color(0xFFE1BEE7), Color(0xFFCE93D8)) // Purple
     val accentColor = Color(0xFF6A1B9A)

    AnimatedTileContainer(
        modifier = modifier,
        gradientColors = colors,
        onClick = onClick
    ) {
        // Floating Orbs
        FloatingOrbs(color = accentColor.copy(alpha = 0.1f))

        Column(
            modifier = Modifier.padding(20.dp).fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
             Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                // Lightbulb pulse handled by reused PulseIcon for now, could be specific
                PulseIcon(icon = Icons.Default.Info, tint = accentColor)
            }

            Column {
                Text(
                    text = "Mentors",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.Black.copy(alpha = 0.8f)
                )
                Text(
                    text = "Expert guidance & support",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Black.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
fun EventTile(
    count: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = listOf(Color(0xFFFFEBEE), Color(0xFFFFCDD2), Color(0xFFEF9A9A)) // Red
    val accentColor = Color(0xFFC62828)

    AnimatedTileContainer(
        modifier = modifier,
        gradientColors = colors,
        onClick = onClick
    ) {
        // Confetti like dots
        FloatingParticles(color = accentColor.copy(alpha = 0.1f))

        Column(
            modifier = Modifier.padding(20.dp).fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
             Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    PulseIcon(icon = Icons.Default.DateRange, tint = accentColor)
                }
                 if (count > 0) {
                     Surface(
                        color = accentColor,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "$count Coming",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            fontWeight = FontWeight.Bold
                        )
                    }
                 }
            }

            Column {
                Text(
                    text = "Events",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.Black.copy(alpha = 0.8f)
                )
                Text(
                    text = "Workshops & Meets",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Black.copy(alpha = 0.6f)
                )
            }
        }
    }
}


@Composable
fun IncubatorTile(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = listOf(Color(0xFFF1F8E9), Color(0xFFDCEDC8), Color(0xFFAED581)) // Light Green
    val accentColor = Color(0xFF558B2F)

    AnimatedTileContainer(
        modifier = modifier,
        gradientColors = colors,
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.padding(20.dp).fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
             Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                PulseIcon(icon = Icons.Default.Home, tint = accentColor) // Replaced Business with Home or Domain (Core doesn't have Business)
            }

            Column {
                Text(
                    text = "Incubators",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.Black.copy(alpha = 0.8f)
                )
                Text(
                    text = "Infrastructure & Space",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Black.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
fun GovernanceTile(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = listOf(Color(0xFFECEFF1), Color(0xFFCFD8DC), Color(0xFFB0BEC5)) // Blue Grey
    val accentColor = Color(0xFF455A64)

    AnimatedTileContainer(
        modifier = modifier,
        gradientColors = colors,
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.padding(20.dp).fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
             Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                PulseIcon(icon = Icons.Default.Build, tint = accentColor)
            }

            Column {
                Text(
                    text = "Governance",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.Black.copy(alpha = 0.8f)
                )
                Text(
                    text = "Policy & Compliance",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Black.copy(alpha = 0.6f)
                )
            }
        }
    }
}


// --- Animation Helpers ---

@Composable
fun PulseIcon(icon: ImageVector, tint: Color) {
    val infiniteTransition = rememberInfiniteTransition()
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )
    
     Icon(
        imageVector = icon,
        contentDescription = null,
        tint = tint,
        modifier = Modifier.scale(scale).size(24.dp)
    )
}

@Composable
fun FloatingParticles(color: Color) {
    val infiniteTransition = rememberInfiniteTransition()
    val offsetY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -30f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        
        drawCircle(color, radius = 8.dp.toPx(), center = Offset(width * 0.8f, height * 0.8f + offsetY))
        drawCircle(color, radius = 5.dp.toPx(), center = Offset(width * 0.2f, height * 0.5f + offsetY/2))
        drawCircle(color, radius = 10.dp.toPx(), center = Offset(width * 0.6f, height * 0.2f - offsetY))
    }
}

@Composable
fun AnimatedGraphBackground(color: Color) {
     val infiniteTransition = rememberInfiniteTransition()
     val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )
    
    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        
        val points = listOf(
            Offset(0f, height * 0.8f),
            Offset(width * 0.3f, height * 0.6f),
            Offset(width * 0.6f, height * 0.7f),
            Offset(width * 0.8f, height * 0.4f),
            Offset(width, height * 0.3f)
        )
        
        val path = Path().apply {
            moveTo(points[0].x, points[0].y)
            cubicTo(
                points[1].x, points[1].y,
                points[2].x, points[2].y,
                points[3].x, points[3].y
            )
            lineTo(points[4].x, points[4].y)
        }
        
        drawPath(
            path = path,
            color = color,
            style = Stroke(width = 4.dp.toPx())
        )
        
        // Moving dot
        val dotProgress = (phase * width) % width
        // Simplified dot travel logic for visual effect (just linear x)
         drawCircle(
             color = color.copy(alpha = 0.8f),
             radius = 6.dp.toPx(),
             center = Offset(phase * width, height * 0.5f) // Approximate path
         )
    }
}

@Composable
fun FloatingOrbs(color: Color) {
    val infiniteTransition = rememberInfiniteTransition()
    val xShift by infiniteTransition.animateFloat(
        initialValue = -20f,
        targetValue = 20f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )
    
    Canvas(modifier = Modifier.fillMaxSize()) {
        drawCircle(color, radius = 40.dp.toPx(), center = Offset(size.width * 0.9f + xShift, size.height * 0.1f))
        drawCircle(color, radius = 20.dp.toPx(), center = Offset(size.width * 0.1f - xShift, size.height * 0.9f))
    }
}
