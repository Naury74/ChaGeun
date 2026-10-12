package com.naury.chageun

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class MainProcessTest {

    @Test
    fun mainProcess_runsAppInitialization() {
        assertThat(isMainProcess("com.naury.chageun", "com.naury.chageun")).isTrue()
    }

    @Test
    fun isolatedProcesses_skipAppInitialization() {
        listOf(":cutout", ":ocr", ":framekit_segmentation", ":framekit_ocr").forEach { suffix ->
            assertThat(isMainProcess("com.naury.chageun$suffix", "com.naury.chageun")).isFalse()
        }
    }

    @Test
    fun unknownProcessName_isTreatedAsMain() {
        assertThat(isMainProcess(null, "com.naury.chageun")).isTrue()
        assertThat(isMainProcess("", "com.naury.chageun")).isTrue()
    }
}
