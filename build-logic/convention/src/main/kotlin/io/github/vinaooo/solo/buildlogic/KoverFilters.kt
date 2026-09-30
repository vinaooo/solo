package io.github.vinaooo.solo.buildlogic

import kotlinx.kover.gradle.plugin.dsl.KoverProjectExtension

/**
 * Coverage counts hand-written, non-UI code: generated code (Hilt, Room, serializers) and composables are
 * left out; UI is covered by Compose UI and screenshot tests instead.
 * The root report is aggregated with the root project's filters, so it applies these too.
 */
internal fun KoverProjectExtension.excludeGeneratedAndUi() {
    reports.filters.excludes {
        annotatedBy("androidx.compose.runtime.Composable", "androidx.compose.ui.tooling.preview.Preview")
        classes(
            "*_Factory*", "*_HiltModules*", "*Hilt_*", "*_Impl*", "*_Provide*", "*_MembersInjector", "*.BuildConfig",
            "*ComposableSingletons*", "*.di.*", "*\$\$serializer",
        )
        packages("dagger.hilt.internal", "hilt_aggregated_deps")
    }
}
