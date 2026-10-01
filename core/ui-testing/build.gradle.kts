plugins {
    alias(libs.plugins.chageun.android.compose)
}

android {
    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
}

dependencies {
    api(libs.androidx.compose.ui.test.junit4)

    testImplementation(libs.androidx.compose.material3)
    testImplementation(libs.robolectric)
    testImplementation(libs.truth)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
