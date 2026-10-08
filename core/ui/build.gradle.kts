plugins {
    alias(libs.plugins.vinkit.android.library)
    alias(libs.plugins.vinkit.android.compose)
}

android {
    namespace = "io.github.vinaooo.solo.core.ui"
}

dependencies {
    implementation(project(":core:designsystem"))
    implementation(project(":domain"))
    api("com.github.vinaooo.vinkit:designsystem:${providers.gradleProperty("vinkit.tag").get()}")
}
