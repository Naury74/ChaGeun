plugins {
    alias(libs.plugins.chageun.jvm.library)
}

dependencies {
    api(projects.core.domain)
    api(libs.junit4)
    api(libs.kotlinx.coroutines.test)
}
