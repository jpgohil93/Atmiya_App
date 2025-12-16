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
    primary = DarkPrimary,
    onPrimary = DarkOnPrimary,
    primaryContainer = SoftSurfaceDark,
    onPrimaryContainer = SoftTextPrimaryDark,
    secondary = AtmiyaSecondary,
    onSecondary = Color.White,
    tertiary = AtmiyaAccent,
    onTertiary = Color.White,
    background = SoftBgDark,
    onBackground = SoftTextPrimaryDark,
    surface = SoftSurfaceDark,
    onSurface = SoftTextPrimaryDark,
    surfaceVariant = SoftSurfaceDark,
    onSurfaceVariant = SoftTextSecondaryDark,
    outline = OutlineDark,
    error = ErrorColor,
    onError = OnErrorColor
)

private val LightColorScheme = lightColorScheme(
    primary = AtmiyaPrimary,
    onPrimary = Color.White,
    primaryContainer = SoftSurfaceLight,
    onPrimaryContainer = SoftTextPrimaryLight,
    secondary = AtmiyaSecondary,
    onSecondary = Color.White,
    tertiary = AtmiyaAccent,
    onTertiary = Color.White,
    background = SoftBgLight,
    onBackground = SoftTextPrimaryLight,
    surface = SoftSurfaceLight,
    onSurface = SoftTextPrimaryLight,
    surfaceVariant = SoftSurfaceLight,
    onSurfaceVariant = SoftTextSecondaryLight,
    outline = OutlineLight,
    error = ErrorColor,
    onError = OnErrorColor
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
