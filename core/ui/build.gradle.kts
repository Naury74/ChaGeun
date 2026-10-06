plugins {
    alias(libs.plugins.chageun.android.compose)
}

dependencies {
    api(projects.core.designsystem)
    api(projects.core.model)
    api(projects.core.domain)
    implementation(libs.androidx.material3.adaptive)
    implementation(libs.androidx.exifinterface)
    implementation(libs.androidx.activity.compose)
    api(libs.androidx.compose.material.icons.extended)

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
