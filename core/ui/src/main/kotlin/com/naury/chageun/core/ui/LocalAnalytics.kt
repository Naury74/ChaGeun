package com.naury.chageun.core.ui

import androidx.compose.runtime.staticCompositionLocalOf
import com.naury.chageun.core.domain.analytics.AnalyticsTracker
import com.naury.chageun.core.domain.analytics.NoOpAnalyticsTracker

/** ViewModel을 거치지 않는 화면 이벤트(링크 열기, 다른 탭으로 이동)용. MainActivity가 실제 구현을 제공한다. */
val LocalAnalyticsTracker = staticCompositionLocalOf<AnalyticsTracker> { NoOpAnalyticsTracker }
