package com.naury.chageun.feature.home

import android.text.format.DateFormat
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.naury.chageun.core.designsystem.theme.ChageunTheme
import com.naury.chageun.core.model.AuthUser
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/** 인사말을 고르는 시간대. 새벽 5시부터 아침으로 본다. */
enum class DayPart {
    Morning,
    Afternoon,
    Evening,
    Night,
    ;

    companion object {
        fun of(time: LocalTime): DayPart = when (time.hour) {
            in MORNING_START until AFTERNOON_START -> Morning
            in AFTERNOON_START until EVENING_START -> Afternoon
            in EVENING_START until NIGHT_START -> Evening
            else -> Night
        }

        private const val MORNING_START = 5
        private const val AFTERNOON_START = 12
        private const val EVENING_START = 18
        private const val NIGHT_START = 22
    }
}

/**
 * 인사말에 부를 이름. Google 계정은 표시 이름, 이메일 계정은 @ 앞부분을 쓴다.
 * 인증을 마치지 않은 계정은 아직 가입 전이라 이름을 부르지 않는다.
 */
fun AuthUser.greetingName(): String? {
    if (needsEmailVerification) return null
    return displayName?.trim()?.takeIf { it.isNotEmpty() }
        ?: email?.substringBefore('@')?.takeIf { it.isNotEmpty() }
}

/** 홈 맨 위. 브랜드, 시간대 인사말, 오늘 날짜와 계정·설정 버튼을 둔다. */
@Composable
internal fun HomeGreetingBar(
    name: String?,
    dayPart: DayPart,
    today: LocalDate,
    onOpenAccount: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = ChageunTheme.spacing.gutter, end = ChageunTheme.spacing.xs, top = ChageunTheme.spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                stringResource(R.string.home_brand),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                greeting(name, dayPart),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                todayLabel(today),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(onClick = onOpenAccount) {
            if (name != null) {
                Initial(name)
            } else {
                Icon(
                    Icons.Filled.AccountCircle,
                    contentDescription = stringResource(R.string.home_open_account),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(AVATAR_SIZE),
                )
            }
        }
        IconButton(onClick = onOpenSettings) {
            Icon(Icons.Filled.Settings, contentDescription = stringResource(R.string.home_open_settings))
        }
    }
}

/** 로그인한 사용자의 이름 첫 글자. */
@Composable
private fun Initial(name: String) {
    val description = stringResource(R.string.home_open_account_named, name)
    Box(
        Modifier
            .size(AVATAR_SIZE)
            .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            name.take(1).uppercase(),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    }
}

@Composable
private fun greeting(name: String?, dayPart: DayPart): String {
    val (plain, named) = when (dayPart) {
        DayPart.Morning -> R.string.home_greeting_morning to R.string.home_greeting_morning_named
        DayPart.Afternoon -> R.string.home_greeting_afternoon to R.string.home_greeting_afternoon_named
        DayPart.Evening -> R.string.home_greeting_evening to R.string.home_greeting_evening_named
        DayPart.Night -> R.string.home_greeting_night to R.string.home_greeting_night_named
    }
    return if (name == null) stringResource(plain) else stringResource(named, name)
}

/** "10월 6일 화요일"처럼 지역 표기에 맞춘 날짜. */
@Composable
private fun todayLabel(today: LocalDate): String {
    val locale = LocalConfiguration.current.locales[0]
    return remember(today, locale) {
        today.format(DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(locale, "MMMMEEEEd"), locale))
    }
}

private val AVATAR_SIZE = 32.dp
