plugins {
    alias(libs.plugins.chageun.android.feature)
}

android {
    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
}

dependencies {
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.material3.adaptive)
    implementation(projects.core.common)
    implementation(libs.androidx.browser)
    implementation(libs.mlkit.genai.prompt)

    testImplementation(libs.androidx.compose.ui.test.junit4)
    testImplementation(libs.robolectric)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
