import com.android.build.api.dsl.ApplicationExtension
import io.github.vinaooo.solo.buildlogic.AdIds
import io.github.vinaooo.solo.buildlogic.AppVersion
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
            appVersion().let { version ->
                defaultConfig.versionCode = version.code
                defaultConfig.versionName = version.name
            }
            releaseSigning()?.let { signing ->
                buildTypes.getByName("release").signingConfig = signingConfigs.create("release") {
                    storeFile = signing.storeFile
                    storePassword = signing.storePassword
                    keyAlias = signing.keyAlias
                    keyPassword = signing.keyPassword
                }
            }
        }
        configureAds()
        configureJUnitPlatform()
    }

    /** The version from git; see [AppVersion]. */
    private fun Project.appVersion(): AppVersion = AppVersion.fromGit(
        describe = git(AppVersion.describeArguments),
        commitCount = git(listOf("rev-list", "--count", "HEAD")),
        shallow = git(listOf("rev-parse", "--is-shallow-repository"))?.trim() == "true",
    )

    /** Output of a git command run in the root project, or null when it fails (for example outside a checkout). */
    private fun Project.git(arguments: List<String>): String? {
        // Run through providers so the configuration cache reruns it and notices a new commit or tag.
        val result = providers.exec {
            workingDir = rootDir
            commandLine(listOf("git") + arguments)
            isIgnoreExitValue = true
        }
        return result.standardOutput.asText.get().takeIf { result.result.get().exitValue == 0 }
    }

    /**
     * AdMob IDs: the app ID becomes the `adMobAppId` manifest placeholder and the banner ad unit
     * `BuildConfig.AD_BANNER_ID`. Debug builds always get Google's test IDs; see [AdIds].
     */
    private fun Project.configureAds() = extensions.configure<ApplicationExtension> {
        val properties = localProperties()
        val release = AdIds.resolve(properties, environment(AdIds.environmentVariables)) ?: AdIds.TEST
        buildFeatures.buildConfig = true
        defaultConfig.buildConfigField(
            "String",
            "AD_TEST_DEVICE_IDS",
            AdIds.testDevices(properties).joinToString(",").quoted(),
        )
        mapOf("debug" to AdIds.TEST, "release" to release).forEach { (buildType, ids) ->
            buildTypes.getByName(buildType) {
                manifestPlaceholders["adMobAppId"] = ids.appId
                buildConfigField("String", "AD_BANNER_ID", ids.bannerId.quoted())
            }
        }
    }

    private fun String.quoted() = "\"$this\""

    /** The upload key from local.properties or the CI environment; without one, release builds stay unsigned. */
    private fun Project.releaseSigning(): ReleaseSigning? =
        ReleaseSigning.resolve(localProperties(), environment(ReleaseSigning.environmentVariables), rootDir)

    /** local.properties, read through providers so the configuration cache notices when it changes. */
    private fun Project.localProperties(): Map<String, String> =
        providers.fileContents(rootProject.layout.projectDirectory.file("local.properties"))
            .asText.orNull
            ?.let { text -> Properties().apply { load(text.reader()) } }
            ?.let { properties -> properties.stringPropertyNames().associateWith(properties::getProperty) }
            .orEmpty()

    private fun Project.environment(names: List<String>): Map<String, String> =
        names.mapNotNull { name -> providers.environmentVariable(name).orNull?.let { name to it } }.toMap()
}
