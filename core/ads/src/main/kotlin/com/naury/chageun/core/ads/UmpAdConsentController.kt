package com.naury.chageun.core.ads

import android.app.Activity
import android.content.Context
import com.google.android.gms.ads.MobileAds
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import com.naury.chageun.core.common.logging.AppLogger
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * UMP 동의를 받고, 광고를 요청할 수 있게 된 뒤에만 Mobile Ads SDK를 초기화한다.
 * 동의 화면은 필요한 지역(EEA 등)에서만 뜬다.
 */
@Singleton
internal class UmpAdConsentController @Inject constructor(
    @ApplicationContext private val context: Context,
    private val logger: AppLogger,
) : AdConsentController {
    private val consentInformation: ConsentInformation = UserMessagingPlatform.getConsentInformation(context)
    private val sdkStarted = AtomicBoolean(false)

    private val _canRequestAds = MutableStateFlow(consentInformation.canRequestAds())
    override val canRequestAds: StateFlow<Boolean> = _canRequestAds.asStateFlow()

    private val _isPrivacyOptionsRequired = MutableStateFlow(false)

    override val isPrivacyOptionsRequired: StateFlow<Boolean> = _isPrivacyOptionsRequired.asStateFlow()

    override fun gatherConsent(activity: Activity) {
        consentInformation.requestConsentInfoUpdate(
            activity,
            ConsentRequestParameters.Builder().build(),
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { error ->
                    if (error != null) logger.warn("ads_consent_form_failed")
                    refresh()
                }
            },
            {
                logger.warn("ads_consent_update_failed")
                refresh()
            },
        )
        // 이전 실행에서 이미 동의했다면 정보 갱신을 기다리지 않고 바로 시작한다.
        refresh()
    }

    override fun showPrivacyOptions(activity: Activity) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity) { refresh() }
    }

    private fun refresh() {
        val canRequest = consentInformation.canRequestAds()
        _canRequestAds.value = canRequest
        _isPrivacyOptionsRequired.value = consentInformation.privacyOptionsRequirementStatus ==
            ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED
        if (canRequest && sdkStarted.compareAndSet(false, true)) MobileAds.initialize(context)
    }
}
