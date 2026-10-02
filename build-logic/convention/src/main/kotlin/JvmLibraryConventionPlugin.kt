import com.naury.chageun.buildlogic.configureKotlinJvm
import com.naury.chageun.buildlogic.configureQuality
import org.gradle.api.Plugin
import org.gradle.api.Project

class JvmLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("org.jetbrains.kotlin.jvm")
            configureQuality()
            configureKotlinJvm()

            // CI와 문서에 적힌 로컬 검사는 모든 모듈에서 Android 쪽 task 이름으로 실행한다.
            tasks.register("testDebugUnitTest") {
                group = "verification"
                dependsOn("test")
            }
        }
    }
}
