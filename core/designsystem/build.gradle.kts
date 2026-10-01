plugins {
    alias(libs.plugins.chageun.android.compose)
}

dependencies {
    api(libs.androidx.compose.foundation)
    api(libs.androidx.compose.material3)
    api(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material.icons.core)
}
