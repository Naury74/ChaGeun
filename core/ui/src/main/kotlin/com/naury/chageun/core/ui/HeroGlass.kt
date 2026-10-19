package com.naury.chageun.core.ui

import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import com.naury.chageun.core.designsystem.component.ChageunButton
import com.naury.kinetica.ui.GlassBackdrop
import com.naury.kinetica.ui.GlassStyles
import com.naury.kinetica.ui.KVariant
import com.naury.kinetica.ui.LocalGlassBackdrop
import com.naury.kinetica.ui.LocalKineticaSurface
import com.naury.kinetica.ui.SurfaceStyle

/** Hero 하늘 위에 뜨는 글래스 표면과, 그 글래스가 비출 하늘 기록. [VehicleHeroSection] 안에서만 값이 있다. */
@Immutable
class HeroGlass internal constructor(val surface: SurfaceStyle, val backdrop: GlassBackdrop)

val LocalHeroGlass = staticCompositionLocalOf<HeroGlass?> { null }

/**
 * 하늘 밝기에 맞춘 글래스. 밝은 하늘은 흰 유리에 짙은 글자, 어두운 하늘은 옅게 밝힌 유리에 흰 글자다.
 * 요약 카드 뒤는 하늘이 거의 끝나 화면 배경에 가까우므로, 기본 Liquid보다 색을 진하게 덮어 바탕과 판이 구분되게 한다.
 */
internal fun heroGlassSurface(sky: Color): SurfaceStyle = if (sky.luminance() < DARK_SKY_LUMINANCE) {
    SurfaceStyle.Glass(GlassStyles.LiquidDark.copy(brightness = 0f, tint = Color.White.copy(alpha = DARK_TINT)))
} else {
    SurfaceStyle.Glass(GlassStyles.Liquid.copy(tint = Color.White.copy(alpha = LIGHT_TINT)))
}

/**
 * Hero 하늘 위 버튼. Hero 안이면 하늘을 블러로 비추는 글래스 버튼이고, 밖에서 그리면 [outside] 모양의 보통 버튼이다.
 * 하늘이 움직이면 글래스마다 블러를 다시 하므로 Hero 하나에 한두 개만 쓴다.
 */
@Composable
fun HeroGlassButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    outside: KVariant = KVariant.Tonal,
) {
    val glass = LocalHeroGlass.current
    if (glass == null) {
        ChageunButton(text, onClick, modifier.minimumInteractiveComponentSize(), variant = outside, icon = icon)
        return
    }
    // ChageunButton은 테마 표면과 가장 가까운 배경을 쓰므로 이 버튼에만 Hero 글래스와 하늘 기록을 넣는다.
    CompositionLocalProvider(LocalKineticaSurface provides glass.surface, LocalGlassBackdrop provides glass.backdrop) {
        ChageunButton(text, onClick, modifier.minimumInteractiveComponentSize(), variant = KVariant.Glass, icon = icon)
    }
}

// 라이트 하늘(가장 어두운 비 오는 하늘도 0.4 안팎)과 다크 하늘(0.05 아래)을 가르는 밝기.
private const val DARK_SKY_LUMINANCE = 0.25f
private const val LIGHT_TINT = 0.55f
private const val DARK_TINT = 0.08f
