package com.naury.chageun.feature.vehicle.album

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.google.common.truth.Truth.assertThat
import com.naury.chageun.core.designsystem.theme.ChageunTheme
import com.naury.chageun.core.model.AlbumPhoto
import com.naury.chageun.core.ui.photo.PhotoInput
import com.naury.chageun.core.ui.photo.rememberPhotoInputState
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

/** 앨범에 넣기 창, 여러 장 고르기의 슬라이드쇼 버튼, 사진 보기의 공유 버튼. */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h800dp")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class AlbumAddShareTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val withVideo get() = AlbumFixtures.withVideo

    private fun state(
        selected: String? = "p2",
        picked: Set<String> = emptySet(),
        items: List<AlbumPhoto> = AlbumFixtures.photos,
    ) = AlbumFixtures.state(selected = selected, picked = picked, items = items)

    private fun setContentWithPhotos(items: List<AlbumPhoto> = AlbumFixtures.photos, content: @Composable () -> Unit) =
        composeRule.setContentWithPhotos(items, content)

    @Test
    fun viewer_shareSendsTheCurrentPhoto_orVideo() {
        val shared = mutableListOf<String>()
        setContentWithPhotos(withVideo) {
            ChageunTheme {
                AlbumScreen(
                    state(selected = "p1", items = withVideo),
                    isTwoPane = false,
                    actions = AlbumActions(onShare = { shared += it.id }),
                )
            }
        }

        composeRule.onNode(hasClickLabel("Share this photo")).performClick()
        composeRule.onNodeWithContentDescription("Photo 2 of 6, Video, 42 seconds, Highway drive").performClick()
        composeRule.onNode(hasClickLabel("Share this video")).performClick()

        assertThat(shared).containsExactly("p1", "v1").inOrder()
    }

    @Test
    fun viewer_hidesShareWithoutAnEditor() {
        setContentWithPhotos {
            ChageunTheme { AlbumScreen(state(), isTwoPane = false, actions = AlbumActions()) }
        }

        composeRule.onNodeWithText("Share").assertDoesNotExist()
    }

    @Test
    fun pickingBar_slideshowNeedsTwoPhotos() {
        var picked by mutableStateOf(setOf("p1"))
        var made = 0
        setContentWithPhotos(withVideo) {
            ChageunTheme {
                AlbumListScaffold(
                    state(selected = null, picked = picked, items = withVideo),
                    AlbumBarActions(onMakeSlideshow = { made++ }),
                ) {}
            }
        }
        val slideshow = composeRule.onNodeWithContentDescription("Make a slideshow from selected photos")
        slideshow.assertIsNotEnabled()

        // 동영상은 슬라이드쇼에 들어가지 않으므로 사진 한 장과 동영상으로는 만들 수 없다.
        picked = setOf("p1", "v1")
        slideshow.assertIsNotEnabled()

        picked = setOf("p1", "v1", "p2")
        slideshow.assertIsEnabled().performClick()
        assertThat(made).isEqualTo(1)
    }

    @Test
    fun pickingBar_controls_haveLabelsAndLargeTouchTargets() {
        setContentWithPhotos {
            ChageunTheme {
                AlbumListScaffold(state(selected = null, picked = setOf("p1", "p2")), AlbumBarActions()) {}
            }
        }

        composeRule.assertAccessibleControls()
    }

    @Test
    @Config(fontScale = 2f)
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun pickingBar_largeFont_keepsEveryTextVisible() {
        setContentWithPhotos {
            ChageunTheme {
                AlbumListScaffold(state(selected = null, picked = setOf("p1", "p2")), AlbumBarActions()) {}
            }
        }

        composeRule.assertNoClippedText()
    }

    @Test
    fun addDialog_offersCameraAndPicking_only_andHandsPickingToTheEditor() {
        var picked = 0
        showAddDialog(onPick = { picked++ })

        composeRule.onNodeWithText("Take a photo").assertIsDisplayed()
        composeRule.onNodeWithText("Edit photos, then add").assertDoesNotExist()
        composeRule.onNodeWithText("Edit a video, then add").assertDoesNotExist()
        composeRule.onNodeWithText("Choose photos or videos").performClick()

        assertThat(picked).isEqualTo(1)
        composeRule.onNodeWithText("Take a photo").assertDoesNotExist()
    }

    @Test
    fun addDialog_controls_haveLabelsAndLargeTouchTargets() {
        showAddDialog()

        composeRule.assertAccessibleControls()
    }

    @Test
    @Config(fontScale = 2f)
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun addDialog_largeFont_keepsEveryTextVisible() {
        showAddDialog()

        composeRule.assertNoClippedText()
    }

    @Test
    @Config(qualifiers = ScreenshotDevices.PHONE_KO)
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun screenshot_addDialogKorean() {
        showAddDialog()
        composeRule.captureScreen("album_add_dialog_ko")
    }

    @Test
    @Config(qualifiers = ScreenshotDevices.PHONE_KO)
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun screenshot_pickingBarKorean() {
        setContentWithPhotos {
            AppFrame {
                val uiState = state(selected = null, picked = setOf("p1", "p2", "p4"))
                AlbumListScaffold(uiState, AlbumBarActions()) { padding ->
                    AlbumScreen(uiState, isTwoPane = false, actions = AlbumActions(), Modifier.padding(padding))
                }
            }
        }
        composeRule.captureScreen("album_picking_bar_ko")
    }

    private fun showAddDialog(onPick: () -> Unit = {}) {
        composeRule.setContent {
            AppFrame {
                val input = rememberPhotoInputState()
                LaunchedEffect(Unit) { input.open() }
                PhotoInput(input, maxItems = 20, onPhotos = { _, _ ->
                }, includeVideos = true, onPickFromGallery = onPick)
            }
        }
        composeRule.waitForIdle()
    }
}
