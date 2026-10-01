package com.naury.chageun.benchmark

import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.Until

internal const val PACKAGE_NAME = "com.naury.chageun"

private const val UI_TIMEOUT_MS = 5_000L

/**
 * A fresh install opens onboarding. Registers a car without a plate so later journeys start on Home.
 * Labels are the English defaults; managed devices run in en-US.
 */
internal fun MacrobenchmarkScope.registerCarIfNeeded() {
    if (!device.wait(Until.hasObject(By.text("Get started")), UI_TIMEOUT_MS)) return
    clickText("Get started")
    clickText("Register without a plate")

    device.wait(Until.hasObject(By.text("Tell us about your car")), UI_TIMEOUT_MS)
    val fields = device.findObjects(By.clazz("android.widget.EditText"))
    fields[0].text = "Hyundai"
    fields[1].text = "Avante"
    fields[2].text = "2022"
    clickText("Gasoline")
    clickText("Next")

    device.wait(Until.hasObject(By.text("What does the odometer show?")), UI_TIMEOUT_MS)
    device.findObject(By.clazz("android.widget.EditText")).text = "42000"
    clickText("Next")
    clickText("Next")
    clickText("Maybe later")
    device.wait(Until.hasObject(By.text("Home")), UI_TIMEOUT_MS)
}

internal fun MacrobenchmarkScope.waitForHome() {
    device.wait(Until.hasObject(By.textContains("Avante").pkg(PACKAGE_NAME)), UI_TIMEOUT_MS)
}

internal fun MacrobenchmarkScope.openTab(label: String) {
    clickText(label)
    device.waitForIdle()
}

/** Flings the first scrollable list down and back up. */
internal fun MacrobenchmarkScope.scrollMainList() {
    val list = device.wait(Until.findObject(By.scrollable(true).pkg(PACKAGE_NAME)), UI_TIMEOUT_MS) ?: return
    list.setGestureMargin(device.displayWidth / GESTURE_MARGIN_DIVISOR)
    list.fling(Direction.DOWN)
    device.waitForIdle()
    list.fling(Direction.UP)
    device.waitForIdle()
}

private fun MacrobenchmarkScope.clickText(text: String) {
    device.wait(Until.findObject(By.text(text)), UI_TIMEOUT_MS)?.click()
    device.waitForIdle()
}

private const val GESTURE_MARGIN_DIVISOR = 5
