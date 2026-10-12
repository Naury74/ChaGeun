package com.naury.chageun.core.domain.weather

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class RenamedRegionCodesTest {

    @Test
    fun followsChains_andLeavesUnchangedCodes() {
        val renamed = mapOf("1111051500" to "1111051600", "1111051600" to "1111051700")

        assertThat(RenamedRegionCodes.follow("1111051500", renamed)).isEqualTo("1111051700")
        assertThat(RenamedRegionCodes.follow("1111051600", renamed)).isEqualTo("1111051700")
        assertThat(RenamedRegionCodes.follow("1168064000", renamed)).isEqualTo("1168064000")
    }

    @Test
    fun stopsOnCycle() {
        val renamed = mapOf("1111051500" to "1111051600", "1111051600" to "1111051500")

        assertThat(RenamedRegionCodes.follow("1111051500", renamed)).isAnyOf("1111051500", "1111051600")
    }
}
