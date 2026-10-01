package com.naury.chageun.data.history

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ImageSizingTest {

    @Test
    fun sampleSize_keepsLongerEdgeAboveTarget() {
        assertThat(ImageSizing.sampleSize(4_000, 3_000, 2_048)).isEqualTo(1)
        assertThat(ImageSizing.sampleSize(8_192, 6_144, 2_048)).isEqualTo(4)
        assertThat(ImageSizing.sampleSize(1_000, 800, 2_048)).isEqualTo(1)
    }

    @Test
    fun fit_scalesDownLongerEdge_andNeverUpscales() {
        assertThat(ImageSizing.fit(4_000, 3_000, 2_048)).isEqualTo(2_048 to 1_536)
        assertThat(ImageSizing.fit(3_000, 4_000, 320)).isEqualTo(240 to 320)
        assertThat(ImageSizing.fit(800, 600, 2_048)).isEqualTo(800 to 600)
    }
}
