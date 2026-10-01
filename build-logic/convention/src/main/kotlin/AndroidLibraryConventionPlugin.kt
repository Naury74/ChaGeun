import com.android.build.api.dsl.LibraryExtension
import com.naury.chageun.buildlogic.configureKotlinAndroid
import com.naury.chageun.buildlogic.configureQuality
import com.naury.chageun.buildlogic.namespaceFromPath
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

class AndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("com.android.library")
        configureQuality()

        extensions.configure<LibraryExtension> {
            namespace = namespaceFromPath()
            configureKotlinAndroid(this)
            defaultConfig.testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        }
    }
}
