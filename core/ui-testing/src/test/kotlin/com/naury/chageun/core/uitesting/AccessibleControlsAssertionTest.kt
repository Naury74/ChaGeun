package com.naury.chageun.core.uitesting

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.dp
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** 접근성 검사 도우미가 실제로 문제를 잡는지 확인한다. 잡지 못하면 화면별 검사가 의미 없다. */
@RunWith(RobolectricTestRunner::class)
class AccessibleControlsAssertionTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun passes_forLabeledIconButton() {
        composeRule.setContent {
            IconButton(onClick = {}) { Icon(Icons.Filled.Settings, contentDescription = "Settings") }
        }

        composeRule.assertAccessibleControls()
    }

    @Test
    fun fails_forUnlabeledOrTinyTarget() {
        composeRule.setContent {
            Column {
                Box(Modifier.size(20.dp).clickable {})
                IconButton(onClick = {}) { Icon(Icons.Filled.Settings, contentDescription = null) }
            }
        }

        val error = runCatching { composeRule.assertAccessibleControls() }.exceptionOrNull()
        assertThat(error).isInstanceOf(IllegalStateException::class.java)
        assertThat(error!!.message).contains("no label")
    }
}
