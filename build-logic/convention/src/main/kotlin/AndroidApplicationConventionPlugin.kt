import com.android.build.api.dsl.ApplicationExtension
import io.github.vinaooo.solo.buildlogic.ReleaseSigning
import io.github.vinaooo.solo.buildlogic.configureJUnitPlatform
import io.github.vinaooo.solo.buildlogic.configureKotlinAndroid
import io.github.vinaooo.solo.buildlogic.libs
import io.github.vinaooo.solo.buildlogic.version
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import java.util.Properties

class AndroidApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("com.android.application")
        pluginManager.apply("solo.quality")
        extensions.configure<ApplicationExtension> {
            configureKotlinAndroid(this)
            defaultConfig.targetSdk = libs.version("targetSdk").toInt()
            releaseSigning()?.let { signing ->
                buildTypes.getByName("release").signingConfig = signingConfigs.create("release") {
                    storeFile = signing.storeFile
                    storePassword = signing.storePassword
                    keyAlias = signing.keyAlias
                    keyPassword = signing.keyPassword
                }
            }
        }
        configureJUnitPlatform()
    }

    /** The upload key from local.properties or the CI environment; without one, release builds stay unsigned. */
    private fun Project.releaseSigning(): ReleaseSigning? {
        // Read through providers so the configuration cache notices when local.properties changes.
        val localProperties = providers.fileContents(rootProject.layout.projectDirectory.file("local.properties"))
            .asText.orNull
            ?.let { text -> Properties().apply { load(text.reader()) } }
            ?.let { properties -> properties.stringPropertyNames().associateWith(properties::getProperty) }
            .orEmpty()
        val environment = ReleaseSigning.environmentVariables
            .mapNotNull { name -> providers.environmentVariable(name).orNull?.let { name to it } }
            .toMap()
        return ReleaseSigning.resolve(localProperties, environment, rootDir)
    }
}
