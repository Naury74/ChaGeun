package com.naury.chageun.feature.vehicle.album

import android.content.Context
import android.view.accessibility.AccessibilityManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.pinch
import androidx.compose.ui.test.swipeDown
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.naury.chageun.core.designsystem.theme.ChageunTheme
import com.naury.chageun.core.designsystem.theme.Gutters
import com.naury.chageun.core.model.AlbumPhoto
import com.naury.chageun.core.uitesting.AppFrame
import com.naury.chageun.core.uitesting.ScreenshotDevices
import com.naury.chageun.core.uitesting.assertAccessibleControls
import com.naury.chageun.core.uitesting.assertNoClippedText
import com.naury.chageun.core.uitesting.captureScreen
import java.time.LocalDate
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h800dp")
// 사진 파일을 실제로 디코딩해야 하므로 모든 테스트를 native 그래픽으로 돌린다.
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class AlbumScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val photos get() = AlbumFixtures.photos
    private val withVideo get() = AlbumFixtures.withVideo

    private fun state(
        selected: String? = "p2",
        isCoverUpdated: Boolean = false,
        picked: Set<String> = emptySet(),
        items: List<AlbumPhoto> = photos,
        isEditFailed: Boolean = false,
    ) = AlbumFixtures.state(selected, isCoverUpdated, picked, items, isEditFailed)

    @Test
    fun viewer_showsCounterAndCurrentPhotoDetails() {
        setContentWithPhotos {
            ChageunTheme { AlbumScreen(state(), isTwoPane = false, actions = AlbumActions()) }
        }

        composeRule.onNodeWithText("2 / 5").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Photo 2 of 5").assertIsDisplayed()
        composeRule.onNodeWithText("New tires").assertIsDisplayed()
    }

    @Test
    fun viewer_actionsReportTheCurrentPhoto() {
        val events = mutableListOf<String>()
        val actions = AlbumActions(
            onEdit = { events += "edit:$it" },
            onSetCover = { events += "cover:${it.id}" },
            onDelete = { events += "delete:$it" },
        )
        setContentWithPhotos { ChageunTheme { AlbumScreen(state(), isTwoPane = false, actions = actions) } }

        composeRule.onNodeWithText("Details").performClick()
        composeRule.onNodeWithText("Car photo").performClick()
        composeRule.onNodeWithText("Delete").performClick()
        composeRule.onNodeWithText("New tires").performClick()

        assertThat(events).containsExactly("edit:p2", "cover:p2", "delete:p2", "edit:p2").inOrder()
    }

    @Test
    fun viewer_withoutNote_offersToWriteOne() {
        var editing: String? = null
        setContentWithPhotos {
            ChageunTheme {
                AlbumScreen(
                    state(selected = "p3"),
                    isTwoPane = false,
                    actions = AlbumActions(onEdit = {
                        editing = it
                    }),
                )
            }
        }

        composeRule.onNodeWithText("Write a note").performClick()

        assertThat(editing).isEqualTo("p3")
    }

    @Test
    fun viewer_showsImageEditOnlyWhenAnEditorIsAttached() {
        var editor by mutableStateOf<((AlbumPhoto) -> Unit)?>(null)
        var edited: String? = null
        setContentWithPhotos {
            ChageunTheme {
                AlbumScreen(state(), isTwoPane = false, actions = AlbumActions(onEditImage = editor))
            }
        }
        composeRule.onNodeWithText("Edit").assertDoesNotExist()

        editor = { edited = it.id }
        composeRule.onNodeWithText("Edit").performClick()

        assertThat(edited).isEqualTo("p2")
    }

    @Test
    fun viewer_dragDownCloses() {
        val selections = mutableListOf<String?>()
        setContentWithPhotos {
            ChageunTheme {
                AlbumScreen(state(), isTwoPane = false, actions = AlbumActions(onSelect = { selections += it }))
            }
        }

        composeRule.onNodeWithContentDescription("New tires").performTouchInput {
            swipeDown(startY = centerY, endY = centerY + 400f, durationMillis = 300)
        }

        assertThat(selections.last()).isNull()
    }

    @Test
    fun viewer_backButtonClosesThePhoto() {
        val selections = mutableListOf<String?>()
        setContentWithPhotos {
            ChageunTheme {
                AlbumScreen(state(), isTwoPane = false, actions = AlbumActions(onSelect = { selections += it }))
            }
        }

        composeRule.onNodeWithContentDescription("Back").performClick()

        assertThat(selections.last()).isNull()
    }

    @Test
    fun thumbnailStrip_jumpsToTheTappedPhoto() {
        val selections = mutableListOf<String?>()
        setContentWithPhotos {
            ChageunTheme {
                AlbumScreen(state(), isTwoPane = false, actions = AlbumActions(onSelect = { selections += it }))
            }
        }

        composeRule.onNodeWithContentDescription("Photo 2 of 5, New tires").assertIsSelected()
        composeRule.onNodeWithContentDescription("Photo 1 of 5, Car wash before the trip").performClick()

        composeRule.onNodeWithText("1 / 5").assertIsDisplayed()
        assertThat(selections.last()).isEqualTo("p1")
    }

    @Test
    fun date_opensTheCalendarOnThatDay() {
        var opened: LocalDate? = null
        setContentWithPhotos {
            ChageunTheme {
                AlbumScreen(state(), isTwoPane = false, actions = AlbumActions(onOpenCalendar = { opened = it }))
            }
        }

        composeRule.onNode(hasClickLabel("View this day in the calendar")).performClick()

        assertThat(opened).isEqualTo(LocalDate.of(2026, 10, 3))
    }

    @Test
    fun tappingThePhoto_hidesAndRestoresControls() {
        setContentWithPhotos {
            ChageunTheme { AlbumScreen(state(), isTwoPane = false, actions = AlbumActions()) }
        }

        tapPhoto("New tires")
        composeRule.onNodeWithText("Details").assertDoesNotExist()
        composeRule.onNodeWithText("2 / 5").assertDoesNotExist()

        tapPhoto("New tires")
        composeRule.onNodeWithText("Details").assertIsDisplayed()
    }

    @Test
    fun withTouchExploration_controlsStayReachable() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        shadowOf(context.getSystemService(AccessibilityManager::class.java)).setTouchExplorationEnabled(true)
        setContentWithPhotos {
            ChageunTheme { AlbumScreen(state(), isTwoPane = false, actions = AlbumActions()) }
        }

        tapPhoto("New tires")

        composeRule.onNodeWithText("Details").assertIsDisplayed()
        composeRule.onNodeWithText("2 / 5").assertIsDisplayed()
    }

    @Test
    fun viewer_controls_haveLabelsAndLargeTouchTargets() {
        setContentWithPhotos {
            ChageunTheme {
                AlbumScreen(
                    state(),
                    isTwoPane = false,
                    actions = AlbumActions(onOpenCalendar = {
                    }, onEditImage = {}, onShare = {}),
                )
            }
        }

        composeRule.assertAccessibleControls()
    }

    @Test
    @Config(qualifiers = "w840dp-h900dp")
    fun twoPane_gridAndViewer_haveLabelsAndLargeTouchTargets() {
        setContentWithPhotos {
            ChageunTheme(spacing = Gutters.Expanded) {
                AlbumScreen(state(), isTwoPane = true, actions = AlbumActions(onOpenCalendar = {}))
            }
        }

        composeRule.onNodeWithContentDescription("Back").assertDoesNotExist()
        composeRule.onNodeWithText("2 / 5").assertIsDisplayed()
        composeRule.assertAccessibleControls()
    }

    @Test
    fun grid_longPressStartsPicking_andTapOpensOtherwise() {
        val events = mutableListOf<String>()
        val actions = AlbumActions(onSelect = { events += "open:$it" }, onTogglePick = { events += "pick:$it" })
        setContentWithPhotos {
            ChageunTheme { AlbumScreen(state(selected = null), isTwoPane = false, actions = actions) }
        }

        composeRule.onNodeWithContentDescription("New tires").performClick()
        composeRule.onNodeWithContentDescription("New tires").performTouchInput { longClick() }

        assertThat(events).containsExactly("open:p2", "pick:p2").inOrder()
    }

    @Test
    fun grid_whilePicking_tapTogglesAndHeaderPicksTheDay() {
        val events = mutableListOf<String>()
        val actions = AlbumActions(
            onSelect = { events += "open:$it" },
            onTogglePick = { events += "pick:$it" },
            onPickDay = { events += "day:$it" },
        )
        setContentWithPhotos {
            ChageunTheme {
                AlbumScreen(state(selected = null, picked = setOf("p1", "p2")), isTwoPane = false, actions = actions)
            }
        }

        composeRule.onNodeWithContentDescription("New tires").assertIsSelected().performClick()
        composeRule.onNodeWithText("Deselect").performClick()
        composeRule.onAllNodesWithText("Select all")[0].performClick()

        assertThat(events).containsExactly("pick:p2", "day:2026-10-03", "day:2026-09-21").inOrder()
    }

    @Test
    fun grid_pinchChangesThumbnailSize() {
        val steps = mutableListOf<Int>()
        setContentWithPhotos {
            ChageunTheme {
                AlbumScreen(
                    state(selected = null),
                    isTwoPane = false,
                    actions = AlbumActions(onZoomGrid = {
                        steps += it
                    }),
                )
            }
        }

        composeRule.onRoot().performTouchInput {
            pinch(
                center - Offset(20f, 0f),
                center - Offset(150f, 0f),
                center + Offset(20f, 0f),
                center + Offset(150f, 0f),
            )
        }
        composeRule.onRoot().performTouchInput {
            pinch(
                center - Offset(150f, 0f),
                center - Offset(20f, 0f),
                center + Offset(150f, 0f),
                center + Offset(20f, 0f),
            )
        }

        assertThat(steps).containsExactly(1, -1).inOrder()
    }

    @Test
    fun grid_controls_haveLabelsAndLargeTouchTargets() {
        setContentWithPhotos {
            ChageunTheme {
                AlbumScreen(state(selected = null, picked = setOf("p1")), isTwoPane = false, actions = AlbumActions())
            }
        }

        composeRule.assertAccessibleControls()
    }

    @Test
    @Config(fontScale = 2f)
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun viewer_largeFont_keepsEveryTextVisible() {
        setContentWithPhotos {
            ChageunTheme {
                AlbumScreen(
                    state(isCoverUpdated = true),
                    isTwoPane = false,
                    actions = AlbumActions(onOpenCalendar = {}, onEditImage = {}, onShare = {}),
                )
            }
        }

        composeRule.assertNoClippedText()
    }

    @Test
    @Config(fontScale = 2f)
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun grid_largeFont_keepsEveryTextVisible() {
        setContentWithPhotos {
            ChageunTheme { AlbumScreen(state(selected = null), isTwoPane = false, actions = AlbumActions()) }
        }

        composeRule.assertNoClippedText()
    }

    @Test
    @Config(qualifiers = ScreenshotDevices.PHONE_KO)
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun screenshot_viewerPhoneKorean() {
        setContentWithPhotos {
            AppFrame {
                AlbumScreen(
                    state(),
                    isTwoPane = false,
                    actions = AlbumActions(onOpenCalendar = {
                    }, onEditImage = {}, onShare = {}),
                )
            }
        }
        composeRule.captureScreen("album_viewer_phone_ko")
    }

    @Test
    @Config(qualifiers = ScreenshotDevices.PHONE_DARK)
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun screenshot_viewerDark_withoutNote_afterSettingCover() {
        setContentWithPhotos {
            AppFrame {
                AlbumScreen(state(selected = "p3", isCoverUpdated = true), isTwoPane = false, actions = AlbumActions())
            }
        }
        composeRule.captureScreen("album_viewer_dark")
    }

    @Test
    @Config(qualifiers = ScreenshotDevices.EXPANDED_KO)
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun screenshot_twoPaneKorean() {
        setContentWithPhotos {
            AppFrame(spacing = Gutters.Expanded) {
                AlbumScreen(state(), isTwoPane = true, actions = AlbumActions(onOpenCalendar = {}))
            }
        }
        composeRule.captureScreen("album_two_pane_ko")
    }

    @Test
    @Config(qualifiers = ScreenshotDevices.FOLD_KO)
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun screenshot_foldKorean() {
        setContentWithPhotos {
            AppFrame(spacing = Gutters.Medium) {
                AlbumScreen(state(selected = "p5"), isTwoPane = true, actions = AlbumActions(onOpenCalendar = {}))
            }
        }
        composeRule.captureScreen("album_fold_ko")
    }

    @Test
    @Config(qualifiers = ScreenshotDevices.PHONE_KO)
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun screenshot_viewerFullScreenKorean() {
        setContentWithPhotos {
            AppFrame { AlbumScreen(state(selected = "p1"), isTwoPane = false, actions = AlbumActions()) }
        }
        tapPhoto("Car wash before the trip")
        composeRule.captureScreen("album_viewer_full_ko")
    }

    @Test
    @Config(qualifiers = ScreenshotDevices.PHONE_KO)
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun screenshot_gridPickingKorean() {
        setContentWithPhotos {
            AppFrame {
                AlbumScreen(
                    state(selected = null, picked = setOf("p1", "p4")),
                    isTwoPane = false,
                    actions = AlbumActions(),
                )
            }
        }
        composeRule.captureScreen("album_grid_picking_ko")
    }

    @Test
    @Config(qualifiers = ScreenshotDevices.PHONE_KO)
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun screenshot_gridKorean() {
        setContentWithPhotos {
            AppFrame { AlbumScreen(state(selected = null), isTwoPane = false, actions = AlbumActions()) }
        }
        composeRule.captureScreen("album_grid_ko")
    }

    @Test
    fun grid_videoCell_showsDuration_andIsReadAsAVideo() {
        setContentWithPhotos(withVideo) {
            ChageunTheme { AlbumScreen(state(selected = null, items = withVideo), isTwoPane = false, AlbumActions()) }
        }

        composeRule.onNodeWithText("0:42").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Video, 42 seconds, Highway drive").assertIsDisplayed()
    }

    @Test
    fun viewer_videoPage_offersPlay_editAndNoCoverAction() {
        val edited = mutableListOf<String>()
        setContentWithPhotos(withVideo) {
            ChageunTheme {
                AlbumScreen(
                    state(selected = "v1", items = withVideo),
                    isTwoPane = false,
                    actions = AlbumActions(onEditImage = { edited += it.id }),
                )
            }
        }

        composeRule.onNodeWithText("2 / 6").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Play video").assertIsDisplayed().assertHasClickAction()
        composeRule.onNodeWithContentDescription("Video, 42 seconds, Highway drive").assertIsDisplayed()
        composeRule.onNodeWithText("Car photo").assertDoesNotExist()
        composeRule.onNode(hasClickLabel("Edit this video")).performClick()
        assertThat(edited).containsExactly("v1")
    }

    @Test
    fun viewer_videoPage_tapTogglesControls_likePhotos() {
        setContentWithPhotos(withVideo) {
            ChageunTheme { AlbumScreen(state(selected = "v1", items = withVideo), isTwoPane = false, AlbumActions()) }
        }

        // 가운데는 재생 버튼이라 비켜서 누른다.
        tapVideoBesidePlay()
        composeRule.onNodeWithText("Details").assertDoesNotExist()
        composeRule.onNodeWithContentDescription("Play video").assertIsDisplayed()

        tapVideoBesidePlay()
        composeRule.onNodeWithText("Details").assertIsDisplayed()
    }

    @Test
    fun viewer_editFailure_isAnnounced() {
        setContentWithPhotos {
            ChageunTheme { AlbumScreen(state(isEditFailed = true), isTwoPane = false, actions = AlbumActions()) }
        }

        composeRule.onNodeWithText("Couldn't save your edit. Please try again.").assertIsDisplayed()
    }

    @Test
    fun videoViewer_controls_haveLabelsAndLargeTouchTargets() {
        setContentWithPhotos(withVideo) {
            ChageunTheme {
                AlbumScreen(
                    state(selected = "v1", items = withVideo),
                    isTwoPane = false,
                    actions = AlbumActions(onOpenCalendar = {}, onEditImage = {}, onShare = {}),
                )
            }
        }

        composeRule.assertAccessibleControls()
    }

    @Test
    @Config(fontScale = 2f)
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun videoViewer_largeFont_keepsEveryTextVisible() {
        setContentWithPhotos(withVideo) {
            ChageunTheme {
                AlbumScreen(
                    state(selected = "v1", items = withVideo),
                    isTwoPane = false,
                    actions = AlbumActions(onOpenCalendar = {}, onEditImage = {}, onShare = {}),
                )
            }
        }

        composeRule.assertNoClippedText()
    }

    @Test
    @Config(fontScale = 2f)
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun videoGrid_largeFont_keepsEveryTextVisible() {
        setContentWithPhotos(withVideo) {
            ChageunTheme { AlbumScreen(state(selected = null, items = withVideo), isTwoPane = false, AlbumActions()) }
        }

        composeRule.assertNoClippedText()
    }

    @Test
    @Config(qualifiers = ScreenshotDevices.PHONE_KO)
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun screenshot_gridWithVideoKorean() {
        setContentWithPhotos(withVideo) {
            AppFrame { AlbumScreen(state(selected = null, items = withVideo), isTwoPane = false, AlbumActions()) }
        }
        composeRule.captureScreen("album_grid_video_ko")
    }

    @Test
    @Config(qualifiers = PHONE_KO_DARK)
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun screenshot_gridWithVideoKoreanDark() {
        setContentWithPhotos(withVideo) {
            AppFrame { AlbumScreen(state(selected = null, items = withVideo), isTwoPane = false, AlbumActions()) }
        }
        composeRule.captureScreen("album_grid_video_ko_dark")
    }

    @Test
    @Config(qualifiers = ScreenshotDevices.PHONE_KO)
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun screenshot_viewerVideoKorean() {
        setContentWithPhotos(withVideo) {
            AppFrame {
                AlbumScreen(
                    state(selected = "v1", items = withVideo),
                    isTwoPane = false,
                    actions = AlbumActions(onOpenCalendar = {}, onEditImage = {}, onShare = {}),
                )
            }
        }
        composeRule.captureScreen("album_viewer_video_ko")
    }

    @Test
    @Config(qualifiers = PHONE_KO_DARK)
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun screenshot_viewerVideoKoreanDark() {
        setContentWithPhotos(withVideo) {
            AppFrame {
                AlbumScreen(
                    state(selected = "v1", items = withVideo),
                    isTwoPane = false,
                    actions = AlbumActions(onOpenCalendar = {}, onEditImage = {}, onShare = {}),
                )
            }
        }
        composeRule.captureScreen("album_viewer_video_ko_dark")
    }

    private fun setContentWithPhotos(items: List<AlbumPhoto> = photos, content: @Composable () -> Unit) =
        composeRule.setContentWithPhotos(items, content)

    // 두 번 누르기를 기다리는 시간이 지나야 한 번 누르기로 처리된다.
    private fun tapPhoto(description: String) {
        composeRule.onNodeWithContentDescription(description).performTouchInput { click() }
        composeRule.mainClock.advanceTimeBy(DOUBLE_TAP_WAIT_MS)
        composeRule.waitForIdle()
    }

    private fun tapVideoBesidePlay() {
        composeRule.onNodeWithContentDescription("Video, 42 seconds, Highway drive").performTouchInput {
            click(Offset(width * 0.2f, height * 0.3f))
        }
        composeRule.mainClock.advanceTimeBy(DOUBLE_TAP_WAIT_MS)
        composeRule.waitForIdle()
    }

    private companion object {
        const val PHONE_KO_DARK = "ko-w360dp-h800dp-night-hdpi"
        const val DOUBLE_TAP_WAIT_MS = 1_000L
    }
}
