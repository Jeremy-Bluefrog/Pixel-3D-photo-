package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

private val ExpressiveDarkColorScheme = darkColorScheme(
    primary = ExpressivePrimary,
    onPrimary = ExpressiveDarkBackground,
    primaryContainer = ExpressivePrimaryContainer,
    onPrimaryContainer = ExpressiveOnPrimaryContainer,
    secondary = ExpressiveSecondary,
    onSecondary = TextPrimary,
    secondaryContainer = ExpressiveSecondaryContainer,
    onSecondaryContainer = ExpressiveOnSecondaryContainer,
    tertiary = ExpressiveTertiary,
    onTertiary = ExpressiveDarkBackground,
    tertiaryContainer = ExpressiveTertiaryContainer,
    onTertiaryContainer = ExpressiveOnTertiaryContainer,
    background = ExpressiveDarkBackground,
    onBackground = TextPrimary,
    surface = ExpressiveDarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = ExpressiveDarkSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    surfaceContainer = ExpressiveDarkSurfaceContainer
)

private val ExpressiveLightColorScheme = lightColorScheme(
    primary = ExpressivePrimary,
    onPrimary = ExpressiveDarkBackground,
    primaryContainer = ExpressivePrimaryContainer,
    onPrimaryContainer = ExpressiveOnPrimaryContainer,
    secondary = ExpressiveSecondary,
    onSecondary = TextPrimary,
    secondaryContainer = ExpressiveSecondaryContainer,
    onSecondaryContainer = ExpressiveOnSecondaryContainer,
    tertiary = ExpressiveTertiary,
    onTertiary = ExpressiveDarkBackground,
    tertiaryContainer = ExpressiveTertiaryContainer,
    onTertiaryContainer = ExpressiveOnTertiaryContainer,
    background = ExpressiveDarkBackground,
    onBackground = TextPrimary,
    surface = ExpressiveDarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = ExpressiveDarkSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    surfaceContainer = ExpressiveDarkSurfaceContainer
)

// Material 3 Expressive Shapes System
val ExpressiveShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(36.dp)
)

@Composable
fun Pixel3DTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> ExpressiveDarkColorScheme
        else -> ExpressiveLightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = ExpressiveShapes,
        content = content
    )
}


