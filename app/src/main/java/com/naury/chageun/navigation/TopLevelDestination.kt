package com.naury.chageun.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Home
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import com.naury.chageun.R

enum class TopLevelDestination(val route: TopLevelRoute, @param:StringRes val labelRes: Int) {
    Home(TopLevelRoute.Home, R.string.nav_home),
    Manage(TopLevelRoute.Manage, R.string.nav_manage),
    History(TopLevelRoute.History, R.string.nav_history),
    Vehicle(TopLevelRoute.Vehicle, R.string.nav_vehicle),
    ;

    val icon: ImageVector
        @Composable get() = when (this) {
            Home -> Icons.Filled.Home
            Manage -> Icons.Filled.Build
            History -> Icons.AutoMirrored.Filled.List
            Vehicle -> ImageVector.vectorResource(R.drawable.ic_vehicle)
        }
}
