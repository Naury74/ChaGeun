plugins {
    alias(libs.plugins.chageun.android.compose)
}

dependencies {
    api(platform(libs.androidx.compose.bom))
    api(libs.androidx.compose.foundation)
    api(libs.androidx.compose.material3)
    api(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material.icons.core)
    // 차근 테마 안쪽에 Kinetica 테마를 둔다. 기능 화면도 Kinetica 컴포넌트와 효과(누름·등장·전환·숫자)를 바로 쓰도록 함께 내보낸다.
    api(libs.kinetica.ui)
    api(libs.kinetica.render.compose)
    api(libs.kinetica.presets)
}
