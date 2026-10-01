import com.naury.chageun.buildlogic.configureKotlinJvm
import com.naury.chageun.buildlogic.configureQuality
import org.gradle.api.Plugin
import org.gradle.api.Project

class JvmLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("org.jetbrains.kotlin.jvm")
        configureQuality()
        configureKotlinJvm()
    }
}
