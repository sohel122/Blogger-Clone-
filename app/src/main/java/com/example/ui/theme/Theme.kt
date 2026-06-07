package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// --- Theme Schemes Mappings ---

private val CoralLightTheme = lightColorScheme(
    primary = CoralPrimary,
    secondary = CoralSecondary,
    tertiary = CoralTertiary,
    background = CoralBackground,
    surface = CoralSurface,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color(0xFF2C1915),
    onSurface = Color(0xFF2C1915)
)

private val CoralDarkTheme = darkColorScheme(
    primary = CoralDarkPrimary,
    secondary = CoralSecondary,
    tertiary = CoralTertiary,
    background = CoralDarkBackground,
    surface = CoralDarkSurface,
    onPrimary = Color.Black,
    onSecondary = Color.White,
    onBackground = Color(0xFFFFF1EE),
    onSurface = Color(0xFFFFF1EE)
)

private val OceanLightTheme = lightColorScheme(
    primary = OceanPrimary,
    secondary = OceanSecondary,
    tertiary = OceanTertiary,
    background = OceanBackground,
    surface = OceanSurface,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color(0xFF0F1B1D),
    onSurface = Color(0xFF0F1B1D)
)

private val OceanDarkTheme = darkColorScheme(
    primary = OceanDarkPrimary,
    secondary = OceanSecondary,
    tertiary = OceanTertiary,
    background = OceanDarkBackground,
    surface = OceanDarkSurface,
    onPrimary = Color.Black,
    onSecondary = Color.White,
    onBackground = Color(0xFFECF7F9),
    onSurface = Color(0xFFECF7F9)
)

private val ForestLightTheme = lightColorScheme(
    primary = ForestPrimary,
    secondary = ForestSecondary,
    tertiary = ForestTertiary,
    background = ForestBackground,
    surface = ForestSurface,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color(0xFF101C16),
    onSurface = Color(0xFF101C16)
)

private val ForestDarkTheme = darkColorScheme(
    primary = ForestDarkPrimary,
    secondary = ForestSecondary,
    tertiary = ForestTertiary,
    background = ForestDarkBackground,
    surface = ForestDarkSurface,
    onPrimary = Color.Black,
    onSecondary = Color.White,
    onBackground = Color(0xFFEEF7F3),
    onSurface = Color(0xFFEEF7F3)
)

private val MidnightLightTheme = lightColorScheme(
    primary = MidnightPrimary,
    secondary = MidnightSecondary,
    tertiary = MidnightTertiary,
    background = MidnightBackground,
    surface = MidnightSurface,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color(0xFF221E1C),
    onSurface = Color(0xFF221E1C)
)

private val MidnightDarkTheme = darkColorScheme(
    primary = MidnightDarkPrimary,
    secondary = MidnightSecondary,
    tertiary = MidnightTertiary,
    background = MidnightDarkBackground,
    surface = MidnightDarkSurface,
    onPrimary = Color.Black,
    onSecondary = Color.White,
    onBackground = Color(0xFFECE5E2),
    onSurface = Color(0xFFECE5E2)
)

@Composable
fun BloggerTheme(
    themeName: String = "Coral Sunset",
    isDarkMode: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = when (themeName) {
        "Ocean Wave" -> if (isDarkMode) OceanDarkTheme else OceanLightTheme
        "Forest Emerald" -> if (isDarkMode) ForestDarkTheme else ForestLightTheme
        "Midnight Charcoal" -> if (isDarkMode) MidnightDarkTheme else MidnightLightTheme
        else -> if (isDarkMode) CoralDarkTheme else CoralLightTheme // Default Coral Sunset
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

// Retain duplicate default theme to prevent compile error in previous template usages
@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    BloggerTheme(
        themeName = "Coral Sunset",
        isDarkMode = darkTheme,
        content = content
    )
}
