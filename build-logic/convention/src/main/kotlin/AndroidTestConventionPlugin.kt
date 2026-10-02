import com.android.build.api.dsl.TestExtension
import com.naury.chageun.buildlogic.configureKotlinAndroid
import com.naury.chageun.buildlogic.configureQuality
import com.naury.chageun.buildlogic.namespaceFromPath
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

/** 벤치마크처럼 Gradle Managed Devices에서 :app을 대상으로 실행하는 instrumented test 모듈. */
class AndroidTestConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("com.android.test")
        configureQuality()

        extensions.configure<TestExtension> {
            namespace = namespaceFromPath()
            configureKotlinAndroid(this)
            defaultConfig.testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
            targetProjectPath = ":app"
            testOptions.managedDevices.localDevices.maybeCreate(MANAGED_DEVICE).apply {
                device = "Pixel 6"
                apiLevel = MANAGED_DEVICE_API
                systemImageSource = "aosp-atd"
            }
        }
    }

    companion object {
        const val MANAGED_DEVICE = "pixel6Api34"
        private const val MANAGED_DEVICE_API = 34
    }
}
