package com.naury.chageun.core.ui

import com.google.common.truth.Truth.assertThat
import com.naury.chageun.core.testing.TestKoreaRegions
import org.junit.Test

class RegionPickerRowsTest {

    private val catalog = TestKoreaRegions.catalog

    private fun places(rows: List<RegionRow>) = rows.filterIsInstance<RegionRow.Place>()

    @Test
    fun top_showsRecentsBeforeProvinces_andSkipsUnknownCodes() {
        val rows = regionRows(catalog, RegionDrill(), query = "", recentCodes = listOf("1168064000", "9999999999"))

        assertThat(rows.first()).isEqualTo(RegionRow.Header(R.string.weather_region_recent))
        val recent = rows[1] as RegionRow.Place
        assertThat(recent.area).isEqualTo(TestKoreaRegions.yeoksam1)
        assertThat(recent.opens).isFalse()
        assertThat(recent.showsPath).isTrue()
        assertThat(rows[2]).isEqualTo(RegionRow.Header(R.string.weather_region_provinces))
        assertThat(places(rows).drop(1).all { it.opens }).isTrue()
        // 최근 지역과 시·도 목록에 같은 곳이 있어도 줄 key가 겹치지 않는다.
        val withSeoul = regionRows(catalog, RegionDrill(), "", listOf("1100000000"))
        assertThat(withSeoul.map { it.key }.toSet()).hasSize(withSeoul.size)
    }

    @Test
    fun top_withoutRecents_startsWithProvinces() {
        val rows = regionRows(catalog, RegionDrill(), query = "", recentCodes = emptyList())

        assertThat(rows.first()).isEqualTo(RegionRow.Header(R.string.weather_region_provinces))
    }

    @Test
    fun drill_goesProvinceThenDistrict_withAWholeOptionOnTop() {
        val inSeoul = RegionDrill.at(TestKoreaRegions.seoul)
        val seoulRows = regionRows(catalog, inSeoul, "", emptyList())
        assertThat(seoulRows.first()).isEqualTo(RegionRow.Whole(TestKoreaRegions.seoul))
        assertThat(places(seoulRows).map { it.area.name }).containsExactly("중구", "강남구").inOrder()
        assertThat(places(seoulRows).all { it.opens }).isTrue()

        val inGangnam = RegionDrill.at(TestKoreaRegions.gangnam)
        assertThat(inGangnam.codes).containsExactly("1100000000", "1168000000").inOrder()
        val gangnamRows = regionRows(catalog, inGangnam, "", emptyList())
        assertThat(gangnamRows.first()).isEqualTo(RegionRow.Whole(TestKoreaRegions.gangnam))
        assertThat(places(gangnamRows).map { it.area.name }).containsExactly("역삼1동", "역삼2동").inOrder()
        assertThat(places(gangnamRows).none { it.opens }).isTrue()

        assertThat(inGangnam.back()).isEqualTo(inSeoul)
        assertThat(inSeoul.back().isTop).isTrue()
        assertThat(RegionDrill().back().isTop).isTrue()
    }

    @Test
    fun drill_survivesSavingAsText() {
        val drill = RegionDrill.at(TestKoreaRegions.gangnam)

        assertThat(RegionDrill.decode(drill.encode())).isEqualTo(drill)
        assertThat(RegionDrill.decode("")).isEqualTo(RegionDrill())
        assertThat(drill.current(catalog)).isEqualTo(TestKoreaRegions.gangnam)
    }

    @Test
    fun search_showsFullPaths_andOnlyTownsArePickedDirectly() {
        val rows = regionRows(catalog, RegionDrill.at(TestKoreaRegions.busan), query = "역삼", recentCodes = emptyList())

        assertThat(places(rows).map { it.area.path }).containsExactly("서울 강남구 역삼1동", "서울 강남구 역삼2동")
        assertThat(places(rows).all { it.showsPath && !it.opens }).isTrue()
        assertThat(places(regionRows(catalog, RegionDrill(), "강남", emptyList())).first().opens).isTrue()
        assertThat(regionSearchSummary("역삼", rows)).isEqualTo(RegionSearchSummary(2, isTruncated = false))
        assertThat(regionSearchSummary("", rows)).isNull()
    }
}
