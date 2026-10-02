package com.naury.chageun.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.naury.chageun.core.designsystem.theme.ChageunTheme
import com.naury.chageun.core.designsystem.theme.ToneColors

enum class StatusTone { Good, Upcoming, Critical, Unknown }

val StatusTone.icon: ImageVector
    get() = when (this) {
        StatusTone.Good -> Icons.Filled.CheckCircle
        StatusTone.Upcoming -> Icons.Filled.DateRange
        StatusTone.Critical -> Icons.Filled.Warning
        StatusTone.Unknown -> Icons.Filled.Info
    }

val StatusTone.colors: ToneColors
    @Composable get() = when (this) {
        StatusTone.Good -> ChageunTheme.colors.good
        StatusTone.Upcoming -> ChageunTheme.colors.upcoming
        StatusTone.Critical -> ChageunTheme.colors.critical
        StatusTone.Unknown -> ChageunTheme.colors.unknown
    }

/** 상태는 항상 아이콘과 텍스트를 함께 써서 전달한다. 색 구분 능력에만 의존하지 않기 위해서다. */
@Composable
fun StatusBadge(tone: StatusTone, label: String, modifier: Modifier = Modifier) {
    val colors = tone.colors
    Surface(
        modifier = modifier.clearAndSetSemantics { contentDescription = label },
        shape = MaterialTheme.shapes.small,
        color = colors.container,
        contentColor = colors.content,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(imageVector = tone.icon, contentDescription = null, modifier = Modifier.size(16.dp))
            Text(text = label, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Preview
@Composable
private fun StatusBadgePreview() {
    ChageunTheme {
        Surface {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                StatusTone.entries.forEach { StatusBadge(tone = it, label = it.name) }
            }
        }
    }
}

@Preview
@Composable
private fun StatusBadgeDarkPreview() {
    ChageunTheme(darkTheme = true) {
        Surface {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                StatusTone.entries.forEach { StatusBadge(tone = it, label = it.name) }
            }
        }
    }
}
