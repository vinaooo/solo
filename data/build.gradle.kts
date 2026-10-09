plugins {
    alias(libs.plugins.vinkit.android.library)
    alias(libs.plugins.vinkit.hilt)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "io.github.vinaooo.solo.data"
}

dependencies {
    implementation(project(":domain"))
    implementation("com.github.vinaooo.vinkit:settings:${providers.gradleProperty("vinkit.tag").get()}")
    implementation("com.github.vinaooo.vinkit:achievements:${providers.gradleProperty("vinkit.tag").get()}")
    implementation("com.github.vinaooo.vinkit:scores:${providers.gradleProperty("vinkit.tag").get()}")
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.core)

    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    testImplementation(libs.robolectric)
    testImplementation(libs.junit4)
    testImplementation(libs.androidx.test.core)
}
