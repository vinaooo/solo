plugins {
    alias(libs.plugins.vinkit.android.library)
    alias(libs.plugins.vinkit.android.compose)
    alias(libs.plugins.roborazzi)
}

android {
    namespace = "io.github.vinaooo.solo.core.designsystem"
}

dependencies {
    implementation(project(":domain"))
    api("com.github.vinaooo.vinkit:designsystem:${providers.gradleProperty("vinkit.tag").get()}")
    api(libs.androidx.compose.material3)
    api(libs.androidx.compose.material.icons.extended)

    testImplementation(libs.roborazzi)
    testImplementation(libs.roborazzi.compose)
    testImplementation(libs.roborazzi.junit.rule)
}
