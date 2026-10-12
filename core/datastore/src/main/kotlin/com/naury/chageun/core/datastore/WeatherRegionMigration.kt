package com.naury.chageun.core.datastore

import androidx.datastore.core.DataMigration
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey

/**
 * 예전 버전이 저장한 시·도 이름(Busan)을 행정구역코드(2600000000)로 바꾼다. 처음 읽을 때 한 번만 돈다.
 *
 * 광주광역시는 지역 목록(2026-07)에서 전남광주통합특별시 아래 구가 되어, 예전 대표 지점(시청)이 있는 서구로 옮긴다.
 * 전라남도는 같은 대표 지점(무안 도청)인 전남광주통합특별시로 옮긴다.
 */
internal object WeatherRegionMigration : DataMigration<Preferences> {

    const val CURRENT = "current"
    val LOCATION = stringPreferencesKey("weather_location")

    private val LEGACY_CODES: Map<String, String> = mapOf(
        "Seoul" to "1100000000",
        "Busan" to "2600000000",
        "Daegu" to "2700000000",
        "Incheon" to "2800000000",
        "Gwangju" to "1224000000",
        "Daejeon" to "3000000000",
        "Ulsan" to "3100000000",
        "Sejong" to "3600000000",
        "Gyeonggi" to "4100000000",
        "Gangwon" to "5100000000",
        "Chungbuk" to "4300000000",
        "Chungnam" to "4400000000",
        "Jeonbuk" to "5200000000",
        "Jeonnam" to "1200000000",
        "Gyeongbuk" to "4700000000",
        "Gyeongnam" to "4800000000",
        "Jeju" to "5000000000",
    )

    /** 예전 시·도 이름이면 코드로 바꾸고, 아니면 그대로 둔다. */
    fun codeFor(stored: String): String = LEGACY_CODES[stored] ?: stored

    override suspend fun shouldMigrate(currentData: Preferences): Boolean = currentData[LOCATION] in LEGACY_CODES

    override suspend fun migrate(currentData: Preferences): Preferences {
        val stored = currentData[LOCATION] ?: return currentData
        return currentData.toMutablePreferences().apply { this[LOCATION] = codeFor(stored) }.toPreferences()
    }

    override suspend fun cleanUp() = Unit
}
