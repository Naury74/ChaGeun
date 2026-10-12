package com.naury.chageun.feature.vehicle.album

import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.google.common.truth.Truth.assertThat
import com.naury.chageun.core.designsystem.theme.ChageunTheme
import com.naury.chageun.core.model.AlbumPhoto
import com.naury.chageun.core.model.VehicleId
import com.naury.chageun.core.ui.UNDO_BAR_MILLIS
import com.naury.chageun.core.uitesting.AppFrame
import com.naury.chageun.core.uitesting.ScreenshotDevices
import com.naury.chageun.core.uitesting.assertAccessibleControls
import com.naury.chageun.core.uitesting.assertNoClippedText
import com.naury.chageun.core.uitesting.captureScreen
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** 사진을 지운 뒤 아래에 뜨는 되돌리기 막대. */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h800dp")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class AlbumUndoTest {

    @get:Rule
    val composeRule = createComposeRule()

    // p1·p2를 지워 남은 사진만 보이는 앨범.
    private fun state(selected: String? = null, undo: AlbumUndo? = photosDeleted, items: List<AlbumPhoto> = left) =
        AlbumFixtures.state(selected = selected, items = items).copy(undo = undo)

    private val left get() = AlbumFixtures.photos.drop(2)
    private val photosDeleted = AlbumUndo(VehicleId("v1"), listOf("p1", "p2"), photoCount = 2, videoCount = 0)

    @Test
    fun grid_afterDelete_showsMessage_andUndoBringsThemBack() {
        val events = mutableListOf<String>()
        composeRule.setContentWithPhotos(left) {
            ChageunTheme {
                AlbumScreen(
                    state(),
                    isTwoPane = false,
                    actions = AlbumActions(onUndoDelete = {
                        events += "undo"
                    }, onFinishDelete = { events += "finish" }),
                )
            }
        }

        composeRule.onNodeWithText("Deleted 2 photos").assertIsDisplayed()
        composeRule.onNodeWithText("Undo").assertHasClickAction().performClick()

        assertThat(events).containsExactly("undo")
    }

    @Test
    fun message_isAnnounced_andTheUndoButtonIsReachable() {
        composeRule.setContentWithPhotos(left) {
            ChageunTheme { AlbumScreen(state(), isTwoPane = false, actions = AlbumActions()) }
        }

        composeRule.onNodeWithText("Deleted 2 photos")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite))
        composeRule.assertAccessibleControls()
    }

    @Test
    fun message_finishesTheDelete_whenTimeRunsOut() {
        var finished = 0
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            ChageunTheme {
                AlbumScreen(
                    state(items = emptyList()),
                    isTwoPane = false,
                    AlbumActions(onFinishDelete = {
                        finished++
                    }),
                )
            }
        }

        composeRule.mainClock.advanceTimeBy(UNDO_BAR_MILLIS / 2)
        assertThat(finished).isEqualTo(0)
        composeRule.mainClock.advanceTimeBy(UNDO_BAR_MILLIS)

        assertThat(finished).isEqualTo(1)
    }

    @Test
    fun mixedDelete_countsPhotosAndVideosTogether() {
        val undo = AlbumUndo(VehicleId("v1"), listOf("p1", "v1", "p2"), photoCount = 2, videoCount = 1)
        composeRule.setContentWithPhotos(left) {
            ChageunTheme { AlbumScreen(state(undo = undo), isTwoPane = false, actions = AlbumActions()) }
        }

        composeRule.onNodeWithText("Deleted 3 photos and videos").assertIsDisplayed()
    }

    @Test
    fun viewer_afterDelete_showsMessageAboveTheControls() {
        val undo = AlbumUndo(VehicleId("v1"), listOf("v1"), photoCount = 0, videoCount = 1)
        composeRule.setContentWithPhotos(left) {
            ChageunTheme {
                AlbumScreen(state(selected = "p3", undo = undo), isTwoPane = false, actions = AlbumActions())
            }
        }

        composeRule.onNodeWithText("Deleted 1 video").assertIsDisplayed()
        composeRule.onNodeWithText("Undo").assertHasClickAction()
    }

    @Test
    @Config(fontScale = 2f)
    fun largeFont_keepsTheMessageAndButtonVisible() {
        composeRule.setContentWithPhotos(left) {
            ChageunTheme { AlbumScreen(state(), isTwoPane = false, actions = AlbumActions()) }
        }

        composeRule.onNodeWithText("Undo").assertIsDisplayed()
        composeRule.assertNoClippedText()
    }

    @Test
    @Config(qualifiers = ScreenshotDevices.PHONE_KO)
    fun screenshot_gridAfterDeleteKorean() {
        composeRule.setContentWithPhotos(left) {
            AppFrame { AlbumScreen(state(), isTwoPane = false, actions = AlbumActions()) }
        }
        composeRule.captureScreen("album_undo_ko")
    }

    @Test
    @Config(qualifiers = PHONE_KO_DARK)
    fun screenshot_gridAfterDeleteKoreanDark() {
        composeRule.setContentWithPhotos(left) {
            AppFrame { AlbumScreen(state(), isTwoPane = false, actions = AlbumActions()) }
        }
        composeRule.captureScreen("album_undo_ko_dark")
    }

    @Test
    @Config(qualifiers = ScreenshotDevices.PHONE_KO)
    fun screenshot_viewerAfterDeleteKorean() {
        composeRule.setContentWithPhotos(left) {
            AppFrame {
                AlbumScreen(
                    state(selected = "p3", undo = photosDeleted.copy(ids = listOf("p2"), photoCount = 1)),
                    isTwoPane = false,
                    actions = AlbumActions(onEditImage = {}, onShare = {}),
                )
            }
        }
        composeRule.captureScreen("album_undo_viewer_ko")
    }

    private companion object {
        const val PHONE_KO_DARK = "ko-w360dp-h800dp-night-hdpi"
    }
}
