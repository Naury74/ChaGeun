package com.naury.chageun.data.weather

import java.util.Locale

/** 지오코더 주소에서 동네 이름에 쓰는 칸만 옮긴 것. 도로명·번지·동(thoroughfare, featureName)은 담지 않는다. */
internal data class AreaAddress(
    val countryCode: String? = null,
    val adminArea: String? = null,
    val subAdminArea: String? = null,
    val locality: String? = null,
    val subLocality: String? = null,
)

/**
 * 주소를 "서울 강남구", "경기 성남시", "Gangnam-gu, Seoul"처럼 시·도와 시·군·구까지만 줄여 쓴다.
 * 위치가 약 10km 단위라 동·번지까지 쓰면 틀린 동네를 가리킬 수 있어서다.
 *
 * 한국 주소는 기기마다 칸이 다르게 온다. 성남시는 locality에, 강남구는 subLocality에 오는 일이 많고
 * subAdminArea에 시·군·구가 오기도 한다. 그래서 세 칸을 모두 보고 시·군을 구보다 먼저 고른다
 * (경기 성남시 분당구 → "경기 성남시", 서울특별시 강남구 → "서울 강남구").
 */
internal object AreaLabels {

    fun label(address: AreaAddress, locale: Locale): String? {
        val isKorea = address.countryCode == null || address.countryCode.equals(KOREA, ignoreCase = true)
        val (province, district) = if (isKorea) koreanParts(address) else otherParts(address)
        val parts = if (locale.language ==
            KOREAN
        ) {
            listOfNotNull(province, district)
        } else {
            listOfNotNull(district, province)
        }
        return parts.takeIf { it.isNotEmpty() }?.joinToString(if (locale.language == KOREAN) " " else ", ")
    }

    private fun koreanParts(address: AreaAddress): Pair<String?, String?> {
        val names = listOf(address.locality, address.subAdminArea, address.subLocality).mapNotNull(::clean)
        val provinceName = clean(address.adminArea) ?: names.firstOrNull { KoreanProvinces.short(it) != null }
        val districts = names.filter { it != provinceName && KoreanProvinces.short(it) == null && isDistrict(it) }
        val district = districts.firstOrNull(::isCityOrCounty) ?: districts.firstOrNull()
        return provinceName?.let { KoreanProvinces.short(it) ?: it } to district
    }

    /** 해외에서는 나라마다 칸의 뜻이 달라 도시(locality)와 그 위 행정 구역만 쓴다. */
    private fun otherParts(address: AreaAddress): Pair<String?, String?> {
        val province = clean(address.adminArea)
        val district = (clean(address.locality) ?: clean(address.subAdminArea))?.takeIf { it != province }
        return province to district
    }

    private fun clean(value: String?): String? = value?.trim()?.takeIf { it.isNotEmpty() }

    private fun isCityOrCounty(name: String): Boolean = CITY_SUFFIXES.any { name.endsWith(it) }

    /** 시·군·구만 남긴다. 역삼동·Yeoksam-dong 같은 동·읍·면 단위는 버린다. */
    private fun isDistrict(name: String): Boolean = isCityOrCounty(name) || WARD_SUFFIXES.any { name.endsWith(it) }

    private const val KOREA = "KR"
    private const val KOREAN = "ko"
    private val CITY_SUFFIXES = listOf("시", "군", "-si", "-gun", " City", " County")
    private val WARD_SUFFIXES = listOf("구", "-gu", " District")
}

/** 시·도의 정식 이름을 지역 목록과 같은 짧은 이름으로 바꾼다(서울특별시 → 서울, Gyeonggi-do → Gyeonggi). */
internal object KoreanProvinces {

    /** 시·도가 아니면 null이다. 이미 짧은 이름이면 그대로 돌려준다. */
    fun short(name: String): String? = KOREAN_NAMES[name] ?: englishShort(name)

    private fun englishShort(name: String): String? {
        var key = name.trim().lowercase(Locale.ROOT)
        ENGLISH_SUFFIXES.firstOrNull { key.endsWith(it) }?.let { key = key.removeSuffix(it) }
        return ENGLISH_NAMES[key.trim()]
    }

    private val KOREAN_NAMES: Map<String, String> = buildMap {
        listOf(
            "서울" to listOf("서울특별시", "서울시"),
            "부산" to listOf("부산광역시"),
            "대구" to listOf("대구광역시"),
            "인천" to listOf("인천광역시"),
            "광주" to listOf("광주광역시"),
            "대전" to listOf("대전광역시"),
            "울산" to listOf("울산광역시"),
            "세종" to listOf("세종특별자치시"),
            "경기" to listOf("경기도"),
            "강원" to listOf("강원도", "강원특별자치도"),
            "충북" to listOf("충청북도"),
            "충남" to listOf("충청남도"),
            "전북" to listOf("전라북도", "전북특별자치도"),
            "전남" to listOf("전라남도"),
            "경북" to listOf("경상북도"),
            "경남" to listOf("경상남도"),
            "제주" to listOf("제주특별자치도", "제주도"),
        ).forEach { (short, fullNames) ->
            put(short, short)
            fullNames.forEach { put(it, short) }
        }
    }

    // 긴 꼬리부터 지운다. "-si"는 지우지 않아 경기 광주시(Gwangju-si)를 광주광역시로 착각하지 않는다.
    private val ENGLISH_SUFFIXES = listOf(
        " special self-governing province",
        " special self-governing city",
        " metropolitan city",
        " special city",
        " province",
        " state",
        "-do",
    )

    private val ENGLISH_NAMES: Map<String, String> = mapOf(
        "seoul" to "Seoul",
        "busan" to "Busan",
        "daegu" to "Daegu",
        "incheon" to "Incheon",
        "gwangju" to "Gwangju",
        "daejeon" to "Daejeon",
        "ulsan" to "Ulsan",
        "sejong" to "Sejong",
        "gyeonggi" to "Gyeonggi",
        "gangwon" to "Gangwon",
        "chungcheongbuk" to "North Chungcheong",
        "north chungcheong" to "North Chungcheong",
        "chungcheongnam" to "South Chungcheong",
        "south chungcheong" to "South Chungcheong",
        "jeollabuk" to "North Jeolla",
        "jeonbuk" to "North Jeolla",
        "north jeolla" to "North Jeolla",
        "jeollanam" to "South Jeolla",
        "south jeolla" to "South Jeolla",
        "gyeongsangbuk" to "North Gyeongsang",
        "north gyeongsang" to "North Gyeongsang",
        "gyeongsangnam" to "South Gyeongsang",
        "south gyeongsang" to "South Gyeongsang",
        "jeju" to "Jeju",
    )
}
