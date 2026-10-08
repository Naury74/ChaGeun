package com.naury.chageun.core.ads

import com.naury.chageun.core.domain.ads.AdEligibility
import com.naury.chageun.core.domain.ads.AppUsageRepository
import com.naury.chageun.core.domain.flags.FeatureFlagRepository
import com.naury.chageun.core.domain.vehicle.VehicleRepository
import java.time.Clock
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged

/** 광고를 보여도 되는지를 한곳에서 판단한다. 화면은 [LocalAdsEnabled]로만 결과를 받는다. */
@Singleton
class AdGate @Inject constructor(
    vehicleRepository: VehicleRepository,
    appUsageRepository: AppUsageRepository,
    consentController: AdConsentController,
    featureFlags: FeatureFlagRepository,
    private val clock: Clock,
) {
    // 스위치가 꺼지면 광고 요청 자체를 하지 않도록 화면에 넘기는 값을 끈다.
    val canShowAds: Flow<Boolean> = combine(
        vehicleRepository.observePrimaryVehicle(),
        appUsageRepository.usage,
        consentController.canRequestAds,
        featureFlags.flags,
    ) { vehicle, usage, canRequestAds, flags ->
        flags.nativeAdsEnabled && AdEligibility.isEligible(vehicle != null, usage, canRequestAds, clock.instant())
    }.distinctUntilChanged()
}
