import com.naury.chageun.buildlogic.libs
import com.naury.chageun.buildlogic.library
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import io.github.takahirom.roborazzi.RoborazziExtension

class AndroidFeatureConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("chageun.android.library.compose")
        pluginManager.apply("chageun.hilt")
        // 기준 이미지는 테스트 옆에 둔다. `recordRoborazziDebug`로 갱신하고 `verifyRoborazziDebug`로 비교한다.
        pluginManager.apply("io.github.takahirom.roborazzi")

        dependencies {
            add("implementation", project(":core:model"))
            add("implementation", project(":core:domain"))
            add("implementation", project(":core:designsystem"))
            add("implementation", project(":core:ui"))

            add("implementation", libs.library("androidx-compose-material3"))
            add("implementation", libs.library("androidx-hilt-lifecycle-viewmodel-compose"))
            add("implementation", libs.library("androidx-lifecycle-runtime-compose"))
            add("implementation", libs.library("androidx-lifecycle-viewmodel-compose"))
            add("implementation", libs.library("androidx-navigation3-runtime"))

            add("testImplementation", project(":core:testing"))
            add("testImplementation", project(":core:ui-testing"))
            add("testImplementation", libs.library("roborazzi"))
            add("testImplementation", libs.library("roborazzi-compose"))
            add("testImplementation", libs.library("kotlinx-coroutines-test"))
            add("testImplementation", libs.library("turbine"))
        }
        extensions.configure<RoborazziExtension> {
            outputDir.set(layout.projectDirectory.dir("src/test/screenshots"))
        }
    }
}
