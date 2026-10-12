package com.naury.chageun.data.weather

import com.google.common.truth.Truth.assertThat
import com.naury.chageun.core.domain.weather.KoreaRegionCatalog
import com.naury.chageun.core.domain.weather.KoreaRegionRow
import com.naury.chageun.core.model.KoreaAreaLevel
import java.io.File
import org.junit.Test

/**
 * 앱에 넣은 지역 목록(assets/weather/korea_regions.csv) 자체를 검사한다. 원본을 새 버전으로 바꿀 때
 * 좌표가 빠졌거나 부모 없는 동이 생기면 여기서 걸린다.
 */
class KoreaRegionDatasetTest {

    private val rows: List<KoreaRegionRow> =
        File("src/main/assets/${KoreaRegionCsv.ASSET_PATH}").useLines(block = KoreaRegionCsv::parse)
    private val catalog = KoreaRegionCatalog(rows)

    @Test
    fun coversTheWholeCountry() {
        val levels = rows.groupingBy { KoreaRegionCatalog.levelOf(it.code) }.eachCount()
        assertThat(rows.size).isAtLeast(MIN_ROWS)
        assertThat(levels[KoreaAreaLevel.Province]).isAtLeast(MIN_PROVINCES)
        assertThat(levels[KoreaAreaLevel.District]).isAtLeast(MIN_DISTRICTS)
        assertThat(levels[KoreaAreaLevel.Town]).isAtLeast(MIN_TOWNS)
    }

    @Test
    fun codesAreUniqueTenDigitNumbers() {
        assertThat(rows.map { it.code }.toSet()).hasSize(rows.size)
        assertThat(rows.filterNot { it.code.matches(CODE) }).isEmpty()
        assertThat(rows.filter { it.name.isBlank() }).isEmpty()
    }

    @Test
    fun everyPointIsInsideKorea() {
        val outside = rows.filterNot {
            it.point.latitude in MIN_LAT..MAX_LAT && it.point.longitude in MIN_LON..MAX_LON
        }
        assertThat(outside).isEmpty()
    }

    @Test
    fun everyDistrictAndTownHasAParent_andEveryProvinceAShortName() {
        val codes = rows.map { it.code }.toSet()
        val orphans = rows.filter { row -> KoreaRegionCatalog.parentCode(row.code)?.let { it !in codes } ?: false }
        assertThat(orphans).isEmpty()
        val provinces = rows.filter { KoreaRegionCatalog.levelOf(it.code) == KoreaAreaLevel.Province }
        assertThat(provinces.filter { it.shortName.isNullOrBlank() }).isEmpty()
        val districts = rows.filter { KoreaRegionCatalog.levelOf(it.code) == KoreaAreaLevel.District }
        assertThat(
            districts.filter {
                catalog.children(it.code).isEmpty() && catalog.find(it.code)?.districtName != null
            },
        )
            .isEmpty()
    }

    @Test
    fun townLabelsAreUnambiguous() {
        val towns = rows.filter { KoreaRegionCatalog.levelOf(it.code) == KoreaAreaLevel.Town }
            .mapNotNull { catalog.find(it.code) }
        assertThat(towns.map { it.path }.toSet()).hasSize(towns.size)
        assertThat(towns.map { it.label }.toSet()).hasSize(towns.size)
    }

    @Test
    fun knownPlacesLookRight() {
        assertThat(catalog.find("1168064000")?.label).isEqualTo("강남구 역삼1동")
        assertThat(catalog.find("4111100000")?.path).isEqualTo("경기 수원시 장안구")
        assertThat(catalog.search("역삼1동").first().path).isEqualTo("서울 강남구 역삼1동")
    }

    private companion object {
        val CODE = Regex("\\d{10}")
        const val MIN_ROWS = 3_500
        const val MIN_PROVINCES = 16
        const val MIN_DISTRICTS = 250
        const val MIN_TOWNS = 3_000

        // 남쪽 마라도(33.1)부터 북쪽 강원 고성·연천(38.5), 서쪽 백령도(124.7)부터 동쪽 독도(131.9)까지.
        const val MIN_LAT = 33.0
        const val MAX_LAT = 38.7
        const val MIN_LON = 124.5
        const val MAX_LON = 132.0
    }
}
