package com.naury.chageun.core.designsystem.motion

import android.provider.Settings
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/** 기획서 19.5: 상태 변화는 180~240ms. */
object ChageunMotion {
    const val SHORT_MS = 180
    const val MEDIUM_MS = 240
    const val STAGGER_MS = 60
}

/** 시스템 '애니메이션 삭제'(배율 0)가 켜져 있으면 true. 이때는 전환을 즉시 끝낸다. */
@Composable
fun rememberReduceMotion(): Boolean {
    val resolver = LocalContext.current.contentResolver
    return remember(resolver) {
        Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
}

/**
 * Reduce Motion이면 즉시 끝나는 spec. 지연이 없으면 iOS처럼 살짝 탄력 있는 스프링으로 움직이고,
 * 순차 등장처럼 [delayMs]가 필요한 경우에는 [durationMs] 동안 감속 곡선으로 움직인다.
 * 스프링의 길이는 [durationMs]에 비례하도록 강성을 고른다.
 */
@Composable
fun <T> motionSpec(durationMs: Int = ChageunMotion.MEDIUM_MS, delayMs: Int = 0): FiniteAnimationSpec<T> = when {
    rememberReduceMotion() -> snap()
    delayMs > 0 -> tween(durationMs, delayMs, FastOutSlowInEasing)
    else -> spring(dampingRatio = SPRING_DAMPING, stiffness = stiffnessFor(durationMs))
}

// 짧은 전환일수록 단단한 스프링을 써 체감 시간을 비슷하게 맞춘다.
private fun stiffnessFor(durationMs: Int): Float = when {
    durationMs <= ChageunMotion.SHORT_MS -> Spring.StiffnessMedium
    durationMs <= ChageunMotion.MEDIUM_MS -> Spring.StiffnessMediumLow
    else -> Spring.StiffnessLow
}

private const val SPRING_DAMPING = 0.85f
