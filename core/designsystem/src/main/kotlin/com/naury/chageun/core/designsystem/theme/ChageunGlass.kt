package com.naury.chageun.core.designsystem.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.naury.kinetica.ui.GlassStyle
import com.naury.kinetica.ui.GlassStyles
import com.naury.kinetica.ui.KineticaTheme
import com.naury.kinetica.ui.SurfaceStyle
import com.naury.kinetica.ui.SurfaceStyles

/**
 * 차근의 글래스 표면. 목록과 카드는 단색으로 두고, 그 위에 떠 있는 것(탭 바, 시트, 대화상자, 토스트, 메뉴)과
 * 화면 위쪽 막대만 뒤를 블러로 비춘다. 밝기는 지금 표면(앱 테마)을 따른다.
 */
object ChageunGlass {
    /** 떠 있는 판(탭 바, 시트, 대화상자, 토스트). 뒤가 비치는 Liquid 글래스다. */
    val chrome: SurfaceStyle
        @Composable @ReadOnlyComposable
        get() = if (KineticaTheme.surface.isDark) SurfaceStyles.LiquidDark else SurfaceStyles.Liquid

    /** 화면 폭 전체 막대. 목록 위에서도 제목이 읽히도록 색을 진하게 덮고 테두리가 없다. */
    val bar: GlassStyle
        @Composable @ReadOnlyComposable
        get() = if (KineticaTheme.surface.isDark) GlassStyles.BarDark else GlassStyles.Bar
}

/**
 * 화면 아래에 떠 있는 탭 바가 가리는 높이. 목록은 탭 바 뒤로 지나가며 비쳐 보이도록 끝까지 그리고,
 * 마지막 항목과 화면 아래에 붙는 버튼은 이만큼 띄운다. 탭 바가 없는 화면에서는 0이다.
 */
val LocalBottomBarPadding = compositionLocalOf<Dp> { 0.dp }
