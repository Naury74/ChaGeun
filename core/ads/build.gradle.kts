plugins {
    alias(libs.plugins.chageun.android.compose)
    alias(libs.plugins.chageun.hilt)
}

dependencies {
    api(projects.core.domain)
    implementation(projects.core.designsystem)
    implementation(projects.core.common)
    implementation(libs.androidx.compose.material3)
    implementation(libs.play.services.ads)
    implementation(libs.user.messaging.platform)
}
