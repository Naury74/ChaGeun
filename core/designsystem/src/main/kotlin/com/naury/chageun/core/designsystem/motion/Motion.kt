package com.naury.chageun.core.designsystem.motion

import android.provider.Settings
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.snap
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

/** Reduce Motion이면 즉시, 아니면 [durationMs] 동안 감속 곡선으로 움직이는 spec. */
@Composable
fun <T> motionSpec(durationMs: Int = ChageunMotion.MEDIUM_MS, delayMs: Int = 0): FiniteAnimationSpec<T> =
    if (rememberReduceMotion()) snap() else tween(durationMs, delayMs, FastOutSlowInEasing)
