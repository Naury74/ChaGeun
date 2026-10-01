package com.naury.chageun.ui

import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h800dp")
class ChageunAppTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun navItem(label: String): SemanticsNodeInteraction = composeRule.onNode(hasText(label) and isSelectable())

    @Test
    fun showsAllTopLevelDestinations() {
        composeRule.setContent { ChageunApp() }

        listOf("Home", "Care", "History", "My car").forEach { navItem(it).assertExists() }
        navItem("Home").assertIsSelected()
    }

    @Test
    fun keepsSelectedDestination_afterRecreation() {
        val restorationTester = StateRestorationTester(composeRule)
        restorationTester.setContent { ChageunApp() }

        navItem("History").performClick()
        restorationTester.emulateSavedInstanceStateRestore()

        navItem("History").assertIsSelected()
    }
}
