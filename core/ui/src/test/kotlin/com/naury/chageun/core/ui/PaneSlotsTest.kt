package com.naury.chageun.core.ui

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class PaneSlotsTest {

    @Test
    fun noHinge_splitsByWeightWithGap() {
        val slots = paneSlots(total = 1000, gap = 20, weights = listOf(0.4f, 0.6f), hingeStart = null, hingeEnd = null)

        assertThat(slots).containsExactly(PaneSlot(0, 392), PaneSlot(412, 588)).inOrder()
    }

    @Test
    fun zeroWidthFold_padsBothSidesByHalfTheGap() {
        val slots = paneSlots(total = 1000, gap = 20, weights = listOf(0.4f, 0.6f), hingeStart = 600, hingeEnd = 600)

        assertThat(slots).containsExactly(PaneSlot(0, 590), PaneSlot(610, 390)).inOrder()
    }

    @Test
    fun wideHinge_isTheGapItself() {
        val slots = paneSlots(total = 1000, gap = 20, weights = listOf(1f, 1f), hingeStart = 480, hingeEnd = 520)

        assertThat(slots).containsExactly(PaneSlot(0, 480), PaneSlot(520, 480)).inOrder()
    }

    @Test
    fun threePanes_putFirstBeforeHinge_andShareTheRest() {
        val slots = paneSlots(total = 1200, gap = 20, weights = listOf(1f, 1f, 1f), hingeStart = 500, hingeEnd = 500)

        assertThat(slots).containsExactly(PaneSlot(0, 490), PaneSlot(510, 335), PaneSlot(865, 335)).inOrder()
    }

    @Test
    fun hingeOutsideLayout_isIgnored() {
        val slots = paneSlots(total = 600, gap = 20, weights = listOf(1f, 1f), hingeStart = 700, hingeEnd = 700)

        assertThat(slots).containsExactly(PaneSlot(0, 290), PaneSlot(310, 290)).inOrder()
    }
}
