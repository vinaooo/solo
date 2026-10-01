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
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.play.services.ads)
    implementation(libs.user.messaging.platform)
}
