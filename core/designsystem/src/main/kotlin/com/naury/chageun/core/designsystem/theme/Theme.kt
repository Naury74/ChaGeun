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
    // Tonal 버튼과 선택된 Chip은 secondaryContainer를 쓴다. 지정하지 않으면 Material 기본 보라색이 나온다.
    secondaryContainer = LightTokens.TonalContainer,
    onSecondaryContainer = LightTokens.OnTonalContainer,
    tertiary = LightTokens.AiAccent,
    background = LightTokens.Background,
    onBackground = LightTokens.OnSurface,
    surface = LightTokens.Surface,
    onSurface = LightTokens.OnSurface,
    surfaceVariant = LightTokens.SurfaceVariant,
    surfaceBright = LightTokens.SurfaceRaised,
    onSurfaceVariant = LightTokens.OnSurfaceVariant,
    surfaceContainerLowest = LightTokens.Surface,
    surfaceContainerLow = LightTokens.Surface,
    surfaceContainer = LightTokens.Surface,
    surfaceContainerHigh = LightTokens.SurfaceVariant,
    outline = LightTokens.Outline,
    outlineVariant = LightTokens.Separator,
    error = LightTokens.Critical,
    errorContainer = LightTokens.CriticalContainer,
)

private val DarkColorScheme = darkColorScheme(
    primary = DarkTokens.Primary,
    onPrimary = DarkTokens.OnPrimary,
    primaryContainer = DarkTokens.PrimaryContainer,
    onPrimaryContainer = DarkTokens.OnPrimaryContainer,
    secondary = DarkTokens.Primary,
    // Tonal 버튼과 선택된 Chip은 secondaryContainer를 쓴다. 지정하지 않으면 Material 기본 보라색이 나온다.
    secondaryContainer = DarkTokens.TonalContainer,
    onSecondaryContainer = DarkTokens.OnTonalContainer,
    tertiary = DarkTokens.AiAccent,
    background = DarkTokens.Background,
    onBackground = DarkTokens.OnSurface,
    surface = DarkTokens.Surface,
    onSurface = DarkTokens.OnSurface,
    surfaceVariant = DarkTokens.SurfaceVariant,
    surfaceBright = DarkTokens.SurfaceRaised,
    onSurfaceVariant = DarkTokens.OnSurfaceVariant,
    surfaceContainerLowest = DarkTokens.Background,
    surfaceContainerLow = DarkTokens.Surface,
    surfaceContainer = DarkTokens.Surface,
    surfaceContainerHigh = DarkTokens.SurfaceVariant,
    outline = DarkTokens.Outline,
    outlineVariant = DarkTokens.Separator,
    error = DarkTokens.Critical,
    errorContainer = DarkTokens.CriticalContainer,
)

/**
 * 브랜드 테마다. Dynamic color는 일부러 지원하지 않는다. 상태 색(good/upcoming/critical)이
 * 기기마다 같아야 한눈에 알아볼 수 있기 때문이다.
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
