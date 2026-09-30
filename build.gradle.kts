plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.detekt) apply false
    alias(libs.plugins.ktlint) apply false
    alias(libs.plugins.pitest) apply false
    alias(libs.plugins.roborazzi) apply false
    alias(libs.plugins.kover) apply false
    alias(libs.plugins.solo.root.coverage)
}

dependencies {
    kover(project(":domain"))
    kover(project(":data"))
    kover(project(":feature:game"))
    kover(project(":feature:scores"))
    kover(project(":feature:settings"))
}

// `./gradlew test` also runs the build logic's own tests (the included build isn't a subproject).
tasks.register("test") {
    dependsOn(gradle.includedBuild("build-logic").task(":convention:test"))
}
