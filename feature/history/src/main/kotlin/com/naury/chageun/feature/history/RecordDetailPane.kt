package com.naury.chageun.feature.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.FactCheck
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.naury.chageun.core.designsystem.theme.ChageunTheme
import com.naury.chageun.core.model.Attachment
import com.naury.chageun.core.model.FuelField
import com.naury.chageun.core.model.MileageSource
import com.naury.chageun.core.model.RecordDetail
import com.naury.chageun.core.model.RecordRef
import com.naury.chageun.core.model.RecordTimestamps
import com.naury.chageun.core.model.TimelineEventType
import com.naury.chageun.core.ui.CardGroup
import com.naury.chageun.core.ui.ChageunDialog
import com.naury.chageun.core.ui.DialogAction
import com.naury.chageun.core.ui.DialogActionStyle
import com.naury.chageun.core.ui.GroupDivider
import com.naury.chageun.core.ui.ItemIconBadge
import com.naury.chageun.core.ui.LabeledListRow
import com.naury.chageun.core.ui.ListRow
import com.naury.chageun.core.ui.MaintenanceItemIcon
import com.naury.chageun.core.ui.formatDate
import com.naury.chageun.core.ui.formatDateTime
import com.naury.chageun.core.ui.formatLitres
import com.naury.chageun.core.ui.formatNumber
import com.naury.chageun.core.ui.icon
import com.naury.chageun.core.ui.labelRes
import com.naury.chageun.core.ui.tone
import com.naury.chageun.feature.history.form.labelRes
import com.naury.kinetica.ui.KButton
import com.naury.kinetica.ui.KButtonSize
import com.naury.kinetica.ui.KIconButton
import com.naury.kinetica.ui.KTone
import com.naury.kinetica.ui.KVariant

@Composable
internal fun RecordDetailPane(
    detail: RecordDetail,
    attachments: AttachmentsState,
    onBack: (() -> Unit)?,
    onDelete: (RecordRef) -> Unit,
    modifier: Modifier = Modifier,
    onEdit: (RecordDetail) -> Unit = {},
    onAskAi: ((RecordRef) -> Unit)? = null,
    sounds: SoundsState? = null,
) {
    var isConfirmingDelete by rememberSaveable { mutableStateOf(false) }
    // 이 기록을 보는 동안만 소리를 재생한다. 다른 기록을 열거나 상세를 닫으면 멈추고 플레이어를 놓는다.
    val soundPlayer = rememberSoundPlayer()
    val shownSounds = sounds?.takeIf { it.isShownFor(detail.ref) }
    // 주행거리 기록은 값 하나뿐이라 고치기·사진·AI 질문 없이 확인과 삭제만 둔다.
    val isMileage = detail is RecordDetail.Mileage
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = ChageunTheme.spacing.gutter, vertical = ChageunTheme.spacing.sm),
        verticalArrangement = Arrangement.spacedBy(ChageunTheme.spacing.xs),
    ) {
        item(key = "header") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                onBack?.let {
                    KIconButton(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.history_back),
                        onClick = it,
                        variant = KVariant.Text,
                        tone = KTone.Neutral,
                    )
                }
                DetailIcon(detail)
                Text(
                    detailTitle(detail),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier
                        .padding(start = ChageunTheme.spacing.sm)
                        .semantics { heading() },
                )
            }
        }
        item(key = "rows") {
            CardGroup(null, Modifier.padding(top = ChageunTheme.spacing.xs)) {
                detailRows(detail).forEachIndexed { index, (icon, label, value) ->
                    if (index > 0) GroupDivider()
                    LabeledListRow(icon, label, value)
                }
            }
        }
        detail.timestamps?.let { times ->
            item(key = "timestamps") { RecordTimes(times) }
        }
        // 이 기록만 질문 대상으로 넣어 AI 화면을 연다. 메모·정비소 이름은 공유하지 않는다.
        onAskAi?.takeUnless { isMileage }?.let { ask ->
            item(key = "ask-ai") {
                CardGroup(null, Modifier.padding(top = ChageunTheme.spacing.xs)) {
                    ListRow(
                        icon = Icons.Filled.AutoAwesome,
                        title = stringResource(R.string.history_ask_ai),
                        body = stringResource(R.string.history_ask_ai_body),
                        tone = ChageunTheme.colors.ai,
                        onClick = { ask(detail.ref) },
                        trailing = {
                            Icon(
                                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        },
                    )
                }
            }
        }
        if (!isMileage) {
            item(key = "attachments") {
                AttachmentSection(
                    attachments = attachments.items,
                    failedCount = attachments.failedCount,
                    onAdd = { attachments.onAdd(detail.ref) },
                    onDelete = attachments.onDelete,
                    onDismissFailure = attachments.onDismissFailure,
                    onEdit = attachments.onEdit,
                    onAddEdited = attachments.onAddEdited?.let { add -> { add(detail.ref) } },
                    onScan = attachments.onScan?.let { scan -> { scan(detail.ref) } },
                    // 사진이 하나도 없으면 묶을 것이 없으므로 버튼을 두지 않는다.
                    onSharePdf = attachments.onSharePdf
                        ?.takeIf { attachments.items.isNotEmpty() }
                        ?.let { share -> { share(detail, attachments.items) } },
                )
            }
        }
        shownSounds?.let { state ->
            item(key = "sounds") { SoundSection(state, detail.ref, soundPlayer) }
        }
        item(key = "actions") {
            ButtonPair(Modifier.padding(top = ChageunTheme.spacing.sm)) {
                if (!isMileage) {
                    KButton(
                        stringResource(R.string.history_edit),
                        onClick = { onEdit(detail) },
                        variant = KVariant.Tonal,
                        size = KButtonSize.Large,
                        icon = Icons.Filled.Edit,
                        fullWidth = true,
                    )
                }
                KButton(
                    stringResource(R.string.history_delete),
                    onClick = { isConfirmingDelete = true },
                    variant = KVariant.Outlined,
                    tone = KTone.Destructive,
                    size = KButtonSize.Large,
                    icon = Icons.Filled.DeleteOutline,
                    fullWidth = true,
                )
            }
        }
    }
    if (isConfirmingDelete) {
        DeleteRecordDialog(
            onConfirm = {
                isConfirmingDelete = false
                onDelete(detail.ref)
            },
            onDismiss = { isConfirmingDelete = false },
        )
    }
}

/** 기록 삭제 확인. 상세의 삭제 버튼과 목록에서 길게 눌러 고른 삭제가 같은 창을 쓴다. */
@Composable
internal fun DeleteRecordDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    ChageunDialog(
        title = stringResource(R.string.history_delete_title),
        message = stringResource(R.string.history_delete_body),
        icon = Icons.Filled.DeleteOutline,
        tone = ChageunTheme.colors.critical,
        onDismiss = onDismiss,
        actions = listOf(
            DialogAction(stringResource(R.string.history_delete), DialogActionStyle.Destructive, onClick = onConfirm),
            DialogAction(stringResource(R.string.history_cancel), DialogActionStyle.Secondary, onClick = onDismiss),
        ),
    )
}

internal data class AttachmentsState(
    val items: List<Attachment>,
    val failedCount: Int,
    val onAdd: (RecordRef) -> Unit,
    val onDelete: (String) -> Unit,
    val onDismissFailure: () -> Unit,
    /** null이면 첨부한 사진에 편집 버튼을 두지 않는다. */
    val onEdit: ((Attachment) -> Unit)? = null,
    /** null이면 "편집해서 추가" 버튼을 두지 않는다. */
    val onAddEdited: ((RecordRef) -> Unit)? = null,
    /** null이면 "영수증 스캔" 버튼을 두지 않는다. */
    val onScan: ((RecordRef) -> Unit)? = null,
    /** null이면 "영수증 PDF로 공유" 버튼을 두지 않는다. */
    val onSharePdf: ((RecordDetail, List<Attachment>) -> Unit)? = null,
)

@Composable
private fun DetailIcon(detail: RecordDetail) {
    if (detail is RecordDetail.Maintenance) {
        MaintenanceItemIcon(detail.item, size = HEADER_ICON_SIZE)
    } else {
        ItemIconBadge(detail.ref.type.icon, detail.ref.type.tone(), size = HEADER_ICON_SIZE)
    }
}

@Composable
private fun detailTitle(detail: RecordDetail): String = when (detail) {
    is RecordDetail.Maintenance -> stringResource(detail.item.labelRes)
    is RecordDetail.Fuel -> stringResource(TimelineEventType.Fuel.labelRes)
    is RecordDetail.Check -> detail.entry.title
    is RecordDetail.Mileage -> stringResource(TimelineEventType.Mileage.labelRes)
}

@Composable
private fun detailRows(detail: RecordDetail): List<Triple<ImageVector, String, String>> {
    val rows = mutableListOf<Triple<ImageVector, String, String?>>()
    fun add(icon: ImageVector, label: String, value: String?) {
        rows += Triple(icon, label, value)
    }
    val date = stringResource(R.string.history_detail_date)
    val mileage = stringResource(R.string.history_detail_mileage)
    val cost = stringResource(R.string.history_detail_cost)
    val memo = stringResource(R.string.history_detail_memo)
    when (detail) {
        is RecordDetail.Maintenance -> {
            val entry = detail.entry
            add(
                Icons.Filled.Event,
                date,
                entry.date?.let { formatDate(it) } ?: stringResource(R.string.history_unknown_date),
            )
            add(
                Icons.Filled.Speed,
                mileage,
                entry.mileage?.let {
                    if (entry.isMileageEstimated) {
                        stringResource(R.string.history_km_estimated, formatNumber(it.value))
                    } else {
                        km(it.value)
                    }
                },
            )
            add(Icons.Filled.Payments, cost, entry.costWon?.let { won(it) })
            add(Icons.Filled.Store, stringResource(R.string.history_detail_shop), entry.shopName)
            add(Icons.AutoMirrored.Filled.Notes, memo, detail.memo)
        }
        is RecordDetail.Fuel -> rows += fuelRows(detail, date, mileage, memo)
        is RecordDetail.Check -> {
            val entry = detail.entry
            add(Icons.Filled.Event, date, formatDate(entry.date))
            add(
                Icons.Filled.Category,
                stringResource(R.string.history_detail_kind),
                stringResource(entry.kind.labelRes),
            )
            add(
                Icons.AutoMirrored.Filled.FactCheck,
                stringResource(R.string.history_detail_periodic_result),
                entry.periodicResult?.let { stringResource(it.labelRes) },
            )
            add(Icons.Filled.Speed, mileage, entry.mileage?.let { km(it.value) })
            add(Icons.Filled.Payments, cost, entry.costWon?.let { won(it) })
            add(Icons.AutoMirrored.Filled.Notes, memo, entry.memo)
        }
        is RecordDetail.Mileage -> {
            add(Icons.Filled.Event, date, formatDate(detail.date))
            add(Icons.Filled.Speed, mileage, km(detail.mileage.value))
        }
    }
    add(
        Icons.Filled.Person,
        stringResource(R.string.history_detail_source),
        stringResource(detail.sourceLabelRes),
    )
    return rows.mapNotNull { (icon, label, value) ->
        value?.takeIf { it.isNotBlank() }?.let { Triple(icon, label, it) }
    }
}

/** 주유 기록은 계산값 표시 등 줄이 많아 따로 만든다. 값이 비면 [detailRows]에서 빠진다. */
@Composable
private fun fuelRows(
    detail: RecordDetail.Fuel,
    date: String,
    mileage: String,
    memo: String,
): List<Triple<ImageVector, String, String?>> {
    val yes = stringResource(R.string.history_yes)
    val no = stringResource(R.string.history_no)
    val rows = mutableListOf<Triple<ImageVector, String, String?>>()
    fun add(icon: ImageVector, label: String, value: String?) {
        rows += Triple(icon, label, value)
    }
    val entry = detail.entry
    val amounts = entry.amounts
    add(Icons.Filled.Event, date, formatDate(entry.date))
    add(Icons.Filled.Speed, mileage, km(entry.mileage.value))
    add(
        Icons.Filled.Payments,
        stringResource(R.string.history_detail_total),
        computed(won(amounts.totalPriceWon), amounts.computedField == FuelField.Total),
    )
    add(
        Icons.Filled.WaterDrop,
        stringResource(R.string.history_detail_volume),
        computed(
            stringResource(R.string.history_liters, formatLitres(amounts.volumeMl)),
            amounts.computedField == FuelField.Volume,
        ),
    )
    add(
        Icons.Filled.Sell,
        stringResource(R.string.history_detail_unit_price),
        computed(
            stringResource(R.string.history_unit_price, formatNumber(amounts.unitPriceWon)),
            amounts.computedField == FuelField.UnitPrice,
        ),
    )
    add(
        Icons.Filled.LocalGasStation,
        stringResource(R.string.history_detail_full_tank),
        if (entry.isFullTank) yes else no,
    )
    add(Icons.Filled.Place, stringResource(R.string.history_detail_station), entry.stationName)
    add(Icons.AutoMirrored.Filled.Notes, memo, entry.memo)
    return rows
}

private val RecordDetail.sourceLabelRes: Int
    get() = when {
        (this as? RecordDetail.Maintenance)?.entry?.isMileageEstimated == true -> R.string.history_source_estimated
        (this as? RecordDetail.Mileage)?.source == MileageSource.Correction -> R.string.history_source_correction
        else -> R.string.history_source_user
    }

@Composable
private fun km(value: Long) = stringResource(R.string.history_km, formatNumber(value))

@Composable
private fun won(value: Long) = stringResource(R.string.history_won, formatNumber(value))

@Composable
private fun computed(value: String, isComputed: Boolean) =
    if (isComputed) stringResource(R.string.history_detail_computed, value) else value

@Composable
internal fun DetailPlaceholder(modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxSize()
            .padding(ChageunTheme.spacing.xl),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            stringResource(R.string.history_select_record),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

private val HEADER_ICON_SIZE = 52.dp

@Composable
private fun RecordTimes(times: RecordTimestamps) {
    val created = stringResource(R.string.history_detail_created, formatDateTime(times.createdAt))
    val text = times.editedAt?.let {
        stringResource(
            R.string.history_detail_times,
            created,
            stringResource(R.string.history_detail_edited, formatDateTime(it)),
        )
    } ?: created
    Text(
        text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = ChageunTheme.spacing.md),
    )
}
