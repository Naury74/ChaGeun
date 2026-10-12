package com.naury.chageun.feature.home

import android.graphics.Bitmap
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.google.common.truth.Truth.assertThat
import com.naury.chageun.core.designsystem.theme.ChageunTheme
import com.naury.chageun.core.model.VehicleHealthLevel
import com.naury.chageun.core.ui.PROFILE_AVATAR_INITIAL_TAG
import com.naury.chageun.core.ui.PROFILE_AVATAR_PHOTO_TAG
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h2000dp")
class HomeAccountAvatarTest {

    @get:Rule
    val composeRule = createComposeRule()

    @get:Rule
    val folder = TemporaryFolder()

    private var openedAccount = 0

    private fun show(name: String?, avatarPath: String?) = composeRule.setContent {
        CompositionLocalProvider(LocalFrozenSkyTime provides 0L) {
            ChageunTheme {
                HomeScreen(
                    uiState = content(VehicleHealthLevel.Good, emptyList())
                        .copy(greetingName = name, avatarPath = avatarPath),
                    paneCount = 1,
                    actions = HomeActions({}, {}, {}, onOpenAccount = { openedAccount++ }),
                )
            }
        }
    }

    private fun photoFile() = folder.newFile("avatar.jpg").apply {
        outputStream().use {
            Bitmap.createBitmap(16, 16, Bitmap.Config.ARGB_8888).compress(Bitmap.CompressFormat.JPEG, 90, it)
        }
    }

    @Test
    fun signedInWithPhoto_showsPhoto_andReadsAccountWithName() {
        show("나우리", photoFile().path)

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodes(hasTestTag(PROFILE_AVATAR_PHOTO_TAG), useUnmergedTree = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
        val button = composeRule.onNode(hasContentDescription("Account, 나우리") and hasClickAction())
        button.assertExists()
        button.performClick()
        assertThat(openedAccount).isEqualTo(1)
    }

    @Test
    fun signedInWithoutPhoto_showsInitial() {
        show("나우리", null)

        composeRule.onNodeWithTag(PROFILE_AVATAR_INITIAL_TAG, useUnmergedTree = true).assertExists()
        composeRule.onNode(hasContentDescription("Account, 나우리") and hasClickAction()).assertExists()
    }

    @Test
    fun signedOut_ignoresLeftoverPhoto_andReadsAccount() {
        show(null, photoFile().path)

        composeRule.onNodeWithTag(PROFILE_AVATAR_PHOTO_TAG, useUnmergedTree = true).assertDoesNotExist()
        composeRule.onNode(hasContentDescription("Account") and hasClickAction()).assertExists()
    }
}
