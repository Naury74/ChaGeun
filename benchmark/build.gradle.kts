plugins {
    alias(libs.plugins.chageun.android.test)
    alias(libs.plugins.baselineprofile)
}

android {
    defaultConfig {
        // root 없이 Macrobenchmark와 프로파일 수집을 하려면 API 28+가 필요하다.
        minSdk = 28
    }
}

baselineProfile {
    managedDevices += "pixel6Api34"
    useConnectedDevices = false
}

dependencies {
    implementation(libs.androidx.benchmark.macro.junit4)
    implementation(libs.androidx.test.ext.junit)
    implementation(libs.androidx.test.uiautomator)

    constraints {
        // benchmark-macro가 wire-runtime 6.4.0을 끌어온다(GHSA-9rm7-3qhh-h2mc).
        implementation(libs.wire.runtime)
    }
}
