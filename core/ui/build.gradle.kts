plugins {
    alias(libs.plugins.solo.android.library)
    alias(libs.plugins.solo.android.compose)
}

android {
    namespace = "io.github.vinaooo.solo.core.ui"
}

dependencies {
    implementation(project(":core:designsystem"))
    implementation(project(":domain"))
}
