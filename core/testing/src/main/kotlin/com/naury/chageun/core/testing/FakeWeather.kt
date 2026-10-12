package com.naury.chageun.core.testing

import com.naury.chageun.core.domain.ads.AppUsage
import com.naury.chageun.core.domain.ads.AppUsageRepository
import com.naury.chageun.core.domain.weather.ApproximateLocationProvider
import com.naury.chageun.core.domain.weather.AreaNameRepository
import com.naury.chageun.core.domain.weather.WeatherRepository
import com.naury.chageun.core.domain.weather.WeatherSettingsRepository
import com.naury.chageun.core.model.GeoPoint
import com.naury.chageun.core.model.WeatherLocation
import com.naury.chageun.core.model.WeatherSettings
import com.naury.chageun.core.model.WeatherSnapshot
import java.time.Instant
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

class FakeWeatherSettingsRepository(initial: WeatherSettings = WeatherSettings(), areaName: String? = null) :
    WeatherSettingsRepository {
    override val settings = MutableStateFlow(initial)
    override val currentAreaName = MutableStateFlow(areaName)
    override val isCurrentLocationMissing = MutableStateFlow(false)

    override suspend fun setEnabled(enabled: Boolean) {
        settings.update { it.copy(isEnabled = enabled) }
        if (!enabled) {
            currentAreaName.value = null
            isCurrentLocationMissing.value = false
        }
    }

    override suspend fun setLocation(location: WeatherLocation) {
        settings.update { it.copy(isEnabled = true, location = location) }
        currentAreaName.value = null
        isCurrentLocationMissing.value = false
    }

    override suspend fun dismissPrompt() = settings.update { it.copy(isPromptDismissed = true) }

    override suspend fun setCurrentAreaName(name: String?) {
        val isCurrent = settings.value.isEnabled && settings.value.location == WeatherLocation.Current
        currentAreaName.value = name?.takeIf { isCurrent }
    }

    override suspend fun setCurrentLocationMissing(missing: Boolean) {
        val isCurrent = settings.value.isEnabled && settings.value.location == WeatherLocation.Current
        isCurrentLocationMissing.value = missing && isCurrent
    }
}

/** 받은 지점을 [requested]에 모은다. 동네 이름도 반올림한 좌표로만 찾는지 테스트에서 확인한다. */
class FakeAreaNameRepository(var areaName: String? = null) : AreaNameRepository {
    val requested = mutableListOf<GeoPoint>()

    override suspend fun areaNameAt(point: GeoPoint): String? {
        requested += point
        return areaName
    }
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
