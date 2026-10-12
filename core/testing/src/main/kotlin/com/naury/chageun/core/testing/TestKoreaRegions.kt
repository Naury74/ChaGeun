package com.naury.chageun.core.testing

import com.naury.chageun.core.domain.weather.KoreaRegionCatalog
import com.naury.chageun.core.domain.weather.KoreaRegionCheck
import com.naury.chageun.core.domain.weather.KoreaRegionListInfo
import com.naury.chageun.core.domain.weather.KoreaRegionRepository
import com.naury.chageun.core.domain.weather.KoreaRegionRow
import com.naury.chageun.core.domain.weather.KoreaRegionUpdates
import com.naury.chageun.core.model.GeoPoint
import com.naury.chageun.core.model.KoreaArea
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * 테스트용 작은 지역 목록. 앱에 넣은 기상청 목록(2026-07)에서 그대로 옮긴 줄이다.
 * 부산 중구 중앙동과 울산 중구 중앙동처럼 이름이 겹치는 곳, 세종처럼 시·군·구가 따로 없는 곳을 담았다.
 */
object TestKoreaRegions {

    val catalog: KoreaRegionCatalog by lazy { KoreaRegionCatalog(ROWS) }

    val seoul: KoreaArea get() = area("1100000000")
    val gangnam: KoreaArea get() = area("1168000000")
    val yeoksam1: KoreaArea get() = area("1168064000")
    val busan: KoreaArea get() = area("2600000000")
    val busanJungang: KoreaArea get() = area("2611051000")
    val jeju: KoreaArea get() = area("5000000000")
    val daejeon: KoreaArea get() = area("3000000000")
    val incheon: KoreaArea get() = area("2800000000")
    val gangwon: KoreaArea get() = area("5100000000")
    val sejong: KoreaArea get() = area("3600000000")
    val pajang: KoreaArea get() = area("4111156000")

    fun area(code: String): KoreaArea = checkNotNull(catalog.find(code)) { "테스트 목록에 없는 코드 $code" }

    private fun row(code: String, name: String, lat: Double, lon: Double, short: String? = null) =
        KoreaRegionRow(code, name, GeoPoint(lat, lon), short)

    private val ROWS = listOf(
        row("1100000000", "서울특별시", 37.5636, 126.9800, "서울"),
        row("1114000000", "중구", 37.5610, 126.9996),
        row("1114055000", "명동", 37.5572, 126.9879),
        row("1168000000", "강남구", 37.5146, 127.0496),
        row("1168064000", "역삼1동", 37.4926, 127.0351),
        row("1168065000", "역삼2동", 37.4932, 127.0489),
        row("1200000000", "전남광주통합특별시", 34.8130, 126.4650, "전남광주"),
        row("1224000000", "서구", 35.1495, 126.8934),
        row("2600000000", "부산광역시", 35.1770, 129.0770, "부산"),
        row("2611000000", "중구", 35.1032, 129.0345),
        row("2611051000", "중앙동", 35.0982, 129.0376),
        row("2611052000", "동광동", 35.1019, 129.0369),
        row("2700000000", "대구광역시", 35.8685, 128.6036, "대구"),
        row("2800000000", "인천광역시", 37.4532, 126.7074, "인천"),
        row("3000000000", "대전광역시", 36.3471, 127.3866, "대전"),
        row("3011000000", "동구", 36.3241, 127.4349),
        row("3100000000", "울산광역시", 35.5354, 129.3137, "울산"),
        row("3111000000", "중구", 35.5663, 129.3349),
        row("3111058500", "중앙동", 35.5575, 129.3240),
        row("3600000000", "세종특별자치시", 36.4800, 127.2891, "세종"),
        row("3611000000", "세종특별자치시", 36.4800, 127.2891),
        row("3611025000", "조치원읍", 36.6045, 127.2984),
        row("4100000000", "경기도", 37.2718, 127.0117, "경기"),
        row("4111100000", "수원시장안구", 37.3010, 127.0122),
        row("4111156000", "파장동", 37.3056, 126.9969),
        row("4111156600", "율천동", 37.2948, 126.9736),
        row("5000000000", "제주특별자치도", 33.4857, 126.5003, "제주"),
        row("5100000000", "강원특별자치도", 37.8827, 127.7320, "강원"),
    )
}

/** [catalog]를 바로 돌려준다. 몇 번 읽었는지 [loads]로 센다. */
class FakeKoreaRegionRepository(private val catalog: KoreaRegionCatalog = TestKoreaRegions.catalog) :
    KoreaRegionRepository {
    var loads = 0
        private set

    override val listInfo = MutableStateFlow(KoreaRegionListInfo(version = "2026-07"))

    override suspend fun catalog(): KoreaRegionCatalog {
        loads++
        return catalog
    }
}

/** [result]를 돌려주고, 몇 번 불렸는지 센다. */
class FakeKoreaRegionUpdates(var result: KoreaRegionCheck = KoreaRegionCheck.UpToDate) : KoreaRegionUpdates {
    var checks = 0
        private set
    var staleChecks = 0
        private set

    override suspend fun checkNow(): KoreaRegionCheck {
        checks++
        return result
    }

    override suspend fun checkIfStale() {
        staleChecks++
    }
}
