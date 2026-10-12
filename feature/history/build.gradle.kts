plugins {
    alias(libs.plugins.chageun.android.feature)
}

android {
    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
}

dependencies {
    implementation(projects.core.ads)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.material3.adaptive)
    // 기록에 붙인 소리를 상세 화면에서 바로 재생한다.
    implementation(libs.androidx.media3.exoplayer)

    testImplementation(libs.androidx.compose.ui.test.junit4)
    testImplementation(libs.robolectric)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
