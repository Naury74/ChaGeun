package com.naury.chageun

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.naury.chageun.core.ads.AdConsentController
import com.naury.chageun.core.ads.AdGate
import com.naury.chageun.core.ads.LocalAdConsent
import com.naury.chageun.core.ads.LocalAdsEnabled
import com.naury.chageun.core.domain.ads.AppUsageRepository
import com.naury.chageun.core.domain.analytics.AnalyticsTracker
import com.naury.chageun.core.domain.flags.FeatureFlagRepository
import com.naury.chageun.core.model.FeatureFlags
import com.naury.chageun.core.notification.DeepLink
import com.naury.chageun.core.notification.DeepLinks.deepLinkOrNull
import com.naury.chageun.core.ui.LocalAnalyticsTracker
import com.naury.chageun.core.ui.LocalFeatureFlags
import com.naury.chageun.ui.ChageunRoot
import com.naury.chageun.update.AppUpdateChecker
import dagger.hilt.android.AndroidEntryPoint
import java.time.Clock
import javax.inject.Inject
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var analyticsTracker: AnalyticsTracker

    @Inject
    lateinit var adConsentController: AdConsentController

    @Inject
    lateinit var adGate: AdGate

    @Inject
    lateinit var appUsageRepository: AppUsageRepository

    @Inject
    lateinit var clock: Clock

    @Inject
    lateinit var featureFlagRepository: FeatureFlagRepository

    @Inject
    lateinit var appUpdateChecker: AppUpdateChecker

    private val deepLink = mutableStateOf<DeepLink?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // 복원된 Activity는 실행 intent에 대한 이동을 이미 마쳤다.
        if (savedInstanceState == null) {
            deepLink.value = intent.deepLinkOrNull()
            lifecycleScope.launch { appUsageRepository.recordLaunch(clock.instant()) }
        }
        setContent {
            val adsEnabled by adGate.canShowAds.collectAsStateWithLifecycle(initialValue = false)
            val featureFlags by featureFlagRepository.flags.collectAsStateWithLifecycle(initialValue = FeatureFlags())
            CompositionLocalProvider(
                LocalFeatureFlags provides featureFlags,
                LocalAnalyticsTracker provides analyticsTracker,
                LocalAdsEnabled provides adsEnabled,
                LocalAdConsent provides adConsentController,
            ) {
                ChageunRoot(
                    deepLink = deepLink.value,
                    onDeepLinkHandled = { deepLink.value = null },
                    // 광고 동의 화면도 온보딩을 끊지 않도록 메인 화면에 들어온 뒤에 띄운다 (기획서 17.1).
                    onMainShown = { adConsentController.gatherConsent(this) },
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        appUpdateChecker.check(this)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.deepLinkOrNull()?.let { deepLink.value = it }
    }
}
