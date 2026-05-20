package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

// Sleek Light Color Scheme based on HTML requirements
private val SleekLightColorScheme = lightColorScheme(
    primary = SleekGreen,
    onPrimary = SleekWhite,
    primaryContainer = SleekGreen.copy(alpha = 0.12f),
    onPrimaryContainer = SleekGreen,
    secondary = SleekPurpleText,
    onSecondary = SleekWhite,
    secondaryContainer = SleekPurpleContainer,
    onSecondaryContainer = SleekPurpleText,
    background = SleekBackground,
    onBackground = SleekDarkText,
    surface = SleekWhite,
    onSurface = SleekDarkText,
    surfaceVariant = SleekNavBackground,
    onSurfaceVariant = SleekSecondaryText,
    outline = SleekOutline,
    outlineVariant = SleekOutline.copy(alpha = 0.5f),
    error = SleekError,
    onError = SleekWhite,
    errorContainer = SleekError.copy(alpha = 0.15f),
    onErrorContainer = SleekError
)

// Sleek Dark Color Scheme that is still visually stunning and eye-safe
private val SleekDarkColorScheme = darkColorScheme(
    primary = SleekGreen,
    onPrimary = SleekWhite,
    primaryContainer = SleekGreen.copy(alpha = 0.25f),
    onPrimaryContainer = SleekWhite,
    secondary = SleekPurpleContainer,
    onSecondary = SleekPurpleText,
    secondaryContainer = SleekPurpleText,
    onSecondaryContainer = SleekPurpleContainer,
    background = SleekDarkText,
    onBackground = SleekBackground,
    surface = SleekDarkText.copy(alpha = 0.95f),
    onSurface = SleekBackground,
    surfaceVariant = SleekSecondaryText,
    onSurfaceVariant = SleekNavIndicatorPill,
    outline = SleekOutline,
    outlineVariant = SleekOutline.copy(alpha = 0.4f),
    error = SleekError,
    onError = SleekWhite,
    errorContainer = SleekError.copy(alpha = 0.2f),
    onErrorContainer = SleekError.copy(alpha = 0.8f)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Set to false by default to showcase our beautiful "Sleek Interface" brand colors!
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> SleekDarkColorScheme
        else -> SleekLightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
