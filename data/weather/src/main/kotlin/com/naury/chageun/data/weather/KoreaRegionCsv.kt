package com.naury.chageun.data.weather

import com.naury.chageun.core.domain.weather.KoreaRegionRow
import com.naury.chageun.core.model.GeoPoint

/**
 * 앱에 넣은 지역 목록(assets/weather/korea_regions.csv)을 읽는다. `#`로 시작하는 줄은 출처 설명이다.
 * 한 줄은 `코드,이름,위도,경도[,시·도 짧은 이름]`이다. 만드는 방법은 scripts/build-korea-regions.py에 있다.
 */
internal object KoreaRegionCsv {

    const val ASSET_PATH = "weather/korea_regions.csv"

    /** 앱에 넣은 목록의 판. 원본(최종 업데이트 파일_20260701)의 연-월이다. 목록을 바꾸면 함께 고친다. */
    const val BUNDLED_VERSION = "2026-07"

    fun parse(lines: Sequence<String>): List<KoreaRegionRow> = lines
        .map(String::trim)
        .filter { it.isNotEmpty() && !it.startsWith(COMMENT) }
        .map(::row)
        .toList()

    private fun row(line: String): KoreaRegionRow {
        val fields = line.split(SEPARATOR)
        require(fields.size == FIELDS || fields.size == FIELDS + 1) { "지역 목록 줄의 칸 수가 맞지 않음: ${fields.size}" }
        return KoreaRegionRow(
            code = fields[0],
            name = fields[1],
            point = GeoPoint(latitude = fields[2].toDouble(), longitude = fields[3].toDouble()),
            shortName = fields.getOrNull(FIELDS),
        )
    }

    private const val COMMENT = "#"
    private const val SEPARATOR = ','
    private const val FIELDS = 4
}
