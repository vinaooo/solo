plugins {
    alias(libs.plugins.solo.android.library)
    alias(libs.plugins.solo.android.compose)
    alias(libs.plugins.solo.hilt)
}

android {
    namespace = "io.github.vinaooo.solo.core.ads"
}

dependencies {
    implementation(project(":core:designsystem"))
}
