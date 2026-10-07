package com.naury.chageun.benchmark

import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.ExperimentalMetricApi
import androidx.benchmark.macro.FrameTimingGfxInfoMetric
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 주요 목록을 스크롤할 때의 프레임 타이밍.
 * 야간 테스트의 ATD 에뮬레이터는 프레임 타임라인을 trace에 남기지 않아 FrameTimingMetric이 실패하므로
 * dumpsys gfxinfo 기반 지표를 쓴다.
 */
@OptIn(ExperimentalMetricApi::class)
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
        metrics = listOf(FrameTimingGfxInfoMetric()),
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
