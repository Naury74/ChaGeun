package com.naury.chageun.core.uitesting

import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasSetTextAction

/**
 * 누를 수 있는 요소와 입력 칸마다 TalkBack이 읽을 이름이 있고, 터치 영역이 48dp 이상인지 확인한다(기획 §28).
 * Robolectric에서는 ATF 검사가 문제를 찾지 못하므로 semantics 정보로 직접 본다.
 * Compose된 노드만 검사하므로 lazy 목록은 화면에 보이는 부분만 확인된다.
 */
fun SemanticsNodeInteractionsProvider.assertAccessibleControls() {
    val problems = onAllNodes(hasClickAction() or hasSetTextAction())
        .fetchSemanticsNodes()
        .mapNotNull { node ->
            val minPx = MIN_TOUCH_TARGET_DP * node.layoutInfo.density.density - SIZE_TOLERANCE_PX
            val touch = node.touchBoundsInRoot
            when {
                node.spokenLabel().isBlank() -> "no label: node #${node.id} at ${node.boundsInRoot}"
                touch.width < minPx || touch.height < minPx ->
                    "touch target ${touch.width.toInt()}x${touch.height.toInt()}px < 48dp: ${node.spokenLabel()}"
                else -> null
            }
        }
    check(problems.isEmpty()) { problems.joinToString(separator = "\n") }
}

private fun SemanticsNode.spokenLabel(): String = listOfNotNull(
    config.getOrNull(SemanticsProperties.ContentDescription)?.joinToString(),
    config.getOrNull(SemanticsProperties.Text)?.joinToString(),
    config.getOrNull(SemanticsProperties.EditableText)?.text,
).joinToString(" ").trim()

private const val MIN_TOUCH_TARGET_DP = 48f
private const val SIZE_TOLERANCE_PX = 1f
