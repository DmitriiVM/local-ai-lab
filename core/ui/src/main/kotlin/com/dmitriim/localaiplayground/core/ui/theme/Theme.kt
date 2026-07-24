package com.dmitriim.localaiplayground.core.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import com.dmitriim.localaiplayground.core.result.AppDimensions
import com.dmitriim.localaiplayground.core.result.LocalAppDimensions

private val BlackColorScheme = darkColorScheme(
    primary = Color.White,
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF262626),
    onPrimaryContainer = Color.White,
    secondary = Color(0xFFD1D1D1),
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF242424),
    onSecondaryContainer = Color(0xFFE5E2E6),
    tertiary = Color(0xFFD6C2FF),
    onTertiary = Color.Black,
    tertiaryContainer = Color(0xFF3B2B5A),
    onTertiaryContainer = Color(0xFFECDDFF),
    background = Color.Black,
    onBackground = Color(0xFFE5E2E6),
    surface = Color.Black,
    onSurface = Color(0xFFE5E2E6),
    surfaceVariant = Color(0xFF242424),
    onSurfaceVariant = Color(0xFFCAC4D0),
    surfaceContainer = Color(0xFF171717),
    surfaceContainerHigh = Color(0xFF242424),
    outline = Color(0xFF938F99),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
)

@Composable
fun LocalAiPlaygroundTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalAppDimensions provides AppDimensions()) {
        MaterialTheme(
            colorScheme = BlackColorScheme,
            typography = Typography,
            content = content,
        )
    }
}
