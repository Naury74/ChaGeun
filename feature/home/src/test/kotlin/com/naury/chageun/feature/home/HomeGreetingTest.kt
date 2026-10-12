package com.naury.chageun.feature.home

import com.google.common.truth.Truth.assertThat
import com.naury.chageun.core.domain.ads.AppUsage
import com.naury.chageun.core.model.AuthMethod
import com.naury.chageun.core.model.AuthUser
import com.naury.chageun.core.model.HealthReason
import com.naury.chageun.core.model.InspectionSchedule
import com.naury.chageun.core.model.InspectionSource
import com.naury.chageun.core.model.InspectionState
import com.naury.chageun.core.model.InspectionStatus
import com.naury.chageun.core.model.MaintenanceItem
import com.naury.chageun.core.model.MaintenanceState
import com.naury.chageun.core.model.VehicleHealthLevel
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import org.junit.Test

class HomeGreetingTest {

    private val now = LocalDateTime.of(2026, 10, 1, 9, 0)

    @Test
    fun greetingName_prefersDisplayName_thenEmailId() {
        val google = AuthUser("u", "naury@gmail.com", " 나우리 ", true, setOf(AuthMethod.Google))
        val email = AuthUser("u", "driver.kim@example.com", null, true, setOf(AuthMethod.Email))
        val unverified = email.copy(isEmailVerified = false)

        assertThat(google.greetingName()).isEqualTo("나우리")
        assertThat(email.greetingName()).isEqualTo("driver.kim")
        assertThat(unverified.greetingName()).isNull()
    }

    @Test
    fun greetingName_usesGoogleName_whenAccountHasNoNameOfItsOwn() {
        // 이메일로 가입한 뒤 Google을 연결한 계정. 계정 이름이 비어 있어 이메일 앞부분 대신 Google 이름을 부른다.
        val linked = AuthUser(
            uid = "u",
            email = "naury@gmail.com",
            displayName = null,
            isEmailVerified = true,
            methods = setOf(AuthMethod.Email, AuthMethod.Google),
            googleName = "나우리",
        )

        assertThat(linked.greetingName()).isEqualTo("나우리")
        assertThat(linked.copy(displayName = "차근이").greetingName()).isEqualTo("차근이")
    }

    @Test
    fun facts_collectSafetyState() {
        val state = content(
            VehicleHealthLevel.NeedsAttention,
            listOf(HealthReason.ActiveSafetyRecall),
            status(MaintenanceItem.Tire, MaintenanceState.Overdue),
            status(MaintenanceItem.EngineOil, MaintenanceState.Overdue),
            status(MaintenanceItem.Wiper, MaintenanceState.Due),
        ).let {
            it.copy(
                overview = it.overview.copy(
                    inspection = InspectionStatus(
                        InspectionSchedule(TODAY.plusDays(9), InspectionSource.User),
                        daysLeft = 9,
                        state = InspectionState.DueSoon,
                    ),
                ),
            )
        }

        val facts = greetingFacts(now, ZoneOffset.UTC, state.vehicle, state.overview, emptyList(), usage())

        assertThat(facts.hasActiveRecall).isTrue()
        assertThat(facts.overdueItemCount).isEqualTo(2)
        assertThat(facts.isInspectionDueSoon).isTrue()
        assertThat(facts.inspectionDaysLeft).isEqualTo(9)
    }

    @Test
    fun facts_useLaunchBeforeThisOne_asPreviousVisit() {
        val state = content(VehicleHealthLevel.Good, emptyList())
        val thisLaunch = now.minusMinutes(1).toInstant(ZoneOffset.UTC)
        val lastMonth = LocalDate.of(2026, 9, 1)
        val justLaunched = usage(last = thisLaunch, previous = lastMonth.atStartOfDay().toInstant(ZoneOffset.UTC))
        val notYetRecorded = usage(last = lastMonth.atStartOfDay().toInstant(ZoneOffset.UTC), previous = null)

        val facts = greetingFacts(now, ZoneOffset.UTC, state.vehicle, state.overview, emptyList(), justLaunched)
        val early = greetingFacts(now, ZoneOffset.UTC, state.vehicle, state.overview, emptyList(), notYetRecorded)

        assertThat(facts.previousVisitDate).isEqualTo(lastMonth)
        assertThat(early.previousVisitDate).isEqualTo(lastMonth)
        assertThat(facts.isFirstVisit).isFalse()
    }

    private fun usage(last: Instant? = null, previous: Instant? = null) =
        AppUsage(firstLaunchAt = Instant.EPOCH, launchCount = 10, lastLaunchAt = last, previousLaunchAt = previous)
}
