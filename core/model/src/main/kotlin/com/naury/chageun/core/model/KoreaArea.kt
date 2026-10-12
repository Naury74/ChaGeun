package com.naury.chageun.core.model

/** 행정구역 단계. 시·도 아래 시·군·구, 그 아래 읍·면·동(행정동)이 있다. */
enum class KoreaAreaLevel {
    Province,
    District,
    Town,
}

/**
 * 날씨 위치로 고를 수 있는 행정구역 하나. 앱에 넣어 둔 기상청 지역 목록에서 온다.
 *
 * @param code 행정구역코드(10자리). 설정에는 이 값만 저장한다.
 * @param provinceName 시·도의 짧은 이름(서울, 경기).
 * @param districtName 시·군·구 이름. 구가 있는 시는 "수원시 장안구"처럼 띄어 쓴다. 세종처럼 시·군·구가 따로 없으면 null이다.
 * @param townName 읍·면·동 이름. 시·도나 시·군·구 단위로 골랐으면 null이다.
 * @param point 기상청이 정한 그 지역의 대표 지점.
 * @param label 홈 날씨 줄처럼 좁은 곳에 쓰는 이름. 헷갈리지 않는 가장 짧은 이름이다(강남구 역삼1동, 서울 강남구).
 */
data class KoreaArea(
    val code: String,
    val level: KoreaAreaLevel,
    val provinceName: String,
    val districtName: String?,
    val townName: String?,
    val point: GeoPoint,
    val label: String,
) {
    /** 시·도부터 이어 쓴 전체 이름(서울 강남구 역삼1동). 설정과 검색 결과에 쓴다. */
    val path: String get() = listOfNotNull(provinceName, districtName, townName).joinToString(" ")

    /** 목록 한 줄에 쓰는 자기 단계의 이름(역삼1동, 강남구, 서울). */
    val name: String get() = townName ?: districtName ?: provinceName
}
