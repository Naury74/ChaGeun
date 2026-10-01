package com.naury.chageun.feature.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import com.naury.chageun.core.designsystem.theme.ChageunTheme
import com.naury.chageun.core.model.Attachment
import com.naury.chageun.core.model.FuelField
import com.naury.chageun.core.model.RecordDetail
import com.naury.chageun.core.model.RecordRef
import com.naury.chageun.core.model.TimelineEventType
import com.naury.chageun.core.ui.formatDate
import com.naury.chageun.core.ui.formatLitres
import com.naury.chageun.core.ui.formatNumber
import com.naury.chageun.core.ui.labelRes
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
                Text(
                    detailTitle(detail),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.semantics { heading() },
                )
            }
        }
        item(key = "rows") { Column { detailRows(detail).forEach { (label, value) -> DetailRow(label, value) } } }
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
            OutlinedButton(onClick = { isConfirmingDelete = true }, modifier = Modifier.fillMaxWidth()) {
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
private fun detailTitle(detail: RecordDetail): String = when (detail) {
    is RecordDetail.Maintenance -> stringResource(detail.item.labelRes)
    is RecordDetail.Fuel -> stringResource(TimelineEventType.Fuel.labelRes)
    is RecordDetail.Check -> detail.entry.title
}

@Composable
private fun detailRows(detail: RecordDetail): List<Pair<String, String>> {
    val yes = stringResource(R.string.history_yes)
    val no = stringResource(R.string.history_no)
    val rows = mutableListOf<Pair<String, String?>>()
    when (detail) {
        is RecordDetail.Maintenance -> {
            val entry = detail.entry
            rows += stringResource(R.string.history_detail_date) to
                (entry.date?.let { formatDate(it) } ?: stringResource(R.string.history_unknown_date))
            rows += stringResource(R.string.history_detail_mileage) to entry.mileage?.let { km(it.value) }
            rows += stringResource(R.string.history_detail_cost) to entry.costWon?.let { won(it) }
            rows += stringResource(R.string.history_detail_shop) to entry.shopName
            rows += stringResource(R.string.history_detail_memo) to detail.memo
        }
        is RecordDetail.Fuel -> {
            val entry = detail.entry
            val amounts = entry.amounts
            rows += stringResource(R.string.history_detail_date) to formatDate(entry.date)
            rows += stringResource(R.string.history_detail_mileage) to km(entry.mileage.value)
            rows += stringResource(R.string.history_detail_total) to
                computed(won(amounts.totalPriceWon), amounts.computedField == FuelField.Total)
            rows += stringResource(R.string.history_detail_volume) to
                computed(
                    stringResource(R.string.history_liters, formatLitres(amounts.volumeMl)),
                    amounts.computedField == FuelField.Volume,
                )
            rows += stringResource(R.string.history_detail_unit_price) to computed(
                stringResource(R.string.history_unit_price, formatNumber(amounts.unitPriceWon)),
                amounts.computedField == FuelField.UnitPrice,
            )
            rows += stringResource(R.string.history_detail_full_tank) to if (entry.isFullTank) yes else no
            rows += stringResource(R.string.history_detail_station) to entry.stationName
            rows += stringResource(R.string.history_detail_memo) to entry.memo
        }
        is RecordDetail.Check -> {
            val entry = detail.entry
            rows += stringResource(R.string.history_detail_date) to formatDate(entry.date)
            rows += stringResource(R.string.history_detail_kind) to stringResource(entry.kind.labelRes)
            rows += stringResource(R.string.history_detail_mileage) to entry.mileage?.let { km(it.value) }
            rows += stringResource(R.string.history_detail_cost) to entry.costWon?.let { won(it) }
            rows += stringResource(R.string.history_detail_memo) to entry.memo
        }
    }
    rows += stringResource(R.string.history_detail_source) to stringResource(R.string.history_source_user)
    return rows.mapNotNull { (label, value) -> value?.let { label to it } }
}

@Composable
private fun km(value: Long) = stringResource(R.string.history_km, formatNumber(value))

@Composable
private fun won(value: Long) = stringResource(R.string.history_won, formatNumber(value))

@Composable
private fun computed(value: String, isComputed: Boolean) =
    if (isComputed) stringResource(R.string.history_detail_computed, value) else value

@Composable
private fun DetailRow(label: String, value: String) {
    Column(Modifier.fillMaxWidth()) {
        HorizontalDivider()
        Row(Modifier.padding(vertical = ChageunTheme.spacing.xs)) {
            Text(
                label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            Text(
                value,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.End,
                modifier = Modifier.weight(VALUE_WEIGHT),
            )
        }
    }
}

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

private const val VALUE_WEIGHT = 1.5f
