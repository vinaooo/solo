import com.android.build.api.dsl.LibraryExtension
import io.github.vinaooo.solo.buildlogic.configureJUnitPlatform
import io.github.vinaooo.solo.buildlogic.configureKotlinAndroid
import io.github.vinaooo.solo.buildlogic.libs
import io.github.vinaooo.solo.buildlogic.version
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

class AndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("com.android.library")
        pluginManager.apply("solo.quality")
        extensions.configure<LibraryExtension> {
            configureKotlinAndroid(this)
            testOptions.targetSdk = libs.version("targetSdk").toInt()
            lint.targetSdk = libs.version("targetSdk").toInt()
        }
        configureJUnitPlatform()
    }
}
