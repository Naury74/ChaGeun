package com.naury.chageun.core.domain.weather

import com.naury.chageun.core.model.GeoPoint
import com.naury.chageun.core.model.KoreaArea
import com.naury.chageun.core.model.KoreaAreaLevel
import kotlinx.coroutines.flow.Flow

/** 지역 목록 파일의 한 줄. [name]은 그 단계의 원래 이름(수원시장안구)이고, [shortName]은 시·도 줄에만 있다. */
data class KoreaRegionRow(val code: String, val name: String, val point: GeoPoint, val shortName: String? = null)

/** 전국 시·도, 시·군·구, 읍·면·동 목록. 앱에 넣은 목록이나, 그보다 새로 내려받은 목록을 쓴다. */
interface KoreaRegionRepository {
    /** 처음 부를 때 백그라운드에서 읽고, 그 뒤로는 메모리에 둔 목록을 돌려준다. */
    suspend fun catalog(): KoreaRegionCatalog

    /** 지금 쓰는 목록의 판과 출처. 새 목록을 받으면 새 값을 낸다. 목록 자체는 읽지 않는다. */
    val listInfo: Flow<KoreaRegionListInfo>
}

/**
 * 지역 목록. 코드로 찾기, 단계별로 내려가기, 이름으로 찾기를 한다.
 *
 * 세종처럼 시·도 아래 같은 이름의 시·군·구 하나만 있는 곳은 그 시·군·구를 건너뛰고 시·도 바로 아래에 읍·면·동을 둔다.
 * 같은 이름을 두 번 고르게 하지 않기 위해서다.
 */
class KoreaRegionCatalog(rows: List<KoreaRegionRow>) {

    private val entries: List<KoreaAreaEntry> = KoreaAreaFactory.build(rows)
    private val byCode: Map<String, KoreaArea> = entries.associate { it.area.code to it.area }
    private val visibleChildren: Map<String, List<KoreaArea>>
    private val search = KoreaRegionSearch(entries.filterNot { it.isPassThrough })

    init {
        val direct = entries.groupBy({ parentCode(it.area.code) }, { it })
        fun visible(code: String): List<KoreaArea> = direct[code].orEmpty().flatMap { child ->
            if (child.isPassThrough) visible(child.area.code) else listOf(child.area)
        }
        visibleChildren = entries.filterNot { it.area.level == KoreaAreaLevel.Town }
            .associate { it.area.code to visible(it.area.code) }
    }

    /** 시·도 목록. 원본 순서(서울, 부산, …)를 따른다. */
    val provinces: List<KoreaArea> = entries.map { it.area }.filter { it.level == KoreaAreaLevel.Province }

    /** 모든 지역 수. 시·도, 시·군·구, 읍·면·동을 모두 센다. */
    val size: Int get() = byCode.size

    fun find(code: String): KoreaArea? = byCode[code]

    /** [code]가 없어졌으면(행정동 통폐합) 그 위 시·군·구, 시·도로 거슬러 올라가 찾는다. */
    fun resolve(code: String): KoreaArea? = find(code) ?: parentCode(code)?.let(::resolve)

    /** 한 단계 아래 지역. 읍·면·동이면 빈 목록이다. */
    fun children(code: String): List<KoreaArea> = visibleChildren[code].orEmpty()

    /** 이름 일부로 찾는다. 띄어 쓴 낱말은 모두 들어 있어야 하고, 초성(ㄱㄴㄱ)만 써도 된다. */
    fun search(query: String, limit: Int = SEARCH_LIMIT): List<KoreaArea> = search.find(query, limit)

    companion object {
        const val SEARCH_LIMIT = 50

        /** 코드로 단계를 안다. 시·도는 뒤 8자리, 시·군·구는 뒤 5자리가 0이다. */
        fun levelOf(code: String): KoreaAreaLevel = when {
            code.endsWith(PROVINCE_TAIL) -> KoreaAreaLevel.Province
            code.endsWith(DISTRICT_TAIL) -> KoreaAreaLevel.District
            else -> KoreaAreaLevel.Town
        }

        /** 한 단계 위 지역의 코드. 시·도면 null이다. */
        fun parentCode(code: String): String? = when (levelOf(code)) {
            KoreaAreaLevel.Province -> null
            KoreaAreaLevel.District -> provinceCodeOf(code)
            KoreaAreaLevel.Town -> districtCodeOf(code)
        }

        /** [code]가 속한 시·도의 코드. */
        fun provinceCodeOf(code: String): String = code.take(PROVINCE_DIGITS) + PROVINCE_TAIL

        /** [code]가 속한 시·군·구의 코드. 시·도 코드를 넣으면 그대로 나온다. */
        fun districtCodeOf(code: String): String = code.take(DISTRICT_DIGITS) + DISTRICT_TAIL

        private const val PROVINCE_DIGITS = 2
        private const val DISTRICT_DIGITS = 5
        private const val PROVINCE_TAIL = "00000000"
        private const val DISTRICT_TAIL = "00000"
    }
}
