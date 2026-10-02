package com.naury.chageun.benchmark

import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.StaleObjectException
import androidx.test.uiautomator.Until

internal const val PACKAGE_NAME = "com.naury.chageun"

private const val UI_TIMEOUT_MS = 5_000L

/**
 * 새로 설치하면 온보딩이 열린다. 번호판 없이 차량을 등록해 이후 journey가 Home에서 시작하게 한다.
 * 라벨은 영어 기본값을 쓴다. managed device는 en-US로 실행된다.
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

/** 첫 번째 스크롤 가능한 목록을 아래로 fling한 뒤 다시 위로 올린다. */
internal fun MacrobenchmarkScope.scrollMainList() {
    listOf(Direction.DOWN, Direction.UP).forEach { direction ->
        // 스크롤 중 목록이 다시 그려지면 이전 UiObject2는 stale이 되므로 방향마다 새로 찾는다.
        val list = device.wait(Until.findObject(By.scrollable(true).pkg(PACKAGE_NAME)), UI_TIMEOUT_MS) ?: return
        list.setGestureMargin(device.displayWidth / GESTURE_MARGIN_DIVISOR)
        try {
            list.fling(direction)
        } catch (_: StaleObjectException) {
            return@forEach
        }
        device.waitForIdle()
    }
}

private fun MacrobenchmarkScope.clickText(text: String) {
    device.wait(Until.findObject(By.text(text)), UI_TIMEOUT_MS)?.click()
    device.waitForIdle()
}

private const val GESTURE_MARGIN_DIVISOR = 5
