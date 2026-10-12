package com.naury.chageun.feature.history

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.google.common.truth.Truth.assertThat
import com.naury.chageun.core.designsystem.theme.ChageunTheme
import com.naury.chageun.core.model.Attachment
import com.naury.chageun.core.model.CheckEntry
import com.naury.chageun.core.model.CheckKind
import com.naury.chageun.core.model.RecordDetail
import com.naury.chageun.core.model.RecordRef
import com.naury.chageun.core.model.TimelineEventType
import java.time.Instant
import java.time.LocalDate
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h2000dp")
class AttachmentPhotoEditTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val ref = RecordRef(TimelineEventType.Repair, "repair")
    private val detail = RecordDetail.Check(ref, CheckEntry(CheckKind.Repair, LocalDate.of(2026, 9, 1), "Bumper"))
    private val attachment = Attachment("a1", ref, "/missing/a1.jpg", "/missing/a1_thumb.jpg", Instant.EPOCH)

    private fun show(photoEdits: AttachmentPhotoEdits) = composeRule.setContent {
        ChageunTheme {
            HistoryScreen(
                uiState = HistoryUiState(
                    isLoading = false,
                    selected = ref,
                    detail = detail,
                    attachments = listOf(attachment),
                ),
                isTwoPane = false,
                onKeywordChanged = {},
                onFilterSelected = {},
                onSelect = {},
                onDelete = {},
                onAdd = {},
                onAddPhotos = {},
                onDeleteAttachment = {},
                onDismissAttachFailure = {},
                photoEdits = photoEdits,
            )
        }
    }

    @Test
    fun editThenAttach_opensEditorForThisRecord() {
        val added = mutableListOf<RecordRef>()
        show(AttachmentPhotoEdits(onAddEdited = { added += it }))

        composeRule.onNodeWithText("Edit, then attach").performScrollTo().performClick()

        assertThat(added).containsExactly(ref)
    }

    @Test
    fun attachedPhoto_canBeEdited_fromItsViewer() {
        val edited = mutableListOf<Attachment>()
        show(AttachmentPhotoEdits(onEdit = { edited += it }))

        composeRule.onNode(hasClickLabel("Open photo")).performScrollTo().performClick()
        composeRule.onNode(hasText("Edit") and hasAnyAncestor(isDialog())).performClick()

        assertThat(edited).containsExactly(attachment)
        assertThat(composeRule.onAllNodes(isDialog()).fetchSemanticsNodes()).isEmpty()
    }

    @Test
    fun withoutEditor_noEditButtons() {
        show(AttachmentPhotoEdits())

        composeRule.onNodeWithText("Edit, then attach").assertDoesNotExist()
        composeRule.onNode(hasClickLabel("Open photo")).performScrollTo().performClick()
        composeRule.onNode(hasText("Edit") and hasAnyAncestor(isDialog())).assertDoesNotExist()
    }

    private fun hasClickLabel(label: String) = SemanticsMatcher("onClickLabel = $label") {
        it.config.getOrElseNullable(SemanticsActions.OnClick) { null }?.label == label
    }
}
