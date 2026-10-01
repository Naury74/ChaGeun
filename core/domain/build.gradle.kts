plugins {
    alias(libs.plugins.chageun.jvm.library)
}

dependencies {
    api(projects.core.model)
    api(libs.kotlinx.coroutines.core)
    implementation(libs.javax.inject)

    testImplementation(projects.core.testing)
    testImplementation(libs.kotlinx.coroutines.test)
}
