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
 * Fails when any composed text is cut off: lines dropped by `maxLines`, clipped by a fixed height, or pushed
 * past the right edge of the window. Run with `@Config(fontScale = 2f)` and `@GraphicsMode(NATIVE)`; the
 * legacy Robolectric graphics mode does not measure text.
 * Only composed nodes are checked, so scroll lazy lists before calling it again for off-screen items.
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

/** Width overflow is not checked: a layout fetched through semantics reports it even for text that fits. */
private fun TextLayoutResult.isTruncated(): Boolean = multiParagraph.didExceedMaxLines || didOverflowHeight

private fun SemanticsNode.label(): String =
    config.getOrNull(SemanticsProperties.Text)?.joinToString().orEmpty().ifEmpty { "node #$id" }

private const val EDGE_TOLERANCE_PX = 1f
