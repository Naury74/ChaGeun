package com.naury.chageun.flow

import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.naury.chageun.MainActivity
import com.naury.chageun.R
import com.naury.chageun.core.domain.album.AlbumRepository
import com.naury.chageun.core.domain.vehicle.VehicleRepository
import com.naury.chageun.feature.vehicle.R as VehicleR
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import java.io.File
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 내 차 앨범의 사진 보기. 사진을 열면 "1 / 1"과 조작 버튼이 보이고, 사진을 누르면 조작 막대가 숨었다 돌아오며,
 * 뒤로 가기로 닫으면 내비게이션이 다시 보인다.
 *
 * 사진 고르기 화면은 시스템 화면이라 조작할 수 없으므로, 앱이 갤러리에서 가져올 때와 같은 저장소 함수로 만든 이미지를
 * 넣는다. 편집·공유처럼 FrameKit 화면을 여는 버튼은 누르지 않는다.
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class AlbumViewerFlowTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val rule = createAndroidComposeRule<MainActivity>()

    @Inject lateinit var vehicleRepository: VehicleRepository

    @Inject lateinit var albumRepository: AlbumRepository

    private val robot by lazy { AppRobot(rule) }

    @Before
    fun inject() = hiltRule.inject()

    @Test
    fun openPhoto_showsCounterAndActions_tapTogglesChrome_backRestoresNavigation() {
        robot.registerVehicle()
        addAlbumPhoto()

        robot.openTab(R.string.nav_vehicle)
        robot.clickText(VehicleR.string.album_see_all, 1)
        // 메모를 붙였으므로 그리드 칸과 사진 보기의 사진 모두 메모를 읽어 준다. 칸만 누를 수 있다.
        val thumbnail = hasContentDescription(PHOTO_NOTE) and hasClickAction()
        robot.click(thumbnail)

        val counter = hasText(robot.text(VehicleR.string.album_counter, 1, 1))
        robot.waitFor(counter)
        // 편집·공유는 FrameKit을 붙인 빌드에만 있어 항상 있는 버튼만 확인한다.
        listOf(
            VehicleR.string.album_action_details,
            VehicleR.string.album_action_cover,
            VehicleR.string.album_delete,
        ).forEach { robot.waitFor(hasText(robot.text(it)) and hasClickAction()) }

        // 한 칸 화면은 사진 보기가 창 전체를 덮어 내비게이션을 숨기고, 두 칸 화면은 그리드 옆에 연다.
        val navigation = robot.tab(R.string.nav_vehicle)
        if (!robot.exists(thumbnail)) robot.waitUntilGone(navigation)

        // 한 번 탭은 두 번 탭(확대)이 아닌지 기다린 뒤 처리되므로 막대가 사라질 때까지 기다리고 다시 누른다.
        val photo = hasContentDescription(PHOTO_NOTE) and !hasClickAction()
        robot.waitFor(photo)
        rule.onAllNodes(photo).onFirst().performClick()
        robot.waitUntilGone(counter)
        rule.onAllNodes(photo).onFirst().performClick()
        robot.waitFor(counter)

        robot.pressBack()
        robot.waitUntilGone(counter)
        robot.waitFor(navigation)
    }

    /** 단색 JPEG을 만들어 앨범에 넣고 메모를 붙인다. 앱이 가져올 때 다시 인코딩하므로 원본은 캐시에 둔다. */
    private fun addAlbumPhoto() = runBlocking {
        val vehicleId = vehicleRepository.observePrimaryVehicle().filterNotNull().first().id
        val source = File(rule.activity.cacheDir, "flow_album_photo.jpg")
        val bitmap = Bitmap.createBitmap(PHOTO_WIDTH, PHOTO_HEIGHT, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(Color.rgb(70, 120, 180))
        source.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, it) }
        bitmap.recycle()

        val today = LocalDate.now()
        val added = albumRepository.add(vehicleId, listOf(Uri.fromFile(source).toString()), today)
        albumRepository.updateDetails(vehicleId, added.addedIds.single(), today, PHOTO_NOTE)
        source.delete()
    }

    private companion object {
        const val PHOTO_NOTE = "새 타이어 끼운 날"
        const val PHOTO_WIDTH = 640
        const val PHOTO_HEIGHT = 480
        const val JPEG_QUALITY = 90
    }
}
