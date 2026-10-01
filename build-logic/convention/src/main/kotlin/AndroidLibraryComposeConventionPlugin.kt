import com.android.build.api.dsl.LibraryExtension
import com.naury.chageun.buildlogic.configureAndroidCompose
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

class AndroidLibraryComposeConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("chageun.android.library")
        extensions.configure<LibraryExtension> {
            configureAndroidCompose(this)
        }
    }
}
