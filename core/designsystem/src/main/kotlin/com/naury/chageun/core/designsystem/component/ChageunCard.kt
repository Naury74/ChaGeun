package com.naury.chageun.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import com.naury.chageun.core.designsystem.theme.kineticaSurface
import com.naury.kinetica.ui.KineticaTheme
import com.naury.kinetica.ui.LocalKineticaContentColor
import com.naury.kinetica.ui.SurfaceStyle
import com.naury.kinetica.ui.kSurface

/**
 * 차근 카드 판. Kinetica 단색 표면(옅은 그림자와 머리카락 테두리)으로 그리고, 시트나 대화상자 안에서는 판 위에 얹힌 한 겹으로 그린다.
 * [color]를 주면 같은 깊이에 그 색을 칠한다. 상태 색처럼 색이 뜻을 가진 판에 쓴다.
 * [inset]이면 그림자와 테두리 없이 색만 칠한다. 시트 안의 안내 칸처럼 판 위에 놓인 색 칸은 이쪽이 색을 잃지 않는다.
 * [popup]이면 따로 띄운 대화상자 창의 판이다. 비칠 화면이 없으니 글래스 시트 안에서 열어도 떠 있는 단색 판으로 그린다.
 *
 * 누르는 카드는 [modifier]에 `pressableCard`나 `kClickable`을 붙인다. 안쪽 글자는 [contentColor]를 받는다.
 */
@Composable
fun ChageunCard(
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.large,
    color: Color = Color.Unspecified,
    contentColor: Color = Color.Unspecified,
    inset: Boolean = false,
    popup: Boolean = false,
    content: @Composable BoxScope.() -> Unit,
) {
    val isDark = MaterialTheme.colorScheme.surface.luminance() < HALF
    val base = if (popup) kineticaSurface(isDark).forPopup() else KineticaTheme.surface
    val ink = when {
        contentColor.isSpecified -> contentColor
        color.isSpecified -> contentColorFor(color)
        else -> base.contentColor
    }
    val solid = (base as? SurfaceStyle.Solid)?.takeIf { color.isSpecified }
    val surface = solid?.copy(color = color, contentColor = ink) ?: base
    val background = when {
        inset && color.isSpecified -> Modifier.background(color, shape)
        // 비칠 배경을 넘기지 않아야 시트 안에서 연 창에서도 판 위 한 겹으로 묻히지 않는다.
        popup -> Modifier.kSurface(surface, shape, backdrop = null)
        else -> Modifier.kSurface(surface, shape)
    }
    Box(
        modifier
            .then(background)
            .clip(shape)
            .semantics { isTraversalGroup = true },
        propagateMinConstraints = true,
    ) {
        CompositionLocalProvider(LocalContentColor provides ink, LocalKineticaContentColor provides ink) { content() }
    }
}

private const val HALF = 0.5f
