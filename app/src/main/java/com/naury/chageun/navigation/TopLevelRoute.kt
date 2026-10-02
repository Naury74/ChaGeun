package com.naury.chageun.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
sealed interface TopLevelRoute : NavKey {
    @Serializable
    data object Home : TopLevelRoute

    @Serializable
    data object Manage : TopLevelRoute

    @Serializable
    data object History : TopLevelRoute

    @Serializable
    data object Vehicle : TopLevelRoute
}

/**
 * 탭 위에 쌓이는 AI 질문 화면.
 * 정비 항목 상세에서 열면 [focusItem]에 해당 항목 이름이 들어온다.
 */
@Serializable
data class AiRoute(val focusItem: String? = null) : NavKey

@Serializable
data object SettingsRoute : NavKey

@Serializable
data object OpenSourceLicensesRoute : NavKey

@Serializable
data object PrivacyNoticeRoute : NavKey
