package com.naury.chageun.core.testing

import com.naury.chageun.core.domain.weather.LastWeather
import com.naury.chageun.core.domain.weather.LastWeatherRepository

/** 메모리에 마지막 날씨 하나를 둔다. [saveCount]로 같은 날씨를 다시 쓰지 않는지 확인한다. */
class FakeLastWeatherRepository(var saved: LastWeather? = null) : LastWeatherRepository {
    var saveCount = 0
        private set

    override suspend fun load(): LastWeather? = saved

    override suspend fun save(weather: LastWeather) {
        saved = weather
        saveCount++
    }

    override suspend fun clear() {
        saved = null
    }
}
