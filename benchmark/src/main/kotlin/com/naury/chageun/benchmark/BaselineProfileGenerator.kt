package com.naury.chageun.benchmark

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** `./gradlew :app:generateBaselineProfile`로 실행한다. 결과는 app/src/release/generated에 생성된다. */
@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {

    @get:Rule
    val rule = BaselineProfileRule()

    @Test
    fun generate() = rule.collect(packageName = PACKAGE_NAME, includeInStartupProfile = true) {
        pressHome()
        startActivityAndWait()
        registerCarIfNeeded()
        waitForHome()
        scrollMainList()
        listOf("Care", "History", "My car", "Home").forEach { tab ->
            openTab(tab)
            scrollMainList()
        }
    }
}
