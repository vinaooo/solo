plugins {
    alias(libs.plugins.solo.android.feature)
    alias(libs.plugins.roborazzi)
}

android {
    namespace = "io.github.vinaooo.solo.feature.game"
}

dependencies {
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.core.ktx)
    testImplementation(libs.roborazzi)
    testImplementation(libs.roborazzi.compose)
    testImplementation(libs.roborazzi.junit.rule)
}
