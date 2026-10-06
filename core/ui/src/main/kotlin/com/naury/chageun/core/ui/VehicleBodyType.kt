package com.naury.chageun.core.ui

import androidx.annotation.DrawableRes

/** 홈과 내 차의 실루엣을 고르는 차체 모양. 제조사 사진 대신 직접 그린 그림을 쓴다. */
enum class VehicleBodyType(@param:DrawableRes val silhouetteRes: Int) {
    Sedan(R.drawable.vehicle_silhouette_sedan),
    Hatchback(R.drawable.vehicle_silhouette_hatchback),
    Suv(R.drawable.vehicle_silhouette_suv),
    Van(R.drawable.vehicle_silhouette_van),
    Truck(R.drawable.vehicle_silhouette_truck),
}

/**
 * 모델명으로 차체 모양을 고른다. 온보딩 목록의 모델은 한국어·영어 이름을 모두 알고,
 * 직접 입력한 모델처럼 모르는 이름은 가장 흔한 세단으로 보여 준다.
 */
fun vehicleBodyTypeOf(model: String): VehicleBodyType = BODY_TYPES[model.normalized()] ?: VehicleBodyType.Sedan

private fun String.normalized() = lowercase().filterNot { it.isWhitespace() || it == '-' }

private val BODY_TYPES: Map<String, VehicleBodyType> = buildMap {
    fun put(type: VehicleBodyType, vararg names: String) = names.forEach { put(it.normalized(), type) }
    put(
        VehicleBodyType.Sedan,
        "아반떼", "Avante", "쏘나타", "Sonata", "그랜저", "Grandeur", "K5", "K8", "G70", "G80", "G90",
        "3시리즈", "3 Series", "5시리즈", "5 Series", "C클래스", "C-Class", "E클래스", "E-Class", "모델 3", "Model 3",
    )
    put(VehicleBodyType.Hatchback, "캐스퍼", "Casper", "모닝", "Morning", "레이", "Ray", "스파크", "Spark")
    put(
        VehicleBodyType.Suv,
        "코나", "Kona", "투싼", "Tucson", "싼타페", "Santa Fe", "팰리세이드", "Palisade", "아이오닉 5", "Ioniq 5",
        "셀토스", "Seltos", "스포티지", "Sportage", "쏘렌토", "Sorento", "EV6", "EV9", "GV60", "GV70", "GV80",
        "티볼리", "Tivoli", "코란도", "Korando", "토레스", "Torres", "렉스턴", "Rexton", "액티언", "Actyon",
        "아르카나", "Arkana", "QM6", "그랑 콜레오스", "Grand Koleos", "트랙스", "Trax", "트레일블레이저",
        "Trailblazer", "이쿼녹스", "Equinox", "X3", "X5", "GLC", "GLE", "모델 Y", "Model Y",
    )
    put(VehicleBodyType.Van, "카니발", "Carnival", "스타리아", "Staria")
    put(VehicleBodyType.Truck, "포터", "Porter")
}
