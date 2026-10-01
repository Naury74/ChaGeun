package com.naury.chageun.core.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable

private val LightColorScheme = lightColorScheme(
    primary = LightTokens.Primary,
    onPrimary = LightTokens.OnPrimary,
    primaryContainer = LightTokens.PrimaryContainer,
    onPrimaryContainer = LightTokens.OnPrimaryContainer,
    secondary = LightTokens.Primary,
    tertiary = LightTokens.AiAccent,
    background = LightTokens.Background,
    onBackground = LightTokens.OnSurface,
    surface = LightTokens.Surface,
    onSurface = LightTokens.OnSurface,
    surfaceVariant = LightTokens.SurfaceVariant,
    onSurfaceVariant = LightTokens.OnSurfaceVariant,
    surfaceContainerLowest = LightTokens.Surface,
    surfaceContainerLow = LightTokens.Surface,
    surfaceContainer = LightTokens.Surface,
    surfaceContainerHigh = LightTokens.SurfaceVariant,
    outline = LightTokens.Outline,
    outlineVariant = LightTokens.SurfaceVariant,
    error = LightTokens.Critical,
    errorContainer = LightTokens.CriticalContainer,
)

private val DarkColorScheme = darkColorScheme(
    primary = DarkTokens.Primary,
    onPrimary = DarkTokens.OnPrimary,
    primaryContainer = DarkTokens.PrimaryContainer,
    onPrimaryContainer = DarkTokens.OnPrimaryContainer,
    secondary = DarkTokens.Primary,
    tertiary = DarkTokens.AiAccent,
    background = DarkTokens.Background,
    onBackground = DarkTokens.OnSurface,
    surface = DarkTokens.Surface,
    onSurface = DarkTokens.OnSurface,
    surfaceVariant = DarkTokens.SurfaceVariant,
    onSurfaceVariant = DarkTokens.OnSurfaceVariant,
    surfaceContainerLowest = DarkTokens.Background,
    surfaceContainerLow = DarkTokens.Surface,
    surfaceContainer = DarkTokens.Surface,
    surfaceContainerHigh = DarkTokens.SurfaceVariant,
    outline = DarkTokens.Outline,
    outlineVariant = DarkTokens.SurfaceVariant,
    error = DarkTokens.Critical,
    errorContainer = DarkTokens.CriticalContainer,
)

/**
 * Brand theme. Dynamic color is intentionally not supported: status tones (good/upcoming/critical)
 * must stay consistent across devices to remain recognizable.
 */
@Composable
fun ChageunTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    spacing: ChageunSpacing = Gutters.Compact,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalExtendedColors provides if (darkTheme) DarkExtendedColors else LightExtendedColors,
        LocalSpacing provides spacing,
    ) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
            typography = ChageunTypography,
            shapes = ChageunShapes,
            content = content,
        )
    }
}

object ChageunTheme {
    val colors: ChageunExtendedColors
        @Composable @ReadOnlyComposable
        get() = LocalExtendedColors.current

    val spacing: ChageunSpacing
        @Composable @ReadOnlyComposable
        get() = LocalSpacing.current
}
