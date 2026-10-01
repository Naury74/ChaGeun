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
 * The AI question screen, pushed on top of a tab.
 * [focusItem] is a maintenance item name when opened from that item's detail.
 */
@Serializable
data class AiRoute(val focusItem: String? = null) : NavKey

@Serializable
data object SettingsRoute : NavKey
