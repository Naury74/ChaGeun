package com.naury.chageun.logging

import android.content.Context
import android.os.Bundle
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics
import com.naury.chageun.core.domain.analytics.AnalyticsEvent
import com.naury.chageun.core.domain.analytics.AnalyticsTracker
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/**
 * [AnalyticsEvent]를 Firebase Analytics로 보낸다. 실제 수집 여부는 사용 통계 설정이 정한다 (UsageStatsSync).
 * google-services.json 없이 빌드하면 Firebase가 초기화되지 않으므로 Debug 로그만 남긴다.
 */
internal class FirebaseAnalyticsTracker @Inject constructor(
    @ApplicationContext context: Context,
    private val logging: LoggingAnalyticsTracker,
) : AnalyticsTracker {

    private val firebase: FirebaseAnalytics? =
        if (FirebaseApp.getApps(context).isNotEmpty()) FirebaseAnalytics.getInstance(context) else null

    override fun track(event: AnalyticsEvent) {
        logging.track(event)
        firebase?.logEvent(
            event.name,
            Bundle().apply {
                event.params.forEach { (key, value) -> putString(key, value) }
            },
        )
    }
}
