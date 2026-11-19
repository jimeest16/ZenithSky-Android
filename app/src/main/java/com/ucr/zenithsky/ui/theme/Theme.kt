package com.ucr.zenithsky.ui.theme

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
    primary = ZenithBluePrimary,
    onPrimary = ZenithOnPrimary,
    primaryContainer = ZenithPrimaryContainer,
    onPrimaryContainer = ZenithOnPrimaryContainer,
    secondary = ZenithTealSecondary,
    onSecondary = ZenithOnSecondary,
    secondaryContainer = ZenithSecondaryContainer,
    onSecondaryContainer = ZenithOnSecondaryContainer,
    tertiary = ZenithAmberTertiary,
    onTertiary = ZenithOnTertiary,
    tertiaryContainer = ZenithTertiaryContainer,
    onTertiaryContainer = ZenithOnTertiaryContainer,
    background = ZenithBackgroundLight,
    onBackground = ZenithOnBackgroundLight,
    surface = ZenithSurfaceLight,
    onSurface = ZenithOnSurfaceLight,
    surfaceVariant = ZenithSurfaceVariantLight,
    onSurfaceVariant = ZenithOnSurfaceVariantLight
)

private val DarkColorScheme = darkColorScheme(
    primary = ZenithBlueDark,
    onPrimary = ZenithOnDark,
    primaryContainer = ZenithPrimaryContainerDark,
    onPrimaryContainer = ZenithOnPrimaryContainerDark,
    secondary = ZenithTealDark,
    onSecondary = ZenithOnTealDark,
    secondaryContainer = ZenithSecondaryContainerDark,
    onSecondaryContainer = ZenithOnSecondaryContainerDark,
    tertiary = ZenithAmberDark,
    onTertiary = Color(0xFF2B1800),
    background = ZenithBackgroundDark,
    onBackground = Color(0xFFE6EEF6),
    surface = ZenithSurfaceDark,
    onSurface = Color(0xFFE6EEF6),
    surfaceVariant = ZenithSurfaceVariantDark,
    onSurfaceVariant = Color(0xFFB4C5D6)
)

@Composable
fun ZenithSkyAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}