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
import com.naury.kinetica.ui.KVariant
import com.naury.kinetica.ui.LocalKineticaSurface
import com.naury.kinetica.ui.SurfaceStyle
import com.naury.kinetica.ui.SurfaceStyles

/** Hero 하늘 위에 뜨는 유리 표면. [VehicleHeroSection] 안에서만 값이 있다. */
@Immutable
class HeroGlass internal constructor(val surface: SurfaceStyle)

val LocalHeroGlass = staticCompositionLocalOf<HeroGlass?> { null }

/**
 * 하늘 밝기에 맞춘 가벼운 유리. 밝은 하늘은 흰 유리에 짙은 글자, 어두운 하늘은 옅게 밝힌 유리에 흰 글자다.
 * 블러 없이 반투명 채움과 위쪽 광택, 위아래 두 색 테두리로만 그린다. 비·눈 하늘은 매 프레임 움직여 블러 글래스는
 * 프레임마다 블러를 다시 해야 하는데(실기기에서 끊김), 카드 뒤는 하늘이 거의 끝나 블러 차이도 눈에 띄지 않는다.
 */
internal fun heroGlassSurface(sky: Color): SurfaceStyle = if (sky.luminance() < DARK_SKY_LUMINANCE) {
    SurfaceStyles.LiteGlassDark
} else {
    (SurfaceStyles.LiteGlass as SurfaceStyle.Solid).copy(color = Color.White.copy(alpha = LIGHT_FILL))
}

/**
 * Hero 하늘 위 버튼. Hero 안이면 가벼운 유리 버튼이고, 밖에서 그리면 [outside] 모양의 보통 버튼이다.
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
    // ChageunButton은 테마 표면을 쓰므로 이 버튼에만 Hero 유리 표면을 넣는다.
    CompositionLocalProvider(LocalKineticaSurface provides glass.surface) {
        ChageunButton(text, onClick, modifier.minimumInteractiveComponentSize(), variant = KVariant.Glass, icon = icon)
    }
}

// 라이트 하늘(가장 어두운 비 오는 하늘도 0.4 안팎)과 다크 하늘(0.05 아래)을 가르는 밝기.
private const val DARK_SKY_LUMINANCE = 0.25f

// 하늘이 옅어지는 카드 자리에서도 판이 바탕과 구분되는 채움.
private const val LIGHT_FILL = 0.6f
