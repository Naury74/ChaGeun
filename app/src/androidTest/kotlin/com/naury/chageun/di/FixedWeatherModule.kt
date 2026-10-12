package com.naury.chageun.di

import com.naury.chageun.core.domain.weather.WeatherRepository
import com.naury.chageun.core.model.GeoPoint
import com.naury.chageun.core.model.WeatherCondition
import com.naury.chageun.core.model.WeatherSnapshot
import com.naury.chageun.data.weather.WeatherRepositoryModule
import dagger.Binds
import dagger.Module
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import java.time.Instant
import javax.inject.Inject

/** 모든 흐름 테스트에서 MET Norway 대신 [FixedWeatherRepository]를 쓴다. 지역 목록·위치는 실제 구현 그대로다. */
@Module
@TestInstallIn(components = [SingletonComponent::class], replaces = [WeatherRepositoryModule::class])
interface FixedWeatherModule {
    @Binds
    fun bindWeatherRepository(repository: FixedWeatherRepository): WeatherRepository
}

/**
 * 어느 지점을 물어도 같은 날씨(18° 비)를 돌려준다. CI 기기의 네트워크 상태나 실제 날씨에 따라 홈 화면이 달라지지 않는다.
 * 받은 좌표는 남기지 않는다.
 */
class FixedWeatherRepository @Inject constructor() : WeatherRepository {
    override suspend fun weatherAt(point: GeoPoint): WeatherSnapshot = WeatherSnapshot(
        condition = CONDITION,
        temperatureC = TEMPERATURE_C,
        windSpeedMs = WIND_SPEED_MS,
        isNight = false,
        observedAt = Instant.now(),
    )

    companion object {
        val CONDITION = WeatherCondition.Rain
        const val TEMPERATURE_C = 18.0
        private const val WIND_SPEED_MS = 2.0
    }
}
