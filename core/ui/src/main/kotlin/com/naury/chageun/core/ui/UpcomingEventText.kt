package com.naury.chageun.core.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.material.icons.filled.Speed
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.naury.chageun.core.designsystem.component.StatusTone
import com.naury.chageun.core.designsystem.theme.ChageunTheme
import com.naury.chageun.core.designsystem.theme.ToneColors
import com.naury.chageun.core.model.Confidence
import com.naury.chageun.core.model.UpcomingEvent

val UpcomingEvent.icon: ImageVector
    get() = when (this) {
        is UpcomingEvent.MaintenanceDue -> item.icon
        is UpcomingEvent.InspectionDue -> Icons.Filled.FactCheck
        is UpcomingEvent.MileageUpdate -> Icons.Filled.Speed
    }

/** 지난 일은 빨강으로, 나머지는 항목 분류 색을 따른다. */
@Composable
fun UpcomingEvent.tone(): ToneColors = when {
    isOverdue -> ChageunTheme.colors.critical
    this is UpcomingEvent.MaintenanceDue -> item.category.tone()
    this is UpcomingEvent.InspectionDue -> ChageunTheme.colors.ai
    else -> ChageunTheme.colors.unknown
}

@Composable
fun upcomingEventTitle(event: UpcomingEvent): String = when (event) {
    is UpcomingEvent.MaintenanceDue -> stringResource(event.item.labelRes)
    is UpcomingEvent.InspectionDue -> stringResource(R.string.upcoming_inspection)
    is UpcomingEvent.MileageUpdate -> stringResource(R.string.upcoming_mileage)
}

/** 색에만 기대지 않도록 상태를 글자로 붙인다. 지남·예상·확정을 구분한다. */
@Composable
fun upcomingEventStatus(event: UpcomingEvent): Pair<StatusTone, String> = when {
    event.isOverdue -> StatusTone.Critical to stringResource(R.string.upcoming_overdue)
    event is UpcomingEvent.MaintenanceDue && event.isEstimated ->
        StatusTone.Unknown to stringResource(R.string.upcoming_estimated)
    event is UpcomingEvent.MaintenanceDue -> StatusTone.Upcoming to stringResource(R.string.upcoming_due_date)
    event is UpcomingEvent.InspectionDue -> StatusTone.Upcoming to stringResource(R.string.upcoming_inspection_due)
    else -> StatusTone.Good to stringResource(R.string.upcoming_mileage_due)
}

/** 예상 일정이 무엇을 근거로 했는지. 확정 일정에는 없다. */
@Composable
fun upcomingEventConfidence(event: UpcomingEvent): String? {
    val confidence = (event as? UpcomingEvent.MaintenanceDue)?.confidence ?: return null
    return stringResource(
        when (confidence) {
            Confidence.High -> R.string.upcoming_confidence_high
            Confidence.Medium -> R.string.upcoming_confidence_medium
            Confidence.Low -> R.string.upcoming_confidence_low
        },
    )
}
