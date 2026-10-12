package com.naury.chageun.core.ui

import android.graphics.Bitmap
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
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
class ProfileAvatarTest {

    @get:Rule
    val composeRule = createComposeRule()

    @get:Rule
    val folder = TemporaryFolder()

    private fun show(name: String?, photoPath: String?) = composeRule.setContent {
        ChageunTheme { ProfileAvatar(name = name, photoPath = photoPath, size = 48.dp) }
    }

    @Test
    fun showsPhoto_whenFileLoads() {
        val photo = folder.newFile("avatar.jpg").apply {
            outputStream().use {
                Bitmap.createBitmap(16, 16, Bitmap.Config.ARGB_8888).compress(Bitmap.CompressFormat.JPEG, 90, it)
            }
        }
        show("나우리", photo.path)

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodes(hasTestTag(PROFILE_AVATAR_PHOTO_TAG)).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag(PROFILE_AVATAR_INITIAL_TAG).assertDoesNotExist()
    }

    @Test
    fun showsInitial_whenPhotoIsMissingOrBroken() {
        val broken = folder.newFile("broken.jpg").apply { writeText("not an image") }
        show("나우리", broken.path)

        composeRule.onNodeWithTag(PROFILE_AVATAR_INITIAL_TAG).assertExists()
        composeRule.onNodeWithText("나").assertExists()
        composeRule.onNodeWithTag(PROFILE_AVATAR_PHOTO_TAG).assertDoesNotExist()
    }

    @Test
    fun showsInitial_withoutPhoto() {
        show("driver", File(folder.root, "none.jpg").path)

        composeRule.onNodeWithText("D").assertExists()
    }
}
