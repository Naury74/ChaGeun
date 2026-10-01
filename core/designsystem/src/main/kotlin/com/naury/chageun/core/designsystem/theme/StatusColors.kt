package com.naury.chageun.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class ToneColors(val content: Color, val container: Color)

@Immutable
data class ChageunExtendedColors(
    val good: ToneColors,
    val upcoming: ToneColors,
    val critical: ToneColors,
    val unknown: ToneColors,
    val ai: ToneColors,
    val heroBackground: Color,
)

internal val LightExtendedColors = ChageunExtendedColors(
    good = ToneColors(LightTokens.Good, LightTokens.GoodContainer),
    upcoming = ToneColors(LightTokens.Upcoming, LightTokens.UpcomingContainer),
    critical = ToneColors(LightTokens.Critical, LightTokens.CriticalContainer),
    unknown = ToneColors(LightTokens.Unknown, LightTokens.UnknownContainer),
    ai = ToneColors(LightTokens.AiAccent, LightTokens.AiContainer),
    heroBackground = LightTokens.Surface,
)

internal val DarkExtendedColors = ChageunExtendedColors(
    good = ToneColors(DarkTokens.Good, DarkTokens.GoodContainer),
    upcoming = ToneColors(DarkTokens.Upcoming, DarkTokens.UpcomingContainer),
    critical = ToneColors(DarkTokens.Critical, DarkTokens.CriticalContainer),
    unknown = ToneColors(DarkTokens.Unknown, DarkTokens.UnknownContainer),
    ai = ToneColors(DarkTokens.AiAccent, DarkTokens.AiContainer),
    heroBackground = DarkTokens.Background,
)

internal val LocalExtendedColors = staticCompositionLocalOf { LightExtendedColors }
