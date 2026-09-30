import com.android.build.api.dsl.CommonExtension
import io.github.vinaooo.solo.buildlogic.libs
import io.github.vinaooo.solo.buildlogic.library
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getByType

class AndroidComposeConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("org.jetbrains.kotlin.plugin.compose")
        extensions.getByType<CommonExtension>().buildFeatures.compose = true
        dependencies {
            val bom = platform(libs.library("androidx-compose-bom"))
            "implementation"(bom)
            "testImplementation"(bom)
            "implementation"(libs.library("androidx-compose-ui"))
            "implementation"(libs.library("androidx-compose-ui-graphics"))
            "implementation"(libs.library("androidx-compose-ui-tooling-preview"))
            "implementation"(libs.library("androidx-compose-material3"))
            "debugImplementation"(libs.library("androidx-compose-ui-tooling"))
            "debugImplementation"(libs.library("androidx-compose-ui-test-manifest"))
            "testImplementation"(libs.library("androidx-compose-ui-test-junit4"))
            "testImplementation"(libs.library("robolectric"))
            "testImplementation"(libs.library("junit4"))
        }
    }
}
