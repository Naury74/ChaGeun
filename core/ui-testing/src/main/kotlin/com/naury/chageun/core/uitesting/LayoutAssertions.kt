package com.naury.chageun.core.uitesting

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.text.TextLayoutResult

/**
 * Compose된 텍스트가 하나라도 잘리면 실패한다. `maxLines` 때문에 줄이 빠지거나, 고정 높이에 잘리거나,
 * 창 오른쪽 끝 밖으로 밀려난 경우다. `@Config(fontScale = 2f)`와 `@GraphicsMode(NATIVE)`로 실행해야 한다.
 * 기존 Robolectric 그래픽 모드는 텍스트를 측정하지 않는다.
 * Compose된 노드만 검사하므로 화면 밖 항목은 lazy 목록을 스크롤한 뒤 다시 호출한다.
 */
fun SemanticsNodeInteractionsProvider.assertNoClippedText() {
    val windowRight = onAllNodes(isRoot()).fetchSemanticsNodes().maxOf { it.boundsInRoot.right }
    val problems = onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsActions.GetTextLayoutResult))
        .fetchSemanticsNodes()
        .mapNotNull { node ->
            when {
                node.textLayout()?.isTruncated() == true -> "truncated: ${node.label()}"
                node.boundsInRoot.right > windowRight + EDGE_TOLERANCE_PX -> "past window edge: ${node.label()}"
                else -> null
            }
        }
    check(problems.isEmpty()) { problems.joinToString(separator = "\n") }
}

private fun SemanticsNode.textLayout(): TextLayoutResult? {
    val results = mutableListOf<TextLayoutResult>()
    config.getOrNull(SemanticsActions.GetTextLayoutResult)?.action?.invoke(results)
    return results.firstOrNull()
}

/** 가로 넘침은 검사하지 않는다. semantics로 가져온 레이아웃은 들어맞는 텍스트도 넘친다고 보고하기 때문이다. */
private fun TextLayoutResult.isTruncated(): Boolean = multiParagraph.didExceedMaxLines || didOverflowHeight

private fun SemanticsNode.label(): String =
    config.getOrNull(SemanticsProperties.Text)?.joinToString().orEmpty().ifEmpty { "node #$id" }

private const val EDGE_TOLERANCE_PX = 1f
