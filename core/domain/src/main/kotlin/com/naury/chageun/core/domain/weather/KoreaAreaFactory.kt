package com.naury.chageun.core.domain.weather

import com.naury.chageun.core.model.KoreaArea
import com.naury.chageun.core.model.KoreaAreaLevel

/**
 * 목록의 한 지역과 검색에 쓰는 원래 이름. [rawPath]는 "서울특별시 강남구 역삼1동"처럼 원본 이름을 이은 것이다.
 * [isPassThrough]는 세종특별자치시 아래 세종특별자치시처럼 시·도와 같은 이름의 시·군·구다.
 */
internal data class KoreaAreaEntry(val area: KoreaArea, val rawPath: String, val isPassThrough: Boolean)

/** 목록 줄을 [KoreaArea]로 바꾸고, 홈에 쓸 짧은 이름을 정한다. */
internal object KoreaAreaFactory {

    fun build(rows: List<KoreaRegionRow>): List<KoreaAreaEntry> {
        val byCode = rows.associateBy { it.code }
        val drafts = rows.map { draft(it, byCode) }
        // 읍·면·동은 "강남구 역삼1동"이 전국에 하나뿐이면 그렇게, 아니면 더 길게 쓴다.
        val counts = drafts.filter { it.area.level == KoreaAreaLevel.Town }
            .flatMap { townLabels(it.area).distinct() }
            .groupingBy { it }
            .eachCount()
        return drafts.map { it.copy(area = it.area.copy(label = labelFor(it.area, counts))) }
    }

    /** 구가 있는 시의 원래 이름을 띄어 쓴다(수원시장안구 → 수원시 장안구, 포항시남구 → 포항시 남구). */
    fun displayDistrict(raw: String): String =
        CITY_WARD.matchEntire(raw)?.let { "${it.groupValues[1]} ${it.groupValues[2]}" } ?: raw

    private fun draft(row: KoreaRegionRow, byCode: Map<String, KoreaRegionRow>): KoreaAreaEntry {
        val level = KoreaRegionCatalog.levelOf(row.code)
        val provinceRow = byCode[KoreaRegionCatalog.provinceCodeOf(row.code)] ?: row
        val districtRow = if (level ==
            KoreaAreaLevel.Province
        ) {
            null
        } else {
            byCode[KoreaRegionCatalog.districtCodeOf(row.code)]
        }
        val isPassThrough = districtRow?.name == provinceRow.name
        val area = KoreaArea(
            code = row.code,
            level = level,
            provinceName = provinceRow.shortName ?: provinceRow.name,
            districtName = districtRow?.takeUnless { isPassThrough }?.name?.let(::displayDistrict),
            townName = row.name.takeIf { level == KoreaAreaLevel.Town },
            point = row.point,
            label = "",
        )
        val rawPath = listOfNotNull(provinceRow.name, districtRow?.name, area.townName).distinct().joinToString(" ")
        return KoreaAreaEntry(area, rawPath, isPassThrough && level == KoreaAreaLevel.District)
    }

    private fun labelFor(area: KoreaArea, counts: Map<String, Int>): String = when (area.level) {
        KoreaAreaLevel.Province -> area.provinceName
        KoreaAreaLevel.District -> area.path
        KoreaAreaLevel.Town -> townLabels(area).firstOrNull { counts[it] == 1 } ?: area.path
    }

    /** 짧은 것부터: 구 이름 + 동(장안구 파장동), 시·군·구 + 동(수원시 장안구 파장동), 시·도부터 전부. */
    private fun townLabels(area: KoreaArea): List<String> {
        val district = area.districtName ?: return listOf(area.path)
        val ward = district.substringAfterLast(' ')
        return listOf("$ward ${area.townName}", "$district ${area.townName}", area.path)
    }

    private val CITY_WARD = Regex("^(.+?시)(.+구)$")
}
