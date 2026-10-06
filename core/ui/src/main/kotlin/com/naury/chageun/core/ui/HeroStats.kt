package com.naury.chageun.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.naury.chageun.core.model.InspectionState
import com.naury.chageun.core.model.InspectionStatus

/** 홈·내 차 Hero 요약 카드에 함께 쓰는 칸. 같은 값이 두 화면에서 같은 모양으로 보이게 한다. */
@Composable
fun mileageHeroStat(mileageKm: Long?): HeroStat = HeroStat(
    label = stringResource(R.string.hero_stat_mileage),
    value = mileageKm?.let { formatNumber(it) } ?: stringResource(R.string.hero_stat_unknown),
    unit = mileageKm?.let { "km" },
)

@Composable
fun inspectionHeroStat(inspection: InspectionStatus): HeroStat {
    val daysLeft = inspection.daysLeft
    val value = when {
        inspection.state == InspectionState.Unknown || daysLeft == null -> stringResource(
            R.string.hero_inspection_unset,
        )
        daysLeft < 0 -> stringResource(R.string.hero_inspection_overdue)
        daysLeft == 0L -> stringResource(R.string.hero_inspection_today)
        else -> stringResource(R.string.hero_inspection_days_left, daysLeft)
    }
    return HeroStat(label = stringResource(R.string.hero_stat_inspection), value = value)
}
