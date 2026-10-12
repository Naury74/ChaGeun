package com.naury.chageun.core.ui

import android.Manifest
import androidx.activity.ComponentDialog
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.activity.result.contract.ActivityResultContract
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.core.app.ActivityOptionsCompat
import com.google.common.truth.Truth.assertThat
import com.naury.chageun.core.domain.weather.KoreaRegionCatalog
import com.naury.chageun.core.domain.weather.KoreaRegionRow
import com.naury.chageun.core.model.GeoPoint
import com.naury.chageun.core.model.WeatherLocation
import com.naury.chageun.core.testing.TestKoreaRegions
import com.naury.chageun.core.uitesting.AppFrame
import com.naury.chageun.core.uitesting.ScreenshotDevices
import com.naury.chageun.core.uitesting.assertAccessibleControls
import com.naury.chageun.core.uitesting.assertNoClippedText
import com.naury.chageun.core.uitesting.captureScreen
import java.io.File
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowDialog

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

    private fun show(
        granted: Boolean = false,
        startWithRegions: Boolean = false,
        recents: List<String> = emptyList(),
        catalog: KoreaRegionCatalog = TestKoreaRegions.catalog,
    ) = composeRule.setContent {
        CompositionLocalProvider(LocalActivityResultRegistryOwner provides registryAnswering(granted)) {
            AppFrame {
                WeatherLocationPicker(
                    onPicked = { picked += it },
                    onDismiss = {},
                    loadRegions = { catalog },
                    recentRegionCodes = recents,
                    startWithRegions = startWithRegions,
                )
            }
        }
    }

    private fun inDialog(matcher: SemanticsMatcher) = composeRule.onNode(matcher and hasAnyAncestor(isDialog()))

    private fun clickInDialog(text: String) =
        inDialog(hasText(text) and hasClickAction()).performScrollTo().performClick()

    /** 지역 목록은 lazy라 화면 밖 줄은 목록을 굴려 꺼낸 뒤 누른다. */
    private fun clickRow(text: String) {
        val row = hasText(text) and hasClickAction()
        inDialog(hasScrollToNodeAction()).performScrollToNode(row)
        inDialog(row).performClick()
    }

    private fun assertTitle(text: String) = inDialog(hasText(text) and isHeading()).assertExists()

    private fun search(text: String) = inDialog(hasSetTextAction()).performTextInput(text)

    @Test
    fun deniedPermission_fallsBackToRegionPicker() {
        show(granted = false)

        clickInDialog("Current location (approximate)")
        inDialog(hasText("No location permission", substring = true)).assertExists()
        clickRow("부산")
        clickRow("All of 부산")

        assertThat(permissionRequests).isEqualTo(1)
        assertThat(picked).containsExactly(WeatherLocation.Region(TestKoreaRegions.busan))
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
    fun drillDown_goesProvinceDistrictTown_andBackGoesUpOneLevel() {
        show()
        composeRule.assertAccessibleControls()

        clickInDialog("Choose a region")
        assertTitle("Choose a region")
        clickRow("서울")
        assertTitle("서울")
        clickRow("강남구")
        assertTitle("서울 강남구")
        composeRule.assertAccessibleControls()

        inDialog(hasContentDescription("Back")).performClick()
        assertTitle("서울")
        clickRow("강남구")
        clickRow("역삼1동")

        assertThat(picked).containsExactly(WeatherLocation.Region(TestKoreaRegions.yeoksam1))
    }

    @Test
    fun wholeDistrictOption_picksTheDistrict() {
        show(startWithRegions = true)

        clickRow("경기")
        clickRow("수원시 장안구")
        clickRow("All of 수원시 장안구")

        assertThat(picked).containsExactly(WeatherLocation.Region(TestKoreaRegions.area("4111100000")))
    }

    @Test
    fun sejong_listsTownsRightUnderTheProvince() {
        show(startWithRegions = true)

        clickRow("세종")
        clickRow("조치원읍")

        assertThat(picked).containsExactly(WeatherLocation.Region(TestKoreaRegions.area("3611025000")))
    }

    @Test
    fun search_showsFullPaths_andPicksATown() {
        show(startWithRegions = true)

        search("역삼")

        inDialog(hasText("2 places found")).assertExists()
        inDialog(hasText("서울 강남구 역삼2동")).assertExists()
        clickRow("서울 강남구 역삼1동")
        assertThat(picked).containsExactly(WeatherLocation.Region(TestKoreaRegions.yeoksam1))
    }

    @Test
    fun search_districtResultOpensItsTowns() {
        show(startWithRegions = true)

        search("강남")
        clickRow("서울 강남구")

        assertTitle("서울 강남구")
        inDialog(hasText("역삼1동") and hasClickAction()).assertExists()
        assertThat(picked).isEmpty()
    }

    @Test
    fun search_withoutMatches_explainsHowToSearch() {
        show(startWithRegions = true)

        search("없는동네")

        inDialog(hasText("No matching places", substring = true)).assertExists()
    }

    @Test
    fun systemBack_clearsSearchThenGoesUp_beforeClosing() {
        var dismissed = 0
        composeRule.setContent {
            AppFrame {
                WeatherLocationPicker(
                    onPicked = { picked += it },
                    onDismiss = { dismissed++ },
                    loadRegions = { TestKoreaRegions.catalog },
                    startWithRegions = true,
                )
            }
        }
        clickRow("서울")
        search("역삼")

        pressBack()
        assertTitle("서울")
        inDialog(hasText("역삼1동") and hasClickAction()).assertDoesNotExist()
        pressBack()
        assertTitle("Choose a region")
        assertThat(dismissed).isEqualTo(0)
        pressBack()
        assertThat(dismissed).isEqualTo(1)
    }

    private fun pressBack() {
        composeRule.runOnUiThread {
            (ShadowDialog.getLatestDialog() as ComponentDialog).onBackPressedDispatcher.onBackPressed()
        }
        composeRule.waitForIdle()
    }

    @Test
    fun recents_comeFirst_andPickRightAway() {
        show(startWithRegions = true, recents = listOf("1168064000", "2600000000"))

        inDialog(hasText("Recently chosen") and isHeading()).assertExists()
        clickRow("서울 강남구 역삼1동")

        assertThat(picked).containsExactly(WeatherLocation.Region(TestKoreaRegions.yeoksam1))
    }

    @Test
    @Config(qualifiers = ScreenshotDevices.PHONE_KO, fontScale = 2f)
    fun largeFont_keepsRowsReadable() {
        show(startWithRegions = true, catalog = RealKoreaRegions.catalog)
        clickRow("서울")
        clickRow("강남구")

        composeRule.assertNoClippedText()
        composeRule.captureScreen("weather_location_regions_town_large_font_ko")
    }

    @Test
    @Config(qualifiers = ScreenshotDevices.PHONE_KO)
    fun sourceDialog_ko() {
        show()
        composeRule.captureScreen("weather_location_source_ko")
    }

    @Test
    @Config(qualifiers = ScreenshotDevices.PHONE_KO)
    fun provinces_ko() = captureProvinces("weather_location_regions_ko")

    @Test
    @Config(qualifiers = PHONE_DARK_KO)
    fun provinces_dark_ko() = captureProvinces("weather_location_regions_dark_ko")

    @Test
    @Config(qualifiers = ScreenshotDevices.PHONE_KO)
    fun districts_ko() = captureDistricts("weather_location_regions_district_ko")

    @Test
    @Config(qualifiers = PHONE_DARK_KO)
    fun districts_dark_ko() = captureDistricts("weather_location_regions_district_dark_ko")

    @Test
    @Config(qualifiers = ScreenshotDevices.PHONE_KO)
    fun towns_ko() = captureTowns("weather_location_regions_town_ko")

    @Test
    @Config(qualifiers = PHONE_DARK_KO)
    fun towns_dark_ko() = captureTowns("weather_location_regions_town_dark_ko")

    @Test
    @Config(qualifiers = ScreenshotDevices.PHONE_KO)
    fun searchResults_ko() = captureSearch("weather_location_regions_search_ko")

    @Test
    @Config(qualifiers = PHONE_DARK_KO)
    fun searchResults_dark_ko() = captureSearch("weather_location_regions_search_dark_ko")

    @Test
    @Config(qualifiers = ScreenshotDevices.MEDIUM_KO)
    fun towns_wideCard_ko() = captureTowns("weather_location_regions_town_wide_ko")

    private fun captureProvinces(name: String) {
        show(startWithRegions = true, recents = listOf("1168064000", "4111100000"), catalog = RealKoreaRegions.catalog)
        composeRule.captureScreen(name)
    }

    private fun captureDistricts(name: String) {
        show(startWithRegions = true, catalog = RealKoreaRegions.catalog)
        clickRow("서울")
        composeRule.captureScreen(name)
    }

    private fun captureTowns(name: String) {
        show(startWithRegions = true, catalog = RealKoreaRegions.catalog)
        clickRow("서울")
        clickRow("강남구")
        composeRule.captureScreen(name)
    }

    private fun captureSearch(name: String) {
        show(startWithRegions = true, catalog = RealKoreaRegions.catalog)
        search("중앙동")
        composeRule.captureScreen(name)
    }

    private companion object {
        const val PHONE_DARK_KO = "ko-w360dp-h800dp-night-hdpi"
    }
}

/** 화면 사진은 앱에 넣은 실제 지역 목록으로 찍는다. 테스트는 모듈 폴더에서 돌아 상대 경로로 읽는다. */
private object RealKoreaRegions {
    val catalog: KoreaRegionCatalog by lazy {
        val rows = File("../../data/weather/src/main/assets/weather/korea_regions.csv").useLines { lines ->
            lines.filterNot { it.startsWith("#") || it.isBlank() }.map { line ->
                val fields = line.split(',')
                KoreaRegionRow(
                    code = fields[0],
                    name = fields[1],
                    point = GeoPoint(fields[2].toDouble(), fields[3].toDouble()),
                    shortName = fields.getOrNull(4),
                )
            }.toList()
        }
        KoreaRegionCatalog(rows)
    }
}
