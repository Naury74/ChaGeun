package com.naury.chageun.feature.vehicle

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.naury.chageun.core.designsystem.theme.ChageunTheme
import com.naury.chageun.core.ui.formatDate
import com.naury.chageun.core.ui.formatNumber

/**
 * 주행거리 기록을 시간에 비례한 가로축 위에 선으로 그린다. 기록이 몇 번 없어도 간격이 실제 기간을 따르므로
 * 오래 입력하지 않은 구간이 그대로 보인다. TalkBack에는 그림 대신 기간·변화·월평균을 한 문장으로 읽어 준다.
 */
@Composable
internal fun MileageTrendCard(trend: MileageTrend, modifier: Modifier = Modifier) {
    val firstKm = formatNumber(trend.first.km)
    val lastKm = formatNumber(trend.last.km)
    val average = trend.monthlyAverageKm?.let {
        stringResource(R.string.vehicle_mileage_trend_average, formatNumber(it))
    }
    val summary = listOfNotNull(
        stringResource(
            R.string.vehicle_mileage_trend_summary,
            formatDate(trend.first.date),
            firstKm,
            formatDate(trend.last.date),
            lastKm,
        ),
        average,
    ).joinToString(" ")
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        modifier = modifier
            .fillMaxWidth()
            .clearAndSetSemantics { contentDescription = summary },
    ) {
        Column(
            Modifier.padding(ChageunTheme.spacing.md),
            verticalArrangement = Arrangement.spacedBy(ChageunTheme.spacing.sm),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(ChageunTheme.spacing.xxs)) {
                Text(
                    stringResource(R.string.vehicle_mileage_trend_title),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                // 월평균을 낼 수 없으면 늘어난 거리를, 그것도 없으면(줄어든 경우) 제목만 둔다.
                val headline = average
                    ?: trend.drivenKm?.let { stringResource(R.string.vehicle_mileage_trend_change, formatNumber(it)) }
                headline?.let {
                    Text(it, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                }
            }
            TrendLine(trend, Modifier.fillMaxWidth().height(CHART_HEIGHT))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                AxisLabel("${formatDate(trend.first.date)}\n$firstKm km")
                AxisLabel("${formatDate(trend.last.date)}\n$lastKm km", end = true)
            }
        }
    }
}

@Composable
private fun AxisLabel(text: String, end: Boolean = false) {
    Text(
        text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = if (end) TextAlign.End else TextAlign.Start,
    )
}

@Composable
private fun TrendLine(trend: MileageTrend, modifier: Modifier) {
    val line = MaterialTheme.colorScheme.primary
    val surface = MaterialTheme.colorScheme.surface
    val grid = MaterialTheme.colorScheme.outlineVariant
    Canvas(modifier) {
        val inset = DOT_RADIUS.toPx() * 2
        val width = size.width - inset * 2
        val height = size.height - inset * 2
        // 값이 거의 같아도 선이 바닥에 붙지 않도록 범위에 여유를 둔다.
        val range = (trend.maxKm - trend.minKm).coerceAtLeast(1)
        fun position(point: MileageTrend.Point) = Offset(
            x = inset + width * trend.fractionOf(point.date),
            y = inset + height * (1f - (point.km - trend.minKm).toFloat() / range),
        )
        val positions = trend.points.map(::position)
        GRID_LINES.forEach { fraction ->
            val y = inset + height * fraction
            drawLine(grid, Offset(inset, y), Offset(inset + width, y), strokeWidth = 1.dp.toPx())
        }
        val path = Path().apply {
            positions.forEachIndexed { index, offset ->
                if (index ==
                    0
                ) {
                    moveTo(offset.x, offset.y)
                } else {
                    lineTo(offset.x, offset.y)
                }
            }
        }
        val fill = Path().apply {
            addPath(path)
            lineTo(positions.last().x, inset + height)
            lineTo(positions.first().x, inset + height)
            close()
        }
        drawPath(fill, Brush.verticalGradient(listOf(line.copy(alpha = FILL_ALPHA), Color.Transparent)))
        drawPath(path, line, style = Stroke(LINE_WIDTH.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        positions.dropLast(1).forEach { drawCircle(line, DOT_RADIUS.toPx() * SMALL_DOT, it) }
        // 가장 최근 값은 바탕색 테두리를 둘러 선 위에서도 눈에 띄게 한다.
        drawCircle(surface, DOT_RADIUS.toPx() * 2, positions.last())
        drawCircle(line, DOT_RADIUS.toPx() * LAST_DOT, positions.last())
    }
}

// 위·가운데·아래에 옅은 기준선을 그어 값의 높낮이를 읽기 쉽게 한다.
private val GRID_LINES = listOf(0f, 0.5f, 1f)
private val CHART_HEIGHT = 120.dp
private val LINE_WIDTH = 2.5.dp
private val DOT_RADIUS = 3.dp
private const val SMALL_DOT = 0.8f
private const val LAST_DOT = 1.4f
private const val FILL_ALPHA = 0.18f
