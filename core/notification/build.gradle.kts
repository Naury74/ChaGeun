plugins {
    alias(libs.plugins.chageun.android.library)
    alias(libs.plugins.chageun.hilt)
}

android {
    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
}

dependencies {
    api(libs.androidx.work.runtime)
    implementation(projects.core.common)
    implementation(projects.core.domain)
    implementation(projects.core.ui)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.hilt.work)
    ksp(libs.androidx.hilt.compiler)

    testImplementation(projects.core.testing)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.androidx.work.testing)
    testImplementation(libs.robolectric)
}
