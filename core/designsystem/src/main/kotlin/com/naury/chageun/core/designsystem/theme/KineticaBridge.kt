package com.naury.chageun.core.designsystem.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.naury.kinetica.ui.KineticaColors
import com.naury.kinetica.ui.SurfaceStyle

/**
 * 차근 팔레트를 옮긴 Kinetica 단색 표면. 판은 카드와 같은 흰색(다크는 짙은 회색), 바탕은 그룹 목록 배경이고,
 * 보조 글자는 차근 토큰을 그대로 쓴다. 꺼진 스위치 트랙·선택된 세그먼트처럼 한 단계 올라온 면은 Kinetica가
 * 글자 색을 옅게 깔아 흰 카드 위에서도 보이게 한다.
 * 테두리와 그림자는 Kinetica CleanLight·CleanDark와 같은 값이다.
 */
internal fun kineticaSurface(darkTheme: Boolean): SurfaceStyle = if (darkTheme) {
    SurfaceStyle.Solid(
        color = DarkTokens.Surface,
        contentColor = DarkTokens.OnSurface,
        background = DarkTokens.Background,
        border = Color.White.copy(alpha = DARK_BORDER_ALPHA),
        borderWidth = 1.dp,
        elevation = 4.dp,
        contentMuted = DarkTokens.OnSurfaceVariant,
    )
} else {
    SurfaceStyle.Solid(
        color = LightTokens.Surface,
        contentColor = LightTokens.OnSurface,
        background = LightTokens.Background,
        border = Color.Black.copy(alpha = LIGHT_BORDER_ALPHA),
        borderWidth = 1.dp,
        elevation = 6.dp,
        contentMuted = LightTokens.OnSurfaceVariant,
    )
}

/**
 * 버튼·스위치·선택 표시는 대표 남색을 쓴다. 위험 색은 라이트에서만 차근 빨강을 넘긴다. 다크의 차근 빨강은 어두운 글자와
 * 짝을 이루는 옅은 색이라, 흰 글자를 얹는 Kinetica 채운 버튼에는 Kinetica 기본 빨강을 쓴다.
 */
internal fun kineticaColors(darkTheme: Boolean): KineticaColors = if (darkTheme) {
    KineticaColors(
        accent = DarkTokens.Primary,
        onAccent = DarkTokens.OnPrimary,
        positive = DarkTokens.Primary,
        onPositive = DarkTokens.OnPrimary,
    )
} else {
    KineticaColors(
        accent = LightTokens.Primary,
        onAccent = LightTokens.OnPrimary,
        positive = LightTokens.Primary,
        onPositive = LightTokens.OnPrimary,
        destructive = LightTokens.Critical,
    )
}

private const val LIGHT_BORDER_ALPHA = 0.06f
private const val DARK_BORDER_ALPHA = 0.08f
