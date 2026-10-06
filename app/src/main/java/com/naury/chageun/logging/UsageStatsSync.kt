package com.naury.chageun.logging

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.naury.chageun.BuildConfig
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
 * 앱 시작 직후에도 이벤트나 비정상 종료 보고가 나가지 않는다.
 * Crashlytics는 개발 중 비정상 종료가 섞이지 않도록 release 계열 빌드에서만 켠다.
 */
class UsageStatsSync @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository,
) {
    fun start(scope: CoroutineScope) {
        if (FirebaseApp.getApps(context).isEmpty()) return
        val analytics = FirebaseAnalytics.getInstance(context)
        val crashlytics = FirebaseCrashlytics.getInstance()
        settingsRepository.settings
            .map { it.isUsageStatsEnabled }
            .distinctUntilChanged()
            .onEach { enabled ->
                analytics.setAnalyticsCollectionEnabled(enabled)
                val sendCrashReports = enabled && !BuildConfig.DEBUG
                crashlytics.setCrashlyticsCollectionEnabled(sendCrashReports)
                // 수집을 끈 동안 기기에 쌓인 보고는 나중에 켜더라도 보내지 않는다.
                if (!sendCrashReports) crashlytics.deleteUnsentReports()
            }
            .launchIn(scope)
    }
}
