plugins {
    alias(libs.plugins.chageun.android.compose)
    alias(libs.plugins.chageun.hilt)
}

android {
    buildFeatures {
        buildConfig = true
    }
    defaultConfig {
        // 기본값은 Google 공식 테스트 광고 단위다. 실제 ID는 local.properties나 CI Secret에서 Gradle 속성으로 넣는다.
        val nativeUnitId = providers.gradleProperty("chageun.admob.nativeUnitId")
            .orElse("ca-app-pub-3940256099942544/2247696110")
            .get()
        buildConfigField("String", "NATIVE_AD_UNIT_ID", "\"$nativeUnitId\"")
    }
}

dependencies {
    api(projects.core.domain)
    implementation(projects.core.designsystem)
    implementation(projects.core.common)
    implementation(libs.androidx.compose.material3)
    implementation(libs.play.services.ads)
    implementation(libs.user.messaging.platform)
}
