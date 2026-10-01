package com.naury.chageun.benchmark

import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Frame timing while scrolling the main lists. */
@RunWith(AndroidJUnit4::class)
class ScrollBenchmark {

    @get:Rule
    val rule = MacrobenchmarkRule()

    @Test
    fun homeScroll() = scroll(tab = "Home")

    @Test
    fun careListScroll() = scroll(tab = "Care")

    @Test
    fun timelineScroll() = scroll(tab = "History")

    private fun scroll(tab: String) = rule.measureRepeated(
        packageName = PACKAGE_NAME,
        metrics = listOf(FrameTimingMetric()),
        compilationMode = CompilationMode.Partial(),
        startupMode = StartupMode.WARM,
        iterations = ITERATIONS,
        setupBlock = {
            pressHome()
            startActivityAndWait()
            registerCarIfNeeded()
            waitForHome()
            openTab(tab)
        },
    ) {
        scrollMainList()
    }

    private companion object {
        const val ITERATIONS = 5
    }
}
