import com.android.build.api.dsl.ApplicationExtension
import com.naury.chageun.buildlogic.configureAndroidCompose
import com.naury.chageun.buildlogic.configureKotlinAndroid
import com.naury.chageun.buildlogic.configureQuality
import com.naury.chageun.buildlogic.libs
import com.naury.chageun.buildlogic.versionInt
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

class AndroidApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("com.android.application")
        configureQuality()

        extensions.configure<ApplicationExtension> {
            configureKotlinAndroid(this)
            configureAndroidCompose(this)
            defaultConfig.targetSdk = libs.versionInt("targetSdk")
        }
    }
}
