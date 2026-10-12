package com.naury.chageun.core.ui

import android.Manifest
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.activity.result.contract.ActivityResultContract
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.core.app.ActivityOptionsCompat
import com.google.common.truth.Truth.assertThat
import com.naury.chageun.core.model.KoreaRegion
import com.naury.chageun.core.model.WeatherLocation
import com.naury.chageun.core.uitesting.AppFrame
import com.naury.chageun.core.uitesting.ScreenshotDevices
import com.naury.chageun.core.uitesting.assertAccessibleControls
import com.naury.chageun.core.uitesting.captureScreen
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = ScreenshotDevices.PHONE)
class WeatherLocationPickerTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val picked = mutableListOf<WeatherLocation>()
    private var permissionRequests = 0

    /** 권한 창 대신 바로 [granted]로 답하는 결과 처리기. */
    private fun registryAnswering(granted: Boolean) = object : ActivityResultRegistryOwner {
        override val activityResultRegistry = object : ActivityResultRegistry() {
            override fun <I, O> onLaunch(
                requestCode: Int,
                contract: ActivityResultContract<I, O>,
                input: I,
                options: ActivityOptionsCompat?,
            ) {
                permissionRequests++
                dispatchResult(requestCode, granted)
            }
        }
    }

    private fun show(granted: Boolean = false, startWithRegions: Boolean = false) = composeRule.setContent {
        CompositionLocalProvider(LocalActivityResultRegistryOwner provides registryAnswering(granted)) {
            AppFrame {
                WeatherLocationPicker(onPicked = { picked += it }, onDismiss = {}, startWithRegions = startWithRegions)
            }
        }
    }

    private fun clickInDialog(text: String) =
        composeRule.onNode(hasText(text) and hasAnyAncestor(isDialog())).performScrollTo().performClick()

    @Test
    fun deniedPermission_fallsBackToRegionList() {
        show(granted = false)

        clickInDialog("Current location (approximate)")
        composeRule.onNode(hasText("No location permission", substring = true) and hasAnyAncestor(isDialog()))
            .assertExists()
        clickInDialog("Busan")

        assertThat(permissionRequests).isEqualTo(1)
        assertThat(picked).containsExactly(WeatherLocation.Region(KoreaRegion.Busan))
    }

    @Test
    fun grantedPermission_picksCurrentLocation() {
        show(granted = true)

        clickInDialog("Current location (approximate)")

        assertThat(picked).containsExactly(WeatherLocation.Current)
    }

    @Test
    fun alreadyGranted_doesNotAskAgain() {
        shadowOf(RuntimeEnvironment.getApplication())
            .grantPermissions(Manifest.permission.ACCESS_COARSE_LOCATION)
        show(granted = false)

        clickInDialog("Current location (approximate)")

        assertThat(permissionRequests).isEqualTo(0)
        assertThat(picked).containsExactly(WeatherLocation.Current)
    }

    @Test
    fun manualRegion_listsAllSeventeen_andPicksLastOne() {
        show()
        composeRule.assertAccessibleControls()

        clickInDialog("Choose a region")
        clickInDialog("Jeju")

        assertThat(KoreaRegion.entries).hasSize(17)
        assertThat(picked).containsExactly(WeatherLocation.Region(KoreaRegion.Jeju))
    }

    @Test
    @Config(qualifiers = ScreenshotDevices.PHONE_KO)
    fun sourceDialog_ko() {
        show()
        composeRule.captureScreen("weather_location_source_ko")
    }

    @Test
    @Config(qualifiers = ScreenshotDevices.PHONE_KO)
    fun regionDialog_ko() {
        show(startWithRegions = true)
        composeRule.captureScreen("weather_location_regions_ko")
    }
}
