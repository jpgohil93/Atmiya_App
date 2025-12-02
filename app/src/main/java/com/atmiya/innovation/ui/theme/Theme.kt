package com.atmiya.innovation.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = AtmiyaPrimary,
    secondary = AtmiyaSecondary,
    tertiary = AtmiyaAccent,
    background = SoftBgDark,
    surface = SoftSurfaceDark,
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onTertiary = Color.White,
    onBackground = SoftTextPrimaryDark,
    onSurface = SoftTextPrimaryDark,
    surfaceVariant = SoftSurfaceDark, // For cards
    onSurfaceVariant = SoftTextSecondaryDark
)

private val LightColorScheme = lightColorScheme(
    primary = AtmiyaPrimary,
    secondary = AtmiyaSecondary,
    tertiary = AtmiyaAccent,
    background = SoftBgLight,
    surface = SoftSurfaceLight,
    onPrimary = Color.White,
    onSecondary = Color.Black, // White secondary needs black text
    onTertiary = Color.White, // Red accent needs white text
    onBackground = SoftTextPrimaryLight,
    onSurface = SoftTextPrimaryLight,
    surfaceVariant = SoftSurfaceLight, // For cards
    onSurfaceVariant = SoftTextSecondaryLight
)

@Composable
fun AtmiyaInnovationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Disable dynamic color to enforce Soft UI
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb() // Match background for seamless look
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
