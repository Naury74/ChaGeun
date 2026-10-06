plugins {
    alias(libs.plugins.chageun.android.library)
    alias(libs.plugins.chageun.hilt)
}

dependencies {
    api(projects.core.domain)
    implementation(projects.core.common)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.kotlinx.coroutines.play.services)
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.googleid)
    implementation(libs.play.services.auth)

    testImplementation(libs.kotlinx.coroutines.test)
}
