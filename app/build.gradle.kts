plugins {
    alias(libs.plugins.chageun.android.application)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.chageun.hilt)
    alias(libs.plugins.aboutlibraries.android)
    alias(libs.plugins.baselineprofile)
    alias(libs.plugins.roborazzi)
}

// google-services.json은 Git에 없다. 파일이 있는 로컬·배포 빌드만 Firebase를 쓰고, CI와 처음 받은 사람도 빌드할 수 있다.
if (file("google-services.json").exists()) {
    apply(plugin = libs.plugins.google.services.get().pluginId)
    // release 빌드 때 R8 mapping을 올려 Crashlytics 스택을 원래 이름으로 보여 준다.
    apply(plugin = libs.plugins.firebase.crashlytics.get().pluginId)
}

// 개발 중 실제 광고 노출·클릭은 무효 트래픽으로 계정 정지 사유가 되므로 실제 ID는 release에만 쓴다.
// providers.gradleProperty는 local.properties를 읽지 않는다. ~/.gradle/gradle.properties나 -P로 넣는다.
val admobTestAppId = "ca-app-pub-3940256099942544~3347511713"
val admobAppId = providers.gradleProperty("chageun.admob.appId").orElse(admobTestAppId).get()
val admobNativeUnitId = providers.gradleProperty("chageun.admob.nativeUnitId").orNull

// SECURITY: 업로드 키와 비밀번호는 저장소에 두지 않는다. 속성이 없으면(CI 등) 서명하지 않은 release를 만든다.
val releaseStoreFile = providers.gradleProperty("chageun.signing.storeFile").orNull

android {
    namespace = "com.naury.chageun"

    defaultConfig {
        applicationId = "com.naury.chageun"
        versionCode = 1
        versionName = "0.1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        manifestPlaceholders["admobAppId"] = admobTestAppId
    }

    signingConfigs {
        if (releaseStoreFile != null) {
            create("release") {
                storeFile = file(releaseStoreFile)
                storePassword = providers.gradleProperty("chageun.signing.storePassword").get()
                keyAlias = providers.gradleProperty("chageun.signing.keyAlias").get()
                keyPassword = providers.gradleProperty("chageun.signing.keyPassword").get()
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
        }
        create("staging") {
            initWith(getByName("release"))
            applicationIdSuffix = ".staging"
            matchingFallbacks += "release"
            signingConfig = signingConfigs.getByName("debug")
            manifestPlaceholders["admobAppId"] = admobTestAppId
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            manifestPlaceholders["admobAppId"] = admobAppId
            signingConfig = signingConfigs.findByName("release")
            // 광고 단위 ID 기본값(테스트)은 core:ads 리소스에 있고 여기서 덮어쓴다.
            admobNativeUnitId?.let { resValue("string", "admob_native_unit_id", it) }
        }
    }

    buildFeatures {
        buildConfig = true
        resValues = true
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
    }

    lint {
        warningsAsErrors = false
        abortOnError = true
        checkDependencies = true
        error += listOf("MissingTranslation", "HardcodedText")
    }
}

// 앱 셸(하단 바·Rail·Hinge) Screenshot도 기능 모듈처럼 테스트 옆에 기준 이미지를 둔다.
roborazzi {
    outputDir.set(layout.projectDirectory.dir("src/test/screenshots"))
}

dependencies {
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.profileinstaller)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.play.app.update.ktx)
    implementation(libs.firebase.crashlytics)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)
    implementation(libs.androidx.hilt.lifecycle.viewmodel.compose)
    implementation(libs.androidx.hilt.work)
    implementation(projects.core.ads)
    implementation(projects.core.auth)
    implementation(projects.core.common)
    implementation(projects.core.datastore)
    implementation(projects.core.designsystem)
    implementation(projects.core.model)
    implementation(projects.core.notification)
    implementation(projects.core.ui)
    implementation(projects.data.backup)
    implementation(projects.data.history)
    implementation(projects.data.maintenance)
    implementation(projects.data.vehicle)
    implementation(projects.data.cloudbackup)
    implementation(projects.feature.account)
    implementation(projects.feature.ai)
    implementation(projects.feature.history)
    implementation(projects.feature.home)
    implementation(projects.feature.manage)
    implementation(projects.feature.onboarding)
    implementation(projects.feature.settings)
    implementation(projects.feature.vehicle)
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.compose.material3.adaptive.navigation.suite)
    implementation(libs.androidx.material3.adaptive)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.kotlinx.serialization.core)

    testImplementation(libs.androidx.compose.ui.test.junit4)
    testImplementation(libs.robolectric)
    testImplementation(libs.roborazzi)
    testImplementation(libs.roborazzi.compose)
    testImplementation(libs.truth)
    testImplementation(projects.core.uiTesting)

    baselineProfile(projects.benchmark)

    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
