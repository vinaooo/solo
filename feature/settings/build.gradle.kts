plugins {
    alias(libs.plugins.vinkit.android.feature)
    alias(libs.plugins.roborazzi)
}

android {
    namespace = "io.github.vinaooo.solo.feature.settings"
}

dependencies {
    implementation(project(":core:designsystem"))
    implementation(project(":core:ui"))
    implementation("com.github.vinaooo.vinkit:settings:${providers.gradleProperty("vinkit.tag").get()}")
    testImplementation(libs.roborazzi)
    testImplementation(libs.roborazzi.compose)
    testImplementation(libs.roborazzi.junit.rule)
}
