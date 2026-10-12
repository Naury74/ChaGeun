package com.naury.chageun.data.weather

import com.google.common.truth.Truth.assertThat
import java.util.Locale
import org.junit.Test

class AreaLabelsTest {

    private val korean = Locale.KOREAN
    private val english = Locale.ENGLISH

    @Test
    fun korean_metropolitanCity_usesGu() {
        val samples = mapOf(
            AreaAddress("KR", adminArea = "서울특별시", subLocality = "강남구") to "서울 강남구",
            AreaAddress("KR", adminArea = "서울특별시", locality = "서울특별시", subLocality = "강남구") to "서울 강남구",
            AreaAddress("KR", adminArea = "부산광역시", subLocality = "해운대구") to "부산 해운대구",
            AreaAddress("KR", adminArea = "부산광역시", locality = "기장군") to "부산 기장군",
            AreaAddress("KR", adminArea = "대구광역시", subAdminArea = "수성구") to "대구 수성구",
            AreaAddress("KR", adminArea = "인천광역시", locality = "연수구", subLocality = "송도동") to "인천 연수구",
        )

        samples.forEach { (address, label) -> assertThat(AreaLabels.label(address, korean)).isEqualTo(label) }
    }

    @Test
    fun korean_province_prefersCityOverGu() {
        val samples = mapOf(
            AreaAddress("KR", adminArea = "경기도", locality = "성남시", subLocality = "분당구") to "경기 성남시",
            AreaAddress("KR", adminArea = "경기도", subAdminArea = "수원시", subLocality = "영통구") to "경기 수원시",
            AreaAddress("KR", adminArea = "경기도", locality = "광주시") to "경기 광주시",
            AreaAddress("KR", adminArea = "경상남도", subLocality = "의창구", locality = "창원시") to "경남 창원시",
            AreaAddress("KR", adminArea = "강원특별자치도", locality = "춘천시") to "강원 춘천시",
            AreaAddress("KR", adminArea = "전북특별자치도", locality = "전주시", subLocality = "완산구") to "전북 전주시",
            AreaAddress("KR", adminArea = "충청북도", subAdminArea = "괴산군") to "충북 괴산군",
            AreaAddress("KR", adminArea = "제주특별자치도", locality = "서귀포시") to "제주 서귀포시",
        )

        samples.forEach { (address, label) -> assertThat(AreaLabels.label(address, korean)).isEqualTo(label) }
    }

    @Test
    fun korean_dropsDongLevel_andKeepsOnlyProvinceWhenNothingElseIsReliable() {
        val samples = mapOf(
            AreaAddress("KR", adminArea = "서울특별시", subLocality = "역삼동") to "서울",
            AreaAddress("KR", adminArea = "세종특별자치시", subLocality = "보람동") to "세종",
            AreaAddress("KR", adminArea = "경기도", locality = "분당동") to "경기",
            AreaAddress("KR", locality = "서울특별시", subLocality = "강남구") to "서울 강남구",
            AreaAddress("KR", subLocality = "해운대구") to "해운대구",
        )

        samples.forEach { (address, label) -> assertThat(AreaLabels.label(address, korean)).isEqualTo(label) }
    }

    @Test
    fun english_putsDistrictFirst_andShortensProvince() {
        val samples = mapOf(
            AreaAddress("KR", adminArea = "Seoul", subLocality = "Gangnam-gu") to "Gangnam-gu, Seoul",
            AreaAddress("KR", adminArea = "Seoul Special City", subLocality = "Gangnam District") to
                "Gangnam District, Seoul",
            AreaAddress("KR", adminArea = "Gyeonggi-do", locality = "Seongnam-si", subLocality = "Bundang-gu") to
                "Seongnam-si, Gyeonggi",
            AreaAddress("KR", adminArea = "Gyeonggi-do", locality = "Gwangju-si") to "Gwangju-si, Gyeonggi",
            AreaAddress("KR", adminArea = "Busan", subLocality = "Haeundae-gu") to "Haeundae-gu, Busan",
            AreaAddress("KR", adminArea = "Busan Metropolitan City", locality = "Busan") to "Busan",
            AreaAddress("KR", adminArea = "Gyeongsangnam-do", locality = "Changwon-si") to
                "Changwon-si, South Gyeongsang",
            AreaAddress("KR", adminArea = "Jeju-do", locality = "Seogwipo-si") to "Seogwipo-si, Jeju",
            AreaAddress("KR", adminArea = "Seoul", subLocality = "Yeoksam-dong") to "Seoul",
        )

        samples.forEach { (address, label) -> assertThat(AreaLabels.label(address, english)).isEqualTo(label) }
    }

    @Test
    fun abroad_usesCityAndRegion() {
        val tokyo = AreaAddress("JP", adminArea = "Tokyo", locality = "Shibuya")
        val sydney = AreaAddress("AU", adminArea = "New South Wales", locality = "Sydney")

        assertThat(AreaLabels.label(tokyo, english)).isEqualTo("Shibuya, Tokyo")
        assertThat(AreaLabels.label(sydney, korean)).isEqualTo("New South Wales Sydney")
        assertThat(AreaLabels.label(AreaAddress("JP", adminArea = "Tokyo", locality = "Tokyo"), english))
            .isEqualTo("Tokyo")
    }

    @Test
    fun emptyAddress_isNull() {
        assertThat(AreaLabels.label(AreaAddress("KR"), korean)).isNull()
        assertThat(AreaLabels.label(AreaAddress("KR", adminArea = " ", subLocality = ""), english)).isNull()
    }
}
