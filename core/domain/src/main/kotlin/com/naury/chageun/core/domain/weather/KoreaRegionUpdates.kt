package com.naury.chageun.core.domain.weather

/**
 * 지금 쓰는 지역 목록의 판. [version]은 "2026-07"처럼 연-월이다.
 * [source]는 내려받은 목록의 출처 문구이고, 앱에 넣은 목록이면 null이라 화면이 리소스 문구를 쓴다.
 */
data class KoreaRegionListInfo(val version: String, val source: String? = null) {
    val isBundled: Boolean get() = source == null
}

/** 지역 목록을 새로 받아 봤는지. */
sealed interface KoreaRegionCheck {
    /** 가진 목록이 가장 새것이다. */
    data object UpToDate : KoreaRegionCheck

    /** 새 목록을 받아 바로 쓰기 시작했다. */
    data class Updated(val version: String) : KoreaRegionCheck

    /** 받지 못했다. [isTemporary]면 네트워크 문제라 나중에 다시 해 볼 만하다. 가진 목록은 그대로 쓴다. */
    data class Failed(val isTemporary: Boolean) : KoreaRegionCheck
}

/**
 * 앱을 새로 내지 않고 지역 목록(시·도, 시·군·구, 읍·면·동)을 웹에서 새로 받는다.
 * 요청에는 고른 지역이나 기기를 알아볼 수 있는 값을 담지 않는다.
 */
interface KoreaRegionUpdates {
    /** 지금 바로 확인한다. 설정 › 날씨의 "지역 목록 업데이트 확인"과 주기 작업이 부른다. */
    suspend fun checkNow(): KoreaRegionCheck

    /** 마지막으로 확인한 지 일주일이 지났으면 확인한다. 지역 고르기 창을 열 때 부른다. */
    suspend fun checkIfStale()
}

/**
 * 새 목록에서 바뀐 행정구역코드(예전 코드 → 새 코드)를 따라간다. A→B, B→C처럼 이어지면 C까지 간다.
 * 표가 잘못되어 고리가 생겨도 멈추도록 표 크기만큼만 따라간다.
 */
object RenamedRegionCodes {
    fun follow(code: String, renamed: Map<String, String>): String {
        var current = code
        repeat(renamed.size) {
            current = renamed[current] ?: return current
        }
        return current
    }
}
