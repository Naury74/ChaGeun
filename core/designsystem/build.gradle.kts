plugins {
    alias(libs.plugins.chageun.android.compose)
}

dependencies {
    api(platform(libs.androidx.compose.bom))
    api(libs.androidx.compose.foundation)
    api(libs.androidx.compose.material3)
    api(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material.icons.core)
    // 차근 테마 안쪽에 Kinetica 테마를 두고, 공통 컴포넌트 일부를 Kinetica로 그린다. 기능 모듈에는 Kinetica 타입을 드러내지 않는다.
    implementation(libs.kinetica.ui)
}
