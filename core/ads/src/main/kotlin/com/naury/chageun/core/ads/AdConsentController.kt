package com.naury.chageun.core.ads

import android.app.Activity
import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** 광고 동의 상태와 동의 화면. 화면과 ViewModel은 UMP SDK 대신 이 인터페이스에 의존한다. */
interface AdConsentController {
    val canRequestAds: StateFlow<Boolean>

    /** 설정 화면의 '광고 개인정보 설정' 버튼 노출 여부. UMP가 요구하는 지역(EEA 등)에서만 true다. */
    val isPrivacyOptionsRequired: StateFlow<Boolean>

    fun gatherConsent(activity: Activity)

    fun showPrivacyOptions(activity: Activity)
}

/** 광고를 쓰지 않는 테스트·Preview용. 동의 화면을 띄우지 않고 광고도 요청하지 않는다. */
object NoAdConsent : AdConsentController {
    override val canRequestAds: StateFlow<Boolean> = MutableStateFlow(false)
    override val isPrivacyOptionsRequired: StateFlow<Boolean> = MutableStateFlow(false)

    override fun gatherConsent(activity: Activity) = Unit

    override fun showPrivacyOptions(activity: Activity) = Unit
}

/** 설정 화면이 ViewModel을 거치지 않고 Activity 기반 UMP 화면을 열 수 있게 MainActivity가 제공한다. */
val LocalAdConsent = staticCompositionLocalOf<AdConsentController> { NoAdConsent }
