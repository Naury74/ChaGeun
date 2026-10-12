package com.naury.chageun.core.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.performTextInput
import com.google.common.truth.Truth.assertThat
import com.naury.chageun.core.designsystem.theme.ChageunTheme
import com.naury.chageun.core.uitesting.assertAccessibleControls
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ChageunTextFieldTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun typing_updatesValue_andErrorIsAnnouncedWithItsReason() {
        var text by mutableStateOf("")
        composeRule.setContent {
            ChageunTheme {
                ChageunTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = "Shop",
                    isError = text.length > 3,
                    supportingText = "Too long".takeIf { text.length > 3 },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        composeRule.onNode(hasSetTextAction()).performTextInput("Speedy")

        assertThat(text).isEqualTo("Speedy")
        composeRule.onNode(hasSetTextAction())
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Error, "Too long"))
        composeRule.assertAccessibleControls()
    }
}
