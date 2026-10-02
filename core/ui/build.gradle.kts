plugins {
    alias(libs.plugins.chageun.android.compose)
}

dependencies {
    api(projects.core.designsystem)
    api(projects.core.model)
    api(projects.core.domain)
    implementation(libs.androidx.material3.adaptive)

    testImplementation(libs.truth)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}

android {
    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
}
