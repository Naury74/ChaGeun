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

    awaitText("Tell us about your car")
    clickText("Hyundai")
    clickText("Avante")
    clickText("2022")
    clickText("Gasoline")
    clickText("Next")

    awaitText("Add a photo of your car")
    clickText("Skip for now")

    awaitText("What does the odometer show?")
    checkNotNull(device.wait(Until.findObject(By.clazz("android.widget.EditText")), UI_TIMEOUT_MS)) {
        "주행거리 입력칸을 찾지 못했다"
    }.text = "42000"
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

private fun MacrobenchmarkScope.awaitText(text: String) {
    check(device.wait(Until.hasObject(By.text(text)), UI_TIMEOUT_MS)) { "'$text' 화면이 나오지 않았다" }
}

/** 작은 화면에서는 버튼이 아래에 가려져 있을 수 있어 세로 목록을 내려 가며 찾는다. */
private fun MacrobenchmarkScope.clickText(text: String) {
    var target = device.wait(Until.findObject(By.text(text)), UI_TIMEOUT_MS)
    repeat(MAX_SCROLLS) {
        if (target != null) return@repeat
        // 연식 칩 같은 가로 목록도 scrollable이므로 가장 높은 것을 화면 목록으로 본다.
        val page = device.findObjects(By.scrollable(true).pkg(PACKAGE_NAME)).maxByOrNull { it.visibleBounds.height() }
        page?.scroll(Direction.DOWN, SCROLL_PERCENT)
        target = device.findObject(By.text(text))
    }
    checkNotNull(target) { "'$text'을(를) 찾지 못했다" }.click()
    device.waitForIdle()
}

private const val GESTURE_MARGIN_DIVISOR = 5
private const val MAX_SCROLLS = 3
private const val SCROLL_PERCENT = 0.8f
