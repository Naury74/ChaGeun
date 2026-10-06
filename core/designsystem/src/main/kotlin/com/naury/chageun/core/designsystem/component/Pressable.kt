package com.naury.chageun.core.designsystem.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import com.naury.chageun.core.designsystem.motion.rememberReduceMotion

/** 누를 때의 반응. 따로 떠 있는 카드는 작아지고, 카드 안의 목록 행은 회색으로 강조된다. */
enum class PressStyle { Scale, Highlight }

/**
 * Material 물결 대신 iOS처럼 누르는 동안 반응하는 클릭 영역.
 * [haptic]을 주면 확정 동작에 가벼운 진동을 더한다. Reduce Motion이면 크기 변화는 하지 않는다.
 */
fun Modifier.pressable(
    onClick: () -> Unit,
    style: PressStyle = PressStyle.Scale,
    enabled: Boolean = true,
    role: Role? = Role.Button,
    onClickLabel: String? = null,
    haptic: HapticFeedbackType? = null,
): Modifier = composed {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val haptics = LocalHapticFeedback.current
    val pressedModifier = when (style) {
        PressStyle.Scale -> {
            val reduceMotion = rememberReduceMotion()
            val scale by animateFloatAsState(
                targetValue = if (pressed && !reduceMotion) PRESSED_SCALE else 1f,
                animationSpec = spring(dampingRatio = PRESS_DAMPING, stiffness = Spring.StiffnessMedium),
                label = "press-scale",
            )
            Modifier.graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
        }
        PressStyle.Highlight -> {
            val highlight by animateColorAsState(
                if (pressed) MaterialTheme.colorScheme.onSurface.copy(alpha = HIGHLIGHT_ALPHA) else Color.Transparent,
                label = "press-highlight",
            )
            Modifier.background(highlight)
        }
    }
    this
        .then(pressedModifier)
        .clickable(
            interactionSource = interaction,
            indication = null,
            enabled = enabled,
            onClickLabel = onClickLabel,
            role = role,
        ) {
            haptic?.let(haptics::performHapticFeedback)
            onClick()
        }
}

private const val PRESSED_SCALE = 0.97f
private const val PRESS_DAMPING = 0.6f
private const val HIGHLIGHT_ALPHA = 0.08f
