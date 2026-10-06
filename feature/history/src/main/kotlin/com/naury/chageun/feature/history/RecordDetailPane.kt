package com.naury.chageun.feature.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.naury.chageun.core.model.RecordDetail
import com.naury.chageun.core.model.RecordRef
import com.naury.chageun.core.model.TimelineEventType
import com.naury.chageun.core.ui.CardGroup
import com.naury.chageun.core.ui.GroupDivider
import com.naury.chageun.core.ui.ItemIconBadge
import com.naury.chageun.core.ui.LabeledListRow
import com.naury.chageun.core.ui.MaintenanceItemIcon
import com.naury.chageun.core.ui.formatDate
import com.naury.chageun.core.ui.formatLitres
import com.naury.chageun.core.ui.formatNumber
import com.naury.chageun.core.ui.icon
import com.naury.chageun.core.ui.labelRes
import com.naury.chageun.core.ui.tone
import com.naury.chageun.feature.history.form.labelRes

@Composable
internal fun RecordDetailPane(
    detail: RecordDetail,
    attachments: AttachmentsState,
    onBack: (() -> Unit)?,
    onDelete: (RecordRef) -> Unit,
    modifier: Modifier = Modifier,
) {
    var isConfirmingDelete by rememberSaveable { mutableStateOf(false) }
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = ChageunTheme.spacing.gutter, vertical = ChageunTheme.spacing.sm),
        verticalArrangement = Arrangement.spacedBy(ChageunTheme.spacing.xs),
    ) {
        item(key = "header") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                onBack?.let {
                    IconButton(onClick = it) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.history_back),
                        )
                    }
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
        item(key = "attachments") {
            AttachmentSection(
                attachments = attachments.items,
                failedCount = attachments.failedCount,
                onAttach = { attachments.onAttach(detail.ref, it) },
                onDelete = attachments.onDelete,
                onDismissFailure = attachments.onDismissFailure,
            )
        }
        item(key = "delete") {
            OutlinedButton(
                onClick = { isConfirmingDelete = true },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = ChageunTheme.spacing.sm)
                    .heightIn(min = ChageunTheme.spacing.minTouchTarget),
            ) {
                Icon(Icons.Filled.DeleteOutline, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(ChageunTheme.spacing.xs))
                Text(stringResource(R.string.history_delete))
            }
        }
    }
    if (isConfirmingDelete) {
        AlertDialog(
            onDismissRequest = { isConfirmingDelete = false },
            title = { Text(stringResource(R.string.history_delete_title)) },
            text = { Text(stringResource(R.string.history_delete_body)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        isConfirmingDelete = false
                        onDelete(detail.ref)
                    },
                ) { Text(stringResource(R.string.history_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { isConfirmingDelete = false }) { Text(stringResource(R.string.history_cancel)) }
            },
        )
    }
}

internal data class AttachmentsState(
    val items: List<Attachment>,
    val failedCount: Int,
    val onAttach: (RecordRef, List<String>) -> Unit,
    val onDelete: (String) -> Unit,
    val onDismissFailure: () -> Unit,
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
}

@Composable
private fun detailRows(detail: RecordDetail): List<Triple<ImageVector, String, String>> {
    val yes = stringResource(R.string.history_yes)
    val no = stringResource(R.string.history_no)
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
        is RecordDetail.Fuel -> {
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
        }
        is RecordDetail.Check -> {
            val entry = detail.entry
            add(Icons.Filled.Event, date, formatDate(entry.date))
            add(
                Icons.Filled.Category,
                stringResource(R.string.history_detail_kind),
                stringResource(entry.kind.labelRes),
            )
            add(Icons.Filled.Speed, mileage, entry.mileage?.let { km(it.value) })
            add(Icons.Filled.Payments, cost, entry.costWon?.let { won(it) })
            add(Icons.AutoMirrored.Filled.Notes, memo, entry.memo)
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

private val RecordDetail.sourceLabelRes: Int
    get() = if ((this as? RecordDetail.Maintenance)?.entry?.isMileageEstimated == true) {
        R.string.history_source_estimated
    } else {
        R.string.history_source_user
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
