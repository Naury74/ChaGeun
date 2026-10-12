plugins {
    alias(libs.plugins.chageun.android.compose)
    alias(libs.plugins.roborazzi)
}

dependencies {
    api(projects.core.designsystem)
    api(projects.core.model)
    api(projects.core.domain)
    implementation(libs.androidx.material3.adaptive)
    implementation(libs.androidx.exifinterface)
    implementation(libs.androidx.activity.compose)
    // 사진 편집 화면. FrameKit 타입은 이 모듈 밖으로 내보내지 않는다.
    implementation(libs.framekit)
    api(libs.androidx.compose.material.icons.extended)

    testImplementation(projects.core.uiTesting)
    testImplementation(libs.roborazzi)
    testImplementation(libs.roborazzi.compose)
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

// 대화상자·입력 칸처럼 여러 화면이 함께 쓰는 부품도 기능 모듈처럼 테스트 옆에 기준 이미지를 둔다.
roborazzi {
    outputDir.set(layout.projectDirectory.dir("src/test/screenshots"))
}
