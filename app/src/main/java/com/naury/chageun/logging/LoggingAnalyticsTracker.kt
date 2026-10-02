package com.naury.chageun.logging

import com.naury.chageun.core.common.logging.AppLogger
import com.naury.chageun.core.common.logging.LogField
import com.naury.chageun.core.domain.analytics.AnalyticsEvent
import com.naury.chageun.core.domain.analytics.AnalyticsTracker
import javax.inject.Inject

/**
 * 외부 전송 SDK를 붙이기 전까지 이벤트를 Debug 로그로만 남긴다. AppLogger가 Release에서 debug를 버리므로
 * Release 빌드는 아무것도 보내거나 기록하지 않는다.
 */
internal class LoggingAnalyticsTracker @Inject constructor(private val logger: AppLogger) : AnalyticsTracker {
    // 사용자 동작마다 한 번 불리므로 spread 복사 비용은 무시할 수 있다.
    @Suppress("SpreadOperator")
    override fun track(event: AnalyticsEvent) {
        val fields = event.params.map { (key, value) -> LogField.EventParam(key, value) }
        logger.debug("analytics_${event.name}", *fields.toTypedArray())
    }
}
