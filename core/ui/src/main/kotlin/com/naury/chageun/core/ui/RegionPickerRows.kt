package com.naury.chageun.core.ui

import androidx.annotation.StringRes
import com.naury.chageun.core.domain.weather.KoreaRegionCatalog
import com.naury.chageun.core.model.KoreaArea
import com.naury.chageun.core.model.KoreaAreaLevel

/** 지역 고르기 창 목록의 한 줄. [key]는 LazyColumn이 줄을 구분하는 값이다. */
internal sealed interface RegionRow {
    val key: String

    /** "최근에 고른 지역", "시·도" 같은 묶음 제목. */
    data class Header(@StringRes val textRes: Int) : RegionRow {
        override val key: String get() = "header-$textRes"
    }

    /** "서울 강남구 전체"처럼 지금 보고 있는 시·도나 시·군·구 전체를 고르는 줄. */
    data class Whole(val area: KoreaArea) : RegionRow {
        override val key: String get() = "whole-${area.code}"
    }

    /**
     * 지역 하나. [opens]면 누를 때 아래 단계로 내려가고, 아니면 바로 고른다.
     * [showsPath]면 이름 아래에 시·도부터 이은 전체 이름을 함께 쓴다(검색 결과, 최근 지역).
     */
    data class Place(val area: KoreaArea, val opens: Boolean, val showsPath: Boolean, val group: String = "") :
        RegionRow {
        override val key: String get() = "$group-${area.code}"
    }
}

/**
 * 지역 고르기 창이 지금 어디를 보고 있는지. [codes]는 위에서부터 내려온 시·도, 시·군·구 코드다.
 * 화면 회전에도 남도록 코드만 담는다.
 */
internal data class RegionDrill(val codes: List<String> = emptyList()) {

    val isTop: Boolean get() = codes.isEmpty()

    /** 한 단계 위로. 맨 위면 그대로다. */
    fun back(): RegionDrill = RegionDrill(codes.dropLast(1))

    /** 지금 보고 있는 시·도나 시·군·구. 맨 위면 null이다. */
    fun current(catalog: KoreaRegionCatalog): KoreaArea? = codes.lastOrNull()?.let(catalog::find)

    /** 저장하기 쉽게 한 줄로 잇는다. */
    fun encode(): String = codes.joinToString(SEPARATOR)

    companion object {
        private const val SEPARATOR = "/"

        /** [area]의 아래 목록을 보는 상태. 검색 결과에서 바로 내려가도 뒤로 가기가 위 단계를 거치도록 위 코드를 함께 담는다. */
        fun at(area: KoreaArea): RegionDrill = when (area.level) {
            KoreaAreaLevel.Province -> RegionDrill(listOf(area.code))
            else -> RegionDrill(listOf(KoreaRegionCatalog.provinceCodeOf(area.code), area.code))
        }

        fun decode(saved: String): RegionDrill = RegionDrill(saved.split(SEPARATOR).filter { it.isNotEmpty() })
    }
}

/** 검색 결과의 수와, 너무 많아 앞부분만 보여 주는지. 검색하지 않을 때는 null이다. */
internal data class RegionSearchSummary(val count: Int, val isTruncated: Boolean)

/**
 * 보여 줄 줄을 고른다. 검색어가 있으면 전국 검색 결과, 없으면 [drill]이 가리키는 단계의 목록이다.
 * 맨 위에서는 최근에 고른 지역을 시·도 목록 앞에 둔다.
 */
internal fun regionRows(
    catalog: KoreaRegionCatalog,
    drill: RegionDrill,
    query: String,
    recentCodes: List<String>,
): List<RegionRow> {
    if (query.isNotBlank()) {
        return catalog.search(query).map { RegionRow.Place(it, opens = opensChildren(catalog, it), showsPath = true) }
    }
    val current = drill.current(catalog) ?: return topRows(catalog, recentCodes)
    val children = catalog.children(current.code).map { child ->
        RegionRow.Place(child, opens = opensChildren(catalog, child), showsPath = false)
    }
    return listOf(RegionRow.Whole(current)) + children
}

/** 검색 중일 때 결과 수. 결과가 검색 한도만큼 차면 더 있을 수 있어 앞부분만 보여 준다고 알린다. */
internal fun regionSearchSummary(query: String, rows: List<RegionRow>): RegionSearchSummary? =
    if (query.isBlank()) null else RegionSearchSummary(rows.size, rows.size >= KoreaRegionCatalog.SEARCH_LIMIT)

private fun topRows(catalog: KoreaRegionCatalog, recentCodes: List<String>): List<RegionRow> {
    val recents = recentCodes.mapNotNull(catalog::find)
    val recentRows = if (recents.isEmpty()) {
        emptyList()
    } else {
        listOf(RegionRow.Header(R.string.weather_region_recent)) +
            recents.map { RegionRow.Place(it, opens = false, showsPath = true, group = RECENT_GROUP) }
    }
    return recentRows + RegionRow.Header(R.string.weather_region_provinces) +
        catalog.provinces.map { RegionRow.Place(it, opens = true, showsPath = false) }
}

/** 아래 단계가 있는 시·도, 시·군·구는 누르면 내려간다. 읍·면·동은 바로 고른다. */
private fun opensChildren(catalog: KoreaRegionCatalog, area: KoreaArea): Boolean =
    catalog.children(area.code).isNotEmpty()

private const val RECENT_GROUP = "recent"
