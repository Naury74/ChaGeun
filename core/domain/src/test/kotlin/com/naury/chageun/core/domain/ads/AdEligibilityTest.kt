package com.naury.chageun.core.domain.ads

import com.google.common.truth.Truth.assertThat
import java.time.Instant
import org.junit.Test

class AdEligibilityTest {

    private val now = Instant.parse("2026-10-02T12:00:00Z")
    private val firstRun = AppUsage(firstLaunchAt = now, launchCount = 1)

    @Test
    fun firstRun_rightAfterInstall_isNotEligible() {
        assertThat(AdEligibility.isEligible(hasVehicle = true, firstRun, canRequestAds = true, now)).isFalse()
    }

    @Test
    fun secondLaunch_orAfter24Hours_isEligible() {
        assertThat(AdEligibility.isEligible(true, firstRun.copy(launchCount = 2), true, now)).isTrue()
        assertThat(AdEligibility.isEligible(true, firstRun, true, now.plusSeconds(24 * 3600))).isTrue()
    }

    @Test
    fun noVehicle_orNoConsent_isNeverEligible() {
        val seasoned = AppUsage(firstLaunchAt = now.minusSeconds(7 * 24 * 3600), launchCount = 10)

        assertThat(AdEligibility.isEligible(hasVehicle = false, seasoned, canRequestAds = true, now)).isFalse()
        assertThat(AdEligibility.isEligible(hasVehicle = true, seasoned, canRequestAds = false, now)).isFalse()
    }
}
