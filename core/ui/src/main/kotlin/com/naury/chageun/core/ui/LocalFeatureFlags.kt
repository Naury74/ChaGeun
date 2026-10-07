package com.naury.chageun.core.ui

import androidx.compose.runtime.staticCompositionLocalOf
import com.naury.chageun.core.model.FeatureFlags

/** 화면은 이 값으로 기능 진입점을 숨긴다. 앱 루트에서 [FeatureFlags]를 넣어 준다. */
val LocalFeatureFlags = staticCompositionLocalOf { FeatureFlags() }
