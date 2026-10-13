package com.naury.chageun.feature.vehicle.album

import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.lifecycle.Lifecycle
import com.google.common.truth.Truth.assertThat
import com.naury.chageun.core.designsystem.theme.ChageunTheme
import com.naury.chageun.core.model.VehicleId
import com.naury.chageun.core.ui.UNDO_BAR_MILLIS
import com.naury.chageun.core.ui.launchExternal
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** 되돌리기를 기다리는 중에 앨범이 띄운 화면으로 다녀올 때와 앱을 떠날 때. */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h800dp")
class AlbumUndoLeaveTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val deleted = AlbumUndo(VehicleId("v1"), listOf("p1"), photoCount = 1, videoCount = 0)
    private var undo by mutableStateOf<AlbumUndo?>(deleted)
    private var finished = 0

    @Before
    fun setUp() {
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            ChageunTheme {
                val finish = {
                    if (undo != null) finished++
                    undo = null
                }
                FinishDeleteWhenLeaving(finish)
                AlbumScreen(
                    AlbumFixtures.state(items = emptyList()).copy(undo = undo),
                    isTwoPane = false,
                    actions = AlbumActions(onFinishDelete = finish),
                )
            }
        }
        composeRule.waitForIdle()
    }

    @Test
    fun openingTheEditorOrShare_keepsUndo_untilComingBack() {
        // 편집기·공유 창은 launchExternal로 띄운다.
        composeRule.activity.launchExternal {}
        moveTo(Lifecycle.State.CREATED)
        moveTo(Lifecycle.State.RESUMED)

        assertThat(finished).isEqualTo(0)
        composeRule.onNodeWithText("Undo").assertIsDisplayed()
    }

    @Test
    fun shareRightAfterTheEditorResult_keepsUndo_too() {
        composeRule.activity.launchExternal {}
        moveTo(Lifecycle.State.CREATED)
        // 편집기 결과는 ON_START와 ON_RESUME 사이에 오고, 공유를 고르면 곧바로 공유 창을 띄운다.
        moveTo(Lifecycle.State.STARTED)
        composeRule.activity.launchExternal {}
        moveTo(Lifecycle.State.CREATED)

        assertThat(finished).isEqualTo(0)
    }

    @Test
    fun leavingTheApp_finishesTheDelete() {
        moveTo(Lifecycle.State.CREATED)

        assertThat(finished).isEqualTo(1)
    }

    @Test
    fun leavingTheApp_afterComingBackFromTheEditor_finishesTheDelete() {
        composeRule.activity.launchExternal {}
        moveTo(Lifecycle.State.CREATED)
        moveTo(Lifecycle.State.RESUMED)
        moveTo(Lifecycle.State.CREATED)

        assertThat(finished).isEqualTo(1)
    }

    @Test
    fun timeRunningOut_whileInTheEditor_finishesTheDelete() {
        composeRule.activity.launchExternal {}
        moveTo(Lifecycle.State.CREATED)
        composeRule.mainClock.advanceTimeBy(UNDO_BAR_MILLIS * 2)
        moveTo(Lifecycle.State.RESUMED)
        composeRule.mainClock.advanceTimeBy(UNDO_BAR_MILLIS * 2)

        assertThat(finished).isEqualTo(1)
        composeRule.onNodeWithText("Undo").assertDoesNotExist()
    }

    private fun moveTo(state: Lifecycle.State) {
        composeRule.activityRule.scenario.moveToState(state)
        composeRule.waitForIdle()
    }
}
