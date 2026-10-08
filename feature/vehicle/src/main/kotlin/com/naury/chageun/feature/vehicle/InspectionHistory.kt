package com.naury.chageun.feature.vehicle

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import com.naury.chageun.core.designsystem.component.StatusBadge
import com.naury.chageun.core.designsystem.theme.ChageunTheme
import com.naury.chageun.core.model.InspectionRecord
import com.naury.chageun.core.ui.formatDate
import com.naury.chageun.core.ui.formatNumber
import com.naury.chageun.core.ui.labelRes
import com.naury.chageun.core.ui.tone

/** 가장 최근 정기검사를 보여 주고, 그 전 검사는 펼쳐서 본다. [history]는 최근 것부터 온다. */
@Composable
internal fun InspectionHistory(history: List<InspectionRecord>) {
    val latest = history.firstOrNull() ?: return
    val past = history.drop(1)
    var isExpanded by rememberSaveable { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(ChageunTheme.spacing.xs)) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Text(
            stringResource(R.string.vehicle_inspection_latest),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        InspectionRecordRow(latest, emphasized = true)
        if (past.isEmpty()) return@Column
        TextButton(onClick = { isExpanded = !isExpanded }) {
            Text(
                if (isExpanded) {
                    stringResource(R.string.vehicle_inspection_past_hide)
                } else {
                    pluralStringResource(R.plurals.vehicle_inspection_past_show, past.size, past.size)
                },
            )
            Icon(
                if (isExpanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                contentDescription = null,
            )
        }
        if (isExpanded) past.forEach { InspectionRecordRow(it, emphasized = false) }
    }
}

@Composable
private fun InspectionRecordRow(record: InspectionRecord, emphasized: Boolean) {
    val summary = listOfNotNull(
        formatDate(record.date),
        record.mileage?.let { stringResource(R.string.vehicle_mileage, formatNumber(it.value)) },
    ).joinToString(" · ")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {},
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(ChageunTheme.spacing.sm),
    ) {
        Text(
            summary,
            style = if (emphasized) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )
        StatusBadge(record.result.tone, stringResource(record.result.labelRes))
    }
}
