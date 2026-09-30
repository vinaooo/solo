plugins {
    alias(libs.plugins.solo.android.library)
    alias(libs.plugins.solo.android.compose)
    alias(libs.plugins.roborazzi)
}

android {
    namespace = "io.github.vinaooo.solo.core.designsystem"
}

dependencies {
    implementation(project(":domain"))
    api(libs.androidx.compose.material3)
    api(libs.androidx.compose.material.icons.extended)

    testImplementation(libs.roborazzi)
    testImplementation(libs.roborazzi.compose)
    testImplementation(libs.roborazzi.junit.rule)
}
