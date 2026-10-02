package com.naury.chageun.logging

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics
import com.naury.chageun.core.domain.settings.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach

/**
 * Manifest는 수집을 꺼 둔 채로 시작한다. 설정을 읽은 뒤에만 켜므로, 사용자가 끈 상태에서는
 * 앱 시작 직후에도 이벤트가 나가지 않는다.
 */
class UsageStatsSync @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository,
) {
    fun start(scope: CoroutineScope) {
        if (FirebaseApp.getApps(context).isEmpty()) return
        val analytics = FirebaseAnalytics.getInstance(context)
        settingsRepository.settings
            .map { it.isUsageStatsEnabled }
            .distinctUntilChanged()
            .onEach(analytics::setAnalyticsCollectionEnabled)
            .launchIn(scope)
    }
}
