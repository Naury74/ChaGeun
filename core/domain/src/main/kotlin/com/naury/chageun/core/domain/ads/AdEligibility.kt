package com.naury.chageun.core.domain.ads

import java.time.Duration
import java.time.Instant
import kotlinx.coroutines.flow.Flow

/**
 * 첫 실행 시각과 실행 횟수. 광고 노출 자격(기획서 17.3)과 홈 인사말("오랜만이에요")에 쓴다.
 * [lastLaunchAt]은 가장 최근 실행, [previousLaunchAt]은 그 바로 전 실행이다.
 */
data class AppUsage(
    val firstLaunchAt: Instant?,
    val launchCount: Int,
    val lastLaunchAt: Instant? = null,
    val previousLaunchAt: Instant? = null,
)

interface AppUsageRepository {
    val usage: Flow<AppUsage>

    suspend fun recordLaunch(at: Instant)
}

/**
 * 기획서 17.3 노출 자격. 차량을 등록했고, 설치 후 24시간이 지났거나 두 번 이상 실행했으며,
 * UMP 결과상 광고를 요청할 수 있을 때만 광고를 보여 준다.
 */
object AdEligibility {
    private val MIN_INSTALL_AGE: Duration = Duration.ofHours(24)
    private const val MIN_LAUNCH_COUNT = 2

    fun isEligible(hasVehicle: Boolean, usage: AppUsage, canRequestAds: Boolean, now: Instant): Boolean {
        if (!hasVehicle || !canRequestAds) return false
        val installedLongEnough = usage.firstLaunchAt?.let { !it.plus(MIN_INSTALL_AGE).isAfter(now) } ?: false
        return installedLongEnough || usage.launchCount >= MIN_LAUNCH_COUNT
    }
}
