plugins {
    alias(libs.plugins.chageun.android.test)
    alias(libs.plugins.baselineprofile)
}

android {
    defaultConfig {
        // Macrobenchmark and profile collection without root need API 28+.
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
        // benchmark-macro pulls wire-runtime 6.4.0 (GHSA-9rm7-3qhh-h2mc).
        implementation(libs.wire.runtime)
    }
}
