package com.naury.chageun.feature.history

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
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
import com.naury.chageun.core.uitesting.AppFrame
import com.naury.chageun.core.uitesting.ScreenshotDevices
import com.naury.chageun.core.uitesting.assertAccessibleControls
import com.naury.chageun.core.uitesting.assertNoClippedText
import com.naury.chageun.core.uitesting.captureScreen
import java.time.Instant
import java.time.LocalDate
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h2000dp")
class ReceiptActionsTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val ref = RecordRef(TimelineEventType.Repair, "repair")
    private val detail = RecordDetail.Check(ref, CheckEntry(CheckKind.Repair, LocalDate.of(2026, 10, 12), "Bumper"))
    private val first = attachment("a1", 1)
    private val second = attachment("a2", 2)

    private val scanned = mutableListOf<RecordRef>()
    private val shared = mutableListOf<Pair<RecordDetail, List<Attachment>>>()
    private val edits = AttachmentPhotoEdits(
        onAddEdited = {},
        onScan = { scanned += it },
        onSharePdf = { record, attachments -> shared += record to attachments },
    )

    private fun show(attachments: List<Attachment>, photoEdits: AttachmentPhotoEdits = edits, frame: Boolean = false) =
        composeRule.setContent {
            val content = @androidx.compose.runtime.Composable {
                HistoryScreen(
                    uiState = HistoryUiState(
                        isLoading = false,
                        selected = ref,
                        detail = detail,
                        attachments = attachments,
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
            if (frame) AppFrame { content() } else ChageunTheme { content() }
        }

    @Test
    fun scanReceipt_opensCameraFlowForThisRecord() {
        show(emptyList())

        composeRule.onNodeWithText("Scan receipt").performScrollTo().performClick()

        assertThat(scanned).containsExactly(ref)
    }

    @Test
    fun sharePdf_isHidden_untilThereIsAPhoto() {
        show(emptyList())

        composeRule.onNodeWithText("Share receipts as PDF").assertDoesNotExist()
    }

    @Test
    fun sharePdf_sendsAttachmentsInTheirOrder() {
        show(listOf(first, second))

        composeRule.onNodeWithText("Share receipts as PDF").performScrollTo().performClick()

        assertThat(shared).containsExactly(detail to listOf(first, second))
    }

    @Test
    fun withoutReceiptEditor_noReceiptButtons() {
        show(listOf(first), AttachmentPhotoEdits())

        composeRule.onNodeWithText("Scan receipt").assertDoesNotExist()
        composeRule.onNodeWithText("Share receipts as PDF").assertDoesNotExist()
    }

    @Test
    fun fullRecord_canStillSharePdf_butNotScanMore() {
        show(List(10) { attachment("a$it", it.toLong()) })

        composeRule.onNodeWithText("Scan receipt").performScrollTo().assertIsNotEnabled()
        composeRule.onNodeWithText("Share receipts as PDF").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun receiptButtons_haveLabelsAndLargeTouchTargets() {
        show(listOf(first))

        composeRule.onNodeWithText("Scan receipt").performScrollTo()
        composeRule.assertAccessibleControls()
    }

    @Test
    @Config(fontScale = 2f)
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun largeFont_keepsReceiptButtonsReadable() {
        show(listOf(first))

        composeRule.onNodeWithText("Share receipts as PDF").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Scan receipt").performScrollTo().assertIsDisplayed()
        composeRule.assertNoClippedText()
    }

    @Test
    @Config(qualifiers = ScreenshotDevices.PHONE_KO)
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun screenshot_attachmentActionsKo() {
        show(listOf(first, second), frame = true)
        composeRule.onNodeWithText("영수증 스캔").performScrollTo()
        composeRule.captureScreen("history_detail_receipts_ko")
    }

    @Test
    @Config(qualifiers = "ko-w360dp-h800dp-night-hdpi")
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun screenshot_attachmentActionsKoDark() {
        show(listOf(first, second), frame = true)
        composeRule.onNodeWithText("영수증 스캔").performScrollTo()
        composeRule.captureScreen("history_detail_receipts_ko_dark")
    }

    private fun attachment(id: String, seconds: Long) =
        Attachment(id, ref, "/missing/$id.jpg", "/missing/${id}_thumb.jpg", Instant.ofEpochSecond(seconds))
}
