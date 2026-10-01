plugins {
    alias(libs.plugins.chageun.jvm.library)
}

dependencies {
    api(projects.core.model)
    implementation(libs.javax.inject)
}
