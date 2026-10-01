package com.naury.chageun.core.uitesting

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h640dp", fontScale = 2f)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class LayoutAssertionsTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun wrappingText_passes() {
        composeRule.setContent { Text(LONG_TEXT) }

        composeRule.assertNoClippedText()
    }

    @Test
    fun ellipsizedText_fails() {
        composeRule.setContent { Text(LONG_TEXT, maxLines = 1, overflow = TextOverflow.Ellipsis) }

        val error = assertThrows(IllegalStateException::class.java) { composeRule.assertNoClippedText() }
        assertThat(error).hasMessageThat().startsWith("truncated")
    }

    @Test
    fun fixedHeightText_fails() {
        composeRule.setContent { Text(LONG_TEXT, modifier = Modifier.height(20.dp)) }

        assertThrows(IllegalStateException::class.java) { composeRule.assertNoClippedText() }
    }

    @Test
    fun textPushedPastWindowEdge_fails() {
        composeRule.setContent {
            Row {
                Text("Fixed", modifier = Modifier.width(340.dp))
                Text("Pushed out", softWrap = false)
            }
        }

        val error = assertThrows(IllegalStateException::class.java) { composeRule.assertNoClippedText() }
        assertThat(error).hasMessageThat().contains("Pushed out")
    }

    private companion object {
        const val LONG_TEXT = "Engine oil and filter replacement is coming up within the next few weeks"
    }
}
