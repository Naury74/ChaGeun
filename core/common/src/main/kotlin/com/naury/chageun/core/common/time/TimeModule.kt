package com.naury.chageun.core.common.time

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

@Module
@InstallIn(SingletonComponent::class)
object TimeModule {
    @Provides
    fun provideClock(): Clock = SystemZoneClock
}

/**
 * 기기 시각과 시간대를 읽을 때마다 새로 보는 시계. [Clock.systemDefaultZone]은 만들 때의 시간대에 묶여,
 * 앱을 켜 둔 채 시간대를 바꾸면 홈 인사말과 해 지는 시각이 예전 시간대로 계산된다.
 */
internal object SystemZoneClock : Clock() {
    override fun getZone(): ZoneId = ZoneId.systemDefault()

    override fun withZone(zone: ZoneId): Clock = system(zone)

    override fun instant(): Instant = Instant.now()

    override fun millis(): Long = System.currentTimeMillis()
}
