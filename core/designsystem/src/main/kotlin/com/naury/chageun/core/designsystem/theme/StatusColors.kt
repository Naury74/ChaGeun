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
    /** Hero 맨 위 하늘색. 아래로 갈수록 [heroBackground]로 이어진다. */
    val heroSky: Color,
    /** 하늘 오른쪽 위의 은은한 빛. */
    val heroGlow: Color,
    /** Hero 아래 요약 카드. 밝은 하늘 위에서 숫자가 또렷하게 보이도록 진한 남색을 쓴다. */
    val heroCard: Color,
    val onHeroCard: Color,
)

internal val LightExtendedColors = ChageunExtendedColors(
    good = ToneColors(LightTokens.Good, LightTokens.GoodContainer),
    upcoming = ToneColors(LightTokens.Upcoming, LightTokens.UpcomingContainer),
    critical = ToneColors(LightTokens.Critical, LightTokens.CriticalContainer),
    unknown = ToneColors(LightTokens.Unknown, LightTokens.UnknownContainer),
    ai = ToneColors(LightTokens.AiAccent, LightTokens.AiContainer),
    // 그룹 배경 위에 차 그림이 바로 놓이도록 화면 배경과 같게 둔다. 흰 사각 영역이 생기면 모서리가 도드라진다.
    heroBackground = LightTokens.Background,
    heroSky = Color(0xFFB9D4FF),
    heroGlow = Color(0xB3FFFFFF),
    heroCard = Color(0xFF1E2A47),
    onHeroCard = Color(0xFFFFFFFF),
)

internal val DarkExtendedColors = ChageunExtendedColors(
    good = ToneColors(DarkTokens.Good, DarkTokens.GoodContainer),
    upcoming = ToneColors(DarkTokens.Upcoming, DarkTokens.UpcomingContainer),
    critical = ToneColors(DarkTokens.Critical, DarkTokens.CriticalContainer),
    unknown = ToneColors(DarkTokens.Unknown, DarkTokens.UnknownContainer),
    ai = ToneColors(DarkTokens.AiAccent, DarkTokens.AiContainer),
    heroBackground = DarkTokens.Background,
    heroSky = Color(0xFF10213F),
    heroGlow = Color(0x1F9EC1FF),
    heroCard = Color(0xFF1C2740),
    onHeroCard = Color(0xFFFFFFFF),
)

internal val LocalExtendedColors = staticCompositionLocalOf { LightExtendedColors }
