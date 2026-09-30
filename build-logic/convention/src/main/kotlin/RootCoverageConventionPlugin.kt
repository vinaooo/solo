import io.github.vinaooo.solo.buildlogic.excludeGeneratedAndUi
import kotlinx.kover.gradle.plugin.dsl.KoverProjectExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

/** The root Kover report: every module's coverage merged, with the same filters and the overall floor. */
class RootCoverageConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("org.jetbrains.kotlinx.kover")
        extensions.configure<KoverProjectExtension> {
            excludeGeneratedAndUi()
            reports.verify.rule("Overall line coverage") { minBound(MIN_LINE_COVERAGE) }
        }
    }

    private companion object {
        const val MIN_LINE_COVERAGE = 70
    }
}
