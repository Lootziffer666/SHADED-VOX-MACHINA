package com.voxelscan.android.ui.theme

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

private val DarkColorScheme = darkColorScheme(
    primary = VoxelMintLight,
    onPrimary = VoxelDarkBackground,
    primaryContainer = VoxelPrimaryContainer,
    onPrimaryContainer = VoxelMintPeak,
    secondary = VoxelAmber,
    onSecondary = VoxelDarkBackground,
    secondaryContainer = VoxelAmberDark,
    onSecondaryContainer = Color.White,
    tertiary = VoxelCyanGlow,
    background = VoxelDarkBackground,
    onBackground = VoxelOnSurface,
    surface = VoxelDarkSurface,
    onSurface = VoxelOnSurface,
    surfaceVariant = VoxelDarkSurfaceContainer,
    onSurfaceVariant = VoxelOnSurfaceMuted,
    outline = VoxelOutline,
    surfaceContainerHigh = VoxelDarkSurfaceHigh,
    surfaceContainerHighest = VoxelDarkSurfaceBorder
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF006954),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF1D836C),
    onPrimaryContainer = Color(0xFFF5FFF9),
    secondary = Color(0xFF984805),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFD9754),
    onSecondaryContainer = Color(0xFF321300),
    tertiary = Color(0xFF1B6852),
    background = Color(0xFFFCF9F2),
    onBackground = Color(0xFF1C1C18),
    surface = Color(0xFFFCF9F2),
    onSurface = Color(0xFF1C1C18),
    surfaceVariant = Color(0xFFE5E2DB),
    onSurfaceVariant = Color(0xFF3E4945),
    outline = Color(0xFF6E7A75),
    surfaceContainerHigh = Color(0xFFEBE8E1),
    surfaceContainerHighest = Color(0xFFE5E2DB)
)

@Composable
fun VoxelScanARTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Keep cyber emerald aesthetic consistent
    content: @Composable () -> Unit,
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
