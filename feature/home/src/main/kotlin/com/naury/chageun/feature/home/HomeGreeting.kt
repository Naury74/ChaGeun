package com.naury.chageun.feature.home

import android.text.format.DateFormat
import androidx.annotation.ArrayRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.naury.chageun.core.designsystem.component.ChageunBrandTitle
import com.naury.chageun.core.designsystem.theme.ChageunTheme
import com.naury.chageun.core.domain.ads.AppUsage
import com.naury.chageun.core.domain.greeting.Greeting
import com.naury.chageun.core.domain.greeting.GreetingFacts
import com.naury.chageun.core.domain.greeting.GreetingSituation
import com.naury.chageun.core.model.AuthUser
import com.naury.chageun.core.model.HealthReason
import com.naury.chageun.core.model.InspectionState
import com.naury.chageun.core.model.MaintenanceOverview
import com.naury.chageun.core.model.MaintenanceState
import com.naury.chageun.core.model.TimelineItem
import com.naury.chageun.core.model.Vehicle
import java.text.NumberFormat
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * 인사말에 부를 이름. Google 계정은 표시 이름, 이메일 계정은 @ 앞부분을 쓴다.
 * 인증을 마치지 않은 계정은 아직 가입 전이라 이름을 부르지 않는다.
 */
fun AuthUser.greetingName(): String? {
    if (needsEmailVerification) return null
    return displayName?.trim()?.takeIf { it.isNotEmpty() }
        ?: email?.substringBefore('@')?.takeIf { it.isNotEmpty() }
}

/**
 * 홈 화면 상태에서 인사말 규칙에 넘길 사실을 모은다.
 *
 * 이번 실행은 홈이 뜨기 직전에 기록되므로, 마지막 실행이 방금 전이면 그 전 실행을 지난 방문으로 본다.
 */
internal fun greetingFacts(
    now: LocalDateTime,
    zone: ZoneId,
    vehicle: Vehicle,
    overview: MaintenanceOverview,
    recent: List<TimelineItem>,
    usage: AppUsage,
): GreetingFacts {
    val nowInstant = now.atZone(zone).toInstant()
    val previousVisit = usage.lastLaunchAt?.takeIf { Duration.between(it, nowInstant) > THIS_LAUNCH_WINDOW }
        ?: usage.previousLaunchAt
    return GreetingFacts(
        now = now,
        hasActiveRecall = HealthReason.ActiveSafetyRecall in overview.health.reasons,
        isInspectionOverdue = overview.inspection.state == InspectionState.Overdue,
        overdueItemCount = overview.statuses.count { it.state == MaintenanceState.Overdue },
        isInspectionDueSoon = overview.inspection.state == InspectionState.DueSoon,
        inspectionDaysLeft = overview.inspection.daysLeft,
        lastRecordDate = recent.mapNotNull { it.date }.maxOrNull(),
        mileage = overview.currentMileage,
        firstRegistrationDate = vehicle.firstRegistrationDate,
        isFirstVisit = usage.launchCount <= 1 && usage.previousLaunchAt == null,
        previousVisitDate = previousVisit?.atZone(zone)?.toLocalDate(),
    )
}

private val THIS_LAUNCH_WINDOW: Duration = Duration.ofMinutes(10)

/** 홈 맨 위. 차근 로고, 계정·설정 버튼, 상황별 인사말, 오늘 날짜와 날씨 한 줄을 둔다. */
@Composable
internal fun HomeGreetingBar(
    name: String?,
    greeting: Greeting,
    today: LocalDate,
    weather: HomeWeather,
    onSetUpWeather: () -> Unit,
    onDismissWeatherPrompt: () -> Unit,
    onOpenAccount: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = ChageunTheme.spacing
    Column(modifier.fillMaxWidth().padding(top = spacing.xs)) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(start = spacing.gutter, end = spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // 버튼 자리를 먼저 잡고 남은 폭을 로고에 준다. 큰 글꼴에서도 버튼이 밀려나지 않는다.
            ChageunBrandTitle(stringResource(R.string.home_brand), Modifier.weight(1f))
            AccountButton(name, onOpenAccount)
            IconButton(onClick = onOpenSettings) {
                Icon(Icons.Filled.Settings, contentDescription = stringResource(R.string.home_open_settings))
            }
        }
        Column(Modifier.padding(horizontal = spacing.gutter).padding(top = spacing.xs)) {
            Text(
                greetingText(greeting, name),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.semantics { heading() },
            )
            Text(todayLabel(today), style = MaterialTheme.typography.bodySmall, color = skyVariantText())
            HomeWeatherLine(
                weather = weather,
                seed = greeting.seed,
                onChooseLocation = onSetUpWeather,
                onDismissPrompt = onDismissWeatherPrompt,
                modifier = Modifier.padding(top = spacing.xxs),
            )
        }
    }
}

@Composable
private fun AccountButton(name: String?, onOpenAccount: () -> Unit) {
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
        // 동그라미 크기가 고정이라 첫 글자도 글꼴 크기 설정을 따르지 않게 한다. 읽을 이름은 설명으로 따로 준다.
        val glyphSize = with(LocalDensity.current) { INITIAL_SIZE.toSp() }
        Text(
            name.take(1).uppercase(),
            style = MaterialTheme.typography.titleSmall.copy(fontSize = glyphSize, lineHeight = glyphSize),
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.wrapContentSize(unbounded = true),
        )
    }
}

/**
 * [greeting]의 문구 목록에서 오늘의 문구를 골라 값과 이름을 넣는다.
 * 값이 없으면 값을 넣는 자리가 없는 문구만 쓴다.
 */
@Composable
internal fun greetingText(greeting: Greeting, name: String?): String {
    val locale = LocalConfiguration.current.locales[0]
    val variants = stringArrayResource(greeting.situation.arrayRes).toList()
    val number = greeting.number
    val usable = if (number == null) variants.filterNot { NUMBER_SLOT in it }.ifEmpty { variants } else variants
    val template = usable[greeting.variantIndex(usable.size)]
    val message = if (NUMBER_SLOT in template && number != null) {
        String.format(locale, template, NumberFormat.getIntegerInstance(locale).format(number))
    } else {
        template
    }
    return if (name == null) message else stringResource(R.string.home_greeting_named, name, message)
}

private const val NUMBER_SLOT = "%1\$s"

/** "10월 6일 화요일"처럼 지역 표기에 맞춘 날짜. */
@Composable
private fun todayLabel(today: LocalDate): String {
    val locale = LocalConfiguration.current.locales[0]
    return remember(today, locale) {
        today.format(DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(locale, "MMMMEEEEd"), locale))
    }
}

private val AVATAR_SIZE = 32.dp
private val INITIAL_SIZE = 14.dp

@get:ArrayRes
private val GreetingSituation.arrayRes: Int
    get() = when (this) {
        GreetingSituation.Recall -> R.array.home_greeting_recall
        GreetingSituation.InspectionOverdue -> R.array.home_greeting_inspection_overdue
        GreetingSituation.MaintenanceOverdue -> R.array.home_greeting_maintenance_overdue
        GreetingSituation.InspectionSoon -> R.array.home_greeting_inspection_soon
        GreetingSituation.FirstVisit -> R.array.home_greeting_first_visit
        GreetingSituation.WelcomeBack -> R.array.home_greeting_welcome_back
        GreetingSituation.NewYear -> R.array.home_greeting_new_year
        GreetingSituation.IndependenceMovementDay -> R.array.home_greeting_independence_movement_day
        GreetingSituation.ChildrensDay -> R.array.home_greeting_childrens_day
        GreetingSituation.MemorialDay -> R.array.home_greeting_memorial_day
        GreetingSituation.LiberationDay -> R.array.home_greeting_liberation_day
        GreetingSituation.NationalFoundationDay -> R.array.home_greeting_national_foundation_day
        GreetingSituation.HangulDay -> R.array.home_greeting_hangul_day
        GreetingSituation.Christmas -> R.array.home_greeting_christmas
        GreetingSituation.CarAnniversary -> R.array.home_greeting_car_anniversary
        GreetingSituation.MileageMilestone -> R.array.home_greeting_mileage_milestone
        GreetingSituation.RecordedRecently -> R.array.home_greeting_recorded_recently
        GreetingSituation.MondayMorning -> R.array.home_greeting_monday_morning
        GreetingSituation.FridayEvening -> R.array.home_greeting_friday_evening
        GreetingSituation.CommuteMorning -> R.array.home_greeting_commute_morning
        GreetingSituation.CommuteEvening -> R.array.home_greeting_commute_evening
        GreetingSituation.Weekend -> R.array.home_greeting_weekend
        GreetingSituation.MileageStale -> R.array.home_greeting_mileage_stale
        GreetingSituation.SpringAfternoon -> R.array.home_greeting_spring_afternoon
        GreetingSituation.SummerAfternoon -> R.array.home_greeting_summer_afternoon
        GreetingSituation.AutumnAfternoon -> R.array.home_greeting_autumn_afternoon
        GreetingSituation.WinterAfternoon -> R.array.home_greeting_winter_afternoon
        GreetingSituation.Dawn -> R.array.home_greeting_dawn
        GreetingSituation.Morning -> R.array.home_greeting_morning
        GreetingSituation.Lunch -> R.array.home_greeting_lunch
        GreetingSituation.Afternoon -> R.array.home_greeting_afternoon
        GreetingSituation.Evening -> R.array.home_greeting_evening
        GreetingSituation.Night -> R.array.home_greeting_night
    }
