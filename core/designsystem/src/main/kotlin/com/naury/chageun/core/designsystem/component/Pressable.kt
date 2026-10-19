package com.naury.chageun.core.designsystem.component

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.naury.chageun.core.designsystem.motion.rememberReduceMotion
import com.naury.kinetica.ui.KIndication
import com.naury.kinetica.ui.jellyPress

/** 누를 때의 반응. 따로 떠 있는 카드는 젤리처럼 눌리고, 카드 안의 목록 행은 옅은 덮개로 강조된다. */
enum class PressStyle { Scale, Highlight }

/**
 * Material 물결 대신 Kinetica 누름 효과를 쓰는 클릭 영역.
 * [PressStyle.Scale]은 납작해졌다가 출렁이며 돌아오고(Kinetica jellyPress), 두 방식 모두 [shape] 모양으로 잘린
 * 옅은 덮개(KIndication)가 덮였다 사라진다. [haptic]을 주면 확정 동작에 가벼운 진동을 더한다.
 * Reduce Motion이면 크기 변화는 하지 않는다.
 *
 * 키보드로 초점이 오면 테마 색 테두리를 그린다(기획 §28). 터치로 누를 때는 초점을 받지 않아
 * 나타나지 않는다. 떠 있는 카드([PressStyle.Scale])는 카드 모서리에 맞추고, 카드 안 목록 행은 행 안쪽에 그린다.
 *
 * @param shape 덮개와 초점 테두리 모양. null이면 카드는 큰 모서리, 목록 행은 부모가 자른 사각형을 쓴다.
 */
fun Modifier.pressable(
    onClick: () -> Unit,
    style: PressStyle = PressStyle.Scale,
    enabled: Boolean = true,
    role: Role? = Role.Button,
    onClickLabel: String? = null,
    haptic: HapticFeedbackType? = null,
    shape: Shape? = null,
): Modifier = composed {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val ringColor = MaterialTheme.colorScheme.primary
    val ringShape = shape ?: if (style == PressStyle.Scale) MaterialTheme.shapes.large else MaterialTheme.shapes.small
    // 목록 행은 카드가 모서리를 잘라 주므로 덮개는 행 전체를 칠한다.
    val overlayShape = shape ?: if (style == PressStyle.Scale) MaterialTheme.shapes.large else RectangleShape
    val haptics = LocalHapticFeedback.current
    val reduceMotion = rememberReduceMotion()
    val motion = if (style == PressStyle.Scale && enabled && !reduceMotion) {
        Modifier.jellyPress(CARD_JELLY)
    } else {
        Modifier
    }
    this
        .then(motion)
        .then(if (focused) Modifier.border(FOCUS_RING_WIDTH, ringColor, ringShape) else Modifier)
        .clickable(
            interactionSource = interaction,
            indication = KIndication(overlayShape),
            enabled = enabled,
            onClickLabel = onClickLabel,
            role = role,
        ) {
            haptic?.let(haptics::performHapticFeedback)
            onClick()
        }
}

private val FOCUS_RING_WIDTH = 2.dp

// 버튼(0.07)보다 약하게 눌러 넓은 카드가 옆 여백을 넘지 않게 한다.
private const val CARD_JELLY = 0.04f
