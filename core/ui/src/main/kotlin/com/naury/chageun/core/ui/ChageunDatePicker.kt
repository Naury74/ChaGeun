package com.naury.chageun.core.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerColors
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerState
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.DateRangePickerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.naury.chageun.core.designsystem.theme.ChageunTheme

/**
 * 날짜 하나를 고르는 창. Material 달력을 [ChageunDialog]와 같은 시트·카드 틀 안에 놓는다.
 * 고를 수 있는 날짜는 [state]의 `selectableDates`가 정한다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChageunDatePickerDialog(
    state: DatePickerState,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    title: String = stringResource(R.string.date_picker_title),
    confirmLabel: String = stringResource(R.string.date_picker_confirm),
    cancelLabel: String = stringResource(R.string.date_picker_cancel),
    confirmEnabled: Boolean = state.selectedDateMillis != null,
) {
    PickerFrame(
        title = title,
        onDismiss = onDismiss,
        confirm = DialogAction(confirmLabel, enabled = confirmEnabled, onClick = onConfirm),
        cancelLabel = cancelLabel,
    ) {
        Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())) {
            DatePicker(state = state, title = { PickerTitle(title) }, colors = chageunDatePickerColors())
        }
    }
}

/**
 * 기간을 고르는 창. 달력이 위아래로 스크롤되는 목록이라, 창은 화면 높이를 다 쓰고 달력만 스크롤한다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChageunDateRangePickerDialog(
    state: DateRangePickerState,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    title: String = stringResource(R.string.date_range_picker_title),
    confirmLabel: String = stringResource(R.string.date_picker_confirm),
    cancelLabel: String = stringResource(R.string.date_picker_cancel),
    confirmEnabled: Boolean = state.selectedStartDateMillis != null,
) {
    PickerFrame(
        title = title,
        onDismiss = onDismiss,
        confirm = DialogAction(confirmLabel, enabled = confirmEnabled, onClick = onConfirm),
        cancelLabel = cancelLabel,
    ) {
        DateRangePicker(
            state = state,
            title = { PickerTitle(title) },
            colors = chageunDatePickerColors(),
            modifier = Modifier.weight(1f, fill = false),
        )
    }
}

@Composable
private fun PickerFrame(
    title: String,
    onDismiss: () -> Unit,
    confirm: DialogAction,
    cancelLabel: String,
    picker: @Composable ColumnScope.() -> Unit,
) {
    DialogFrame(
        paneTitle = title,
        onDismiss = onDismiss,
        dismissible = true,
        // Material 달력은 360dp 폭에 맞춰 안쪽 여백을 이미 두므로, 창 여백을 더하면 폰에서 마지막 열이 눌린다.
        horizontalPadding = 0.dp,
    ) {
        // 고른 날짜(headline)가 기본으로는 headlineLarge라 창 제목보다 훨씬 커 보인다. 한 단계 작게 쓴다.
        val typography = MaterialTheme.typography
        MaterialTheme(typography = typography.copy(headlineLarge = typography.headlineSmall)) { picker() }
        Spacer(Modifier.height(ChageunTheme.spacing.md))
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = ChageunTheme.spacing.lg),
            horizontalArrangement = Arrangement.spacedBy(ChageunTheme.spacing.xs),
        ) {
            DialogButton(
                DialogAction(cancelLabel, DialogActionStyle.Secondary, onClick = onDismiss),
                Modifier.weight(1f),
            )
            DialogButton(confirm, Modifier.weight(1f))
        }
    }
}

@Composable
private fun PickerTitle(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier
            .padding(start = TITLE_START, end = TITLE_START, top = ChageunTheme.spacing.xs)
            .semantics { heading() },
    )
}

/** 시트 바탕에 그대로 놓이도록 달력 배경을 없애고, 구분선은 목록과 같은 연한 색을 쓴다. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun chageunDatePickerColors(): DatePickerColors {
    val colors = MaterialTheme.colorScheme
    return DatePickerDefaults.colors(
        containerColor = Color.Transparent,
        titleContentColor = colors.onSurfaceVariant,
        headlineContentColor = colors.onSurface,
        weekdayContentColor = colors.onSurfaceVariant,
        subheadContentColor = colors.onSurfaceVariant,
        navigationContentColor = colors.onSurfaceVariant,
        selectedDayContainerColor = colors.primary,
        selectedDayContentColor = colors.onPrimary,
        todayDateBorderColor = colors.primary,
        todayContentColor = colors.primary,
        selectedYearContainerColor = colors.primary,
        selectedYearContentColor = colors.onPrimary,
        dayInSelectionRangeContainerColor = colors.primaryContainer,
        dayInSelectionRangeContentColor = colors.onPrimaryContainer,
        dividerColor = colors.outlineVariant,
    )
}

private val TITLE_START = 24.dp
