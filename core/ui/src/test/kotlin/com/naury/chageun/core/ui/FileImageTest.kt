package com.naury.chageun.core.ui

import android.graphics.Bitmap
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import com.google.common.truth.Truth.assertThat
import com.naury.chageun.core.designsystem.theme.ChageunTheme
import java.io.File
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class FileImageTest {

    @get:Rule
    val composeRule = createComposeRule()

    @get:Rule
    val folder = TemporaryFolder()

    private fun writeImage(name: String): File = folder.newFile(name).apply {
        outputStream().use {
            Bitmap.createBitmap(8, 4, Bitmap.Config.ARGB_8888).compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }

    @Test
    fun missingFile_isReportedAsMissing_notLoading() {
        var state: FileImageState? = null
        composeRule.setContent { state = rememberFileImageState(File(folder.root, "none.png").path).value }

        composeRule.waitUntil(timeoutMillis = 5_000) { state == FileImageState.Missing }
    }

    @Test
    fun heroShowsPhotoImmediately_whenComingBackToTheScreen() {
        val file = writeImage("car_cutout.png")
        var isShown by mutableStateOf(true)
        composeRule.setContent {
            ChageunTheme {
                if (isShown) {
                    VehicleHeroSection(
                        title = "Kia Sportage",
                        subtitle = "2023",
                        mileage = null,
                        freshness = null,
                        photoPath = file.path,
                    )
                }
            }
        }
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodes(hasContentDescription(PHOTO_DESCRIPTION)).fetchSemanticsNodes().isNotEmpty()
        }

        // 탭을 떠났다가 돌아오는 상황. 파일을 지워 두면 다시 읽어서는 사진이 나올 수 없으므로
        // 첫 프레임부터 사진이 보인다면 캐시에서 나온 것이다.
        isShown = false
        composeRule.waitForIdle()
        assertThat(file.delete()).isTrue()
        isShown = true
        composeRule.waitForIdle()

        composeRule.onNodeWithContentDescription(PHOTO_DESCRIPTION).assertExists()
    }

    private companion object {
        const val PHOTO_DESCRIPTION = "Photo of your car"
    }
}
