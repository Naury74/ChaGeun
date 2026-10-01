plugins {
    alias(libs.plugins.chageun.android.library)
    alias(libs.plugins.chageun.android.room)
    alias(libs.plugins.chageun.hilt)
}

android {
    sourceSets {
        getByName("test").assets.directories.add("$projectDir/schemas")
    }
    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
}

dependencies {
    api(projects.core.model)
    implementation(libs.kotlinx.coroutines.core)

    testImplementation(libs.androidx.test.core)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.robolectric)
    testImplementation(libs.turbine)
}
