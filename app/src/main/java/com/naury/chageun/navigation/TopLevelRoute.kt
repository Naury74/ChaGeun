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
