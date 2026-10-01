package com.naury.chageun.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
data class ChageunSpacing(
    val xxs: Dp = 4.dp,
    val xs: Dp = 8.dp,
    val sm: Dp = 12.dp,
    val md: Dp = 16.dp,
    val lg: Dp = 24.dp,
    val xl: Dp = 32.dp,
    val gutter: Dp = 16.dp,
    val paneGap: Dp = 16.dp,
    val minTouchTarget: Dp = 48.dp,
)

object Gutters {
    val Compact = ChageunSpacing(gutter = 16.dp, paneGap = 16.dp)
    val Medium = ChageunSpacing(gutter = 24.dp, paneGap = 24.dp)
    val Expanded = ChageunSpacing(gutter = 32.dp, paneGap = 24.dp)
}

internal val LocalSpacing = staticCompositionLocalOf { Gutters.Compact }
