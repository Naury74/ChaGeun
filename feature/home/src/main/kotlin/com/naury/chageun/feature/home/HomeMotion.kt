package com.naury.chageun.feature.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import com.naury.kinetica.presets.EffectPresets
import com.naury.kinetica.render.kineticaEffects
import com.naury.kinetica.ui.KStaggerState
import com.naury.kinetica.ui.kClickable
import com.naury.kinetica.ui.staggerIn

/** 칸 하나 안에서 항목이 떠오르는 순서. 목록을 만들 때 위에서부터 0, 1, 2…를 나눠 준다. */
internal class AppearOrder {
    private var next = 0

    fun next(): Int = next++
}

/**
 * 홈 칸마다 하나씩 두는 차례 등장 기억. null이면 이미 한 번 보여 준 홈이라 바로 그린다.
 * 탭을 오갈 때마다 카드가 다시 떠오르면 정신없어서 처음 열 때만 쓴다.
 */
internal val LocalHomeStagger = staticCompositionLocalOf<KStaggerState?> { null }

/** [index] 순서대로 아래에서 살짝 떠오르며 나타난다. 스크롤해 다시 들어온 카드는 다시 떠오르지 않는다. */
@Composable
internal fun Modifier.homeAppear(index: Int): Modifier {
    val stagger = LocalHomeStagger.current ?: return this
    return staggerIn(index, stagger)
}

/**
 * 통째로 눌리는 카드. 누른 쪽으로 살짝 기울며 작아지고, 모양에 맞춘 옅은 눌림 표시가 덮인다.
 * 차례 등장은 [homeAppear]가 맡으므로 카드 프리셋의 등장 효과는 뺀다.
 */
@Composable
internal fun Modifier.pressableCard(shape: Shape, onClick: () -> Unit): Modifier = this
    .kineticaEffects(CardPress, shape)
    .kClickable(shape, onClick = onClick)

private val CardPress = EffectPresets.card.copy(appear = null)
