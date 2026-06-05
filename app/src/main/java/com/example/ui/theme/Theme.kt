package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val LightColorScheme = lightColorScheme(
    primary = PrimaryLight,
    onPrimary = Color.White,
    secondary = SecondaryLight,
    onSecondary = Color.White,
    background = BackgroundLight,
    onBackground = Color(0xFF0F172A),
    surface = SurfaceLight,
    onSurface = Color(0xFF0F172A),
    primaryContainer = Color(0xFFDBEAFE),
    onPrimaryContainer = PrimaryLight
)

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryDark,
    onPrimary = Color(0xFF381E72),       // Deep violet contrast
    secondary = SecondaryDark,
    onSecondary = Color(0xFF332D41),
    background = BackgroundDark,
    onBackground = TextLightDark,
    surface = SurfaceDark,
    onSurface = TextLightDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = TextLightDark,
    primaryContainer = Color(0xFF4A4458), // Slate/mauve AI container
    onPrimaryContainer = Color(0xFFD0BCFF),
    outline = BorderDark
)

private val SepiaColorScheme = lightColorScheme(
    primary = PrimarySepia,
    onPrimary = Color.White,
    secondary = AccentSepia,
    onSecondary = Color.White,
    background = BackgroundSepia,
    onBackground = TextSepia,
    surface = SurfaceSepia,
    onSurface = TextSepia,
    primaryContainer = Color(0xFFEFE4CC),
    onPrimaryContainer = TextSepia
)

@Composable
fun ScripKuTheme(
    theme: String = "sepia", // "light", "dark", "sepia"
    content: @Composable () -> Unit
) {
    val colorScheme = when (theme) {
        "light" -> LightColorScheme
        "dark" -> DarkColorScheme
        "sepia" -> SepiaColorScheme
        else -> SepiaColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

// Wrapper for initial template support
@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val theme = if (darkTheme) "dark" else "light"
    ScripKuTheme(theme = theme, content = content)
}
