package com.naury.chageun.core.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.naury.chageun.core.designsystem.motion.ChageunMotion
import com.naury.chageun.core.designsystem.motion.motionSpec
import com.naury.chageun.core.model.MaintenanceRule
import com.naury.chageun.core.model.MaintenanceStatus

/**
 * 주기 중 이미 사용한 비율. 두 기준 중 더 많이 진행된 쪽 값을 쓴다.
 * UI가 잘못된 막대를 그리지 않도록 두 기준 모두 평가할 수 없으면 null을 반환한다.
 */
fun usedFraction(status: MaintenanceStatus, rule: MaintenanceRule?): Float? {
    val byDistance = rule?.intervalKm?.let { interval -> status.remainingKm?.let { 1f - it.toFloat() / interval } }
    val byTime = rule?.intervalMonths?.let { months ->
        status.remainingDays?.let { 1f - it.toFloat() / (months * AVERAGE_DAYS_PER_MONTH) }
    }
    return listOfNotNull(byDistance, byTime).maxOrNull()?.coerceIn(0f, 1f)
}

private const val AVERAGE_DAYS_PER_MONTH = 30.44f

/** 주기 중 사용한 비율을 막대로 보여 준다. 값이 바뀌면 부드럽게 채워진다. */
@Composable
fun MaintenanceProgressBar(fraction: Float, color: Color, modifier: Modifier = Modifier) {
    val animated by animateFloatAsState(
        fraction,
        motionSpec(ChageunMotion.MEDIUM_MS * 2),
        label = "maintenance-progress",
    )
    LinearProgressIndicator(
        progress = { animated },
        color = color,
        trackColor = MaterialTheme.colorScheme.surfaceVariant,
        drawStopIndicator = {},
        modifier = modifier
            .fillMaxWidth()
            .height(8.dp),
    )
}
