package com.naury.chageun.core.testing

import com.naury.chageun.core.domain.ads.AppUsage
import com.naury.chageun.core.domain.ads.AppUsageRepository
import com.naury.chageun.core.domain.weather.ApproximateLocationProvider
import com.naury.chageun.core.domain.weather.WeatherRepository
import com.naury.chageun.core.domain.weather.WeatherSettingsRepository
import com.naury.chageun.core.model.GeoPoint
import com.naury.chageun.core.model.WeatherLocation
import com.naury.chageun.core.model.WeatherSettings
import com.naury.chageun.core.model.WeatherSnapshot
import java.time.Instant
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

class FakeWeatherSettingsRepository(initial: WeatherSettings = WeatherSettings()) : WeatherSettingsRepository {
    override val settings = MutableStateFlow(initial)

    override suspend fun setEnabled(enabled: Boolean) = settings.update { it.copy(isEnabled = enabled) }

    override suspend fun setLocation(location: WeatherLocation) =
        settings.update { it.copy(isEnabled = true, location = location) }

    override suspend fun dismissPrompt() = settings.update { it.copy(isPromptDismissed = true) }
}

/** 받은 지점을 [requested]에 모은다. 반올림한 값만 넘어오는지 테스트에서 확인한다. */
class FakeWeatherRepository(var snapshot: WeatherSnapshot? = null) : WeatherRepository {
    val requested = mutableListOf<GeoPoint>()

    override suspend fun weatherAt(point: GeoPoint): WeatherSnapshot? {
        requested += point
        return snapshot
    }
}

class FakeApproximateLocationProvider(var permitted: Boolean = false, var location: GeoPoint? = null) :
    ApproximateLocationProvider {
    override fun hasPermission(): Boolean = permitted

    override suspend fun currentLocation(): GeoPoint? = location.takeIf { permitted }
}

class FakeAppUsageRepository(initial: AppUsage = AppUsage(firstLaunchAt = null, launchCount = 0)) : AppUsageRepository {
    override val usage = MutableStateFlow(initial)

    override suspend fun recordLaunch(at: Instant) = usage.update {
        AppUsage(
            firstLaunchAt = it.firstLaunchAt ?: at,
            launchCount = it.launchCount + 1,
            lastLaunchAt = at,
            previousLaunchAt = it.lastLaunchAt,
        )
    }
}
