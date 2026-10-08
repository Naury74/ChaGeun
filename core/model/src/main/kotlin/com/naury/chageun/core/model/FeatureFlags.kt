package com.naury.chageun.core.model

/**
 * 기능을 원격으로 끄기 위한 스위치(기획 §36.3). 서버 오류로 잘못 켜지지 않도록 아직 없는 기능은 기본으로 꺼 둔다.
 * 끄면 화면만 숨기지 않고 관련 작업과 통신도 멈춰야 한다.
 */
data class FeatureFlags(
    /** 차량번호로 차량 정보를 불러오는 기능. 자동차365 연계 전이라 꺼 둔다. */
    val vehicleAutoLookupEnabled: Boolean = false,
    /** 리콜 정보를 주기적으로 새로 받는 기능. V1은 자동차리콜센터로 연결만 한다. */
    val recallRefreshEnabled: Boolean = false,
    val nativeAdsEnabled: Boolean = true,
    val aiShareEnabled: Boolean = true,
    val mcpConnectionEnabled: Boolean = false,
    val maintenancePredictionEnabled: Boolean = true,
)
