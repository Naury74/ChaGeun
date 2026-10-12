package com.naury.chageun.core.domain.weather

import com.google.common.truth.Truth.assertThat
import com.naury.chageun.core.model.KoreaAreaLevel
import com.naury.chageun.core.testing.TestKoreaRegions
import org.junit.Test

class KoreaRegionCatalogTest {

    private val catalog = TestKoreaRegions.catalog

    @Test
    fun levelsAndParents_comeFromTheCode() {
        assertThat(KoreaRegionCatalog.levelOf("1100000000")).isEqualTo(KoreaAreaLevel.Province)
        assertThat(KoreaRegionCatalog.levelOf("1168000000")).isEqualTo(KoreaAreaLevel.District)
        assertThat(KoreaRegionCatalog.levelOf("1168064000")).isEqualTo(KoreaAreaLevel.Town)
        assertThat(KoreaRegionCatalog.parentCode("1168064000")).isEqualTo("1168000000")
        assertThat(KoreaRegionCatalog.parentCode("1168000000")).isEqualTo("1100000000")
        assertThat(KoreaRegionCatalog.parentCode("1100000000")).isNull()
    }

    @Test
    fun labels_areTheShortestUnambiguousName() {
        assertThat(TestKoreaRegions.seoul.label).isEqualTo("서울")
        assertThat(TestKoreaRegions.gangnam.label).isEqualTo("서울 강남구")
        assertThat(TestKoreaRegions.yeoksam1.label).isEqualTo("강남구 역삼1동")
        // 구가 있는 시는 띄어 쓰고, 동 이름에는 구 이름만 붙인다.
        assertThat(TestKoreaRegions.area("4111100000").label).isEqualTo("경기 수원시 장안구")
        assertThat(TestKoreaRegions.pajang.label).isEqualTo("장안구 파장동")
        // 부산 중구 중앙동과 울산 중구 중앙동은 시·도까지 붙여야 구분된다.
        assertThat(TestKoreaRegions.busanJungang.label).isEqualTo("부산 중구 중앙동")
        assertThat(TestKoreaRegions.area("3111058500").label).isEqualTo("울산 중구 중앙동")
        // 세종은 시·군·구가 따로 없다.
        assertThat(TestKoreaRegions.area("3611025000").label).isEqualTo("세종 조치원읍")
    }

    @Test
    fun path_startsFromTheProvince() {
        assertThat(TestKoreaRegions.yeoksam1.path).isEqualTo("서울 강남구 역삼1동")
        assertThat(TestKoreaRegions.pajang.path).isEqualTo("경기 수원시 장안구 파장동")
        assertThat(TestKoreaRegions.area("1224000000").path).isEqualTo("전남광주 서구")
        assertThat(TestKoreaRegions.sejong.path).isEqualTo("세종")
    }

    @Test
    fun children_skipSejongsSameNamedDistrict() {
        assertThat(catalog.provinces.map { it.name }).containsAtLeast("서울", "부산", "세종", "제주").inOrder()
        assertThat(catalog.children("1100000000").map { it.name }).containsExactly("중구", "강남구").inOrder()
        assertThat(catalog.children("1168000000").map { it.name }).containsExactly("역삼1동", "역삼2동").inOrder()
        assertThat(catalog.children("3600000000").map { it.name }).containsExactly("조치원읍")
        assertThat(catalog.children("1168064000")).isEmpty()
    }

    @Test
    fun resolve_fallsBackToTheNearestParentWhenACodeDisappears() {
        assertThat(catalog.resolve("1168099999")).isEqualTo(TestKoreaRegions.gangnam)
        assertThat(catalog.resolve("1199000000")).isEqualTo(TestKoreaRegions.seoul)
        assertThat(catalog.resolve("9900000000")).isNull()
    }

    @Test
    fun search_matchesPartOfAnyName() {
        assertThat(catalog.search("역삼").map { it.path }).containsExactly("서울 강남구 역삼1동", "서울 강남구 역삼2동")
        // 정식 이름(서울특별시)으로도, 시·도와 동을 함께 써도 찾는다.
        assertThat(catalog.search("서울특별시 역삼2").map { it.code }).containsExactly("1168065000")
        assertThat(catalog.search("  서울   역삼1 ").map { it.code }).containsExactly("1168064000")
        assertThat(catalog.search("수원 장안").first().code).isEqualTo("4111100000")
        assertThat(catalog.search("수원시장안구 파장").map { it.code }).containsExactly("4111156000")
        assertThat(catalog.search("없는동네")).isEmpty()
        assertThat(catalog.search("   ")).isEmpty()
    }

    @Test
    fun search_ranksOwnNameMatchesFirst_andSupportsInitials() {
        // "중구"로 시작하는 시·군·구가 그 아래 동보다 앞에 온다.
        val jung = catalog.search("중구")
        assertThat(jung.take(3).map { it.level }).containsExactly(
            KoreaAreaLevel.District,
            KoreaAreaLevel.District,
            KoreaAreaLevel.District,
        )
        assertThat(catalog.search("ㄱㄴㄱ").first()).isEqualTo(TestKoreaRegions.gangnam)
        assertThat(catalog.search("ㅇㅅ1").map { it.code }).containsExactly("1168064000")
    }

    @Test
    fun search_leavesOutSejongsSameNamedDistrict_andHonoursTheLimit() {
        assertThat(catalog.search("세종").map { it.code }).containsExactly("3600000000", "3611025000").inOrder()
        assertThat(catalog.search("동", limit = 2)).hasSize(2)
    }
}
