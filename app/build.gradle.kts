plugins {
    alias(libs.plugins.vinkit.android.application)
    alias(libs.plugins.vinkit.android.compose)
    alias(libs.plugins.vinkit.hilt)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.roborazzi)
}

android {
    namespace = "io.github.vinaooo.solo"

    defaultConfig {
        applicationId = "io.github.vinaooo.solo"
        // Runs instrumented tests on HiltTestApplication; the orchestrator gives each test its own process and clears
        // the app's data (saved game, settings, scores) in between.
        testInstrumentationRunner = "io.github.vinaooo.solo.HiltTestRunner"
        testInstrumentationRunnerArguments["clearPackageData"] = "true"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    testOptions {
        execution = "ANDROIDX_TEST_ORCHESTRATOR"
    }

    // FakeAdsModule keeps the real ads SDKs out of both Robolectric and on-device tests.
    sourceSets {
        getByName("test").kotlin.directories += "src/sharedTest/kotlin"
        getByName("androidTest").kotlin.directories += "src/sharedTest/kotlin"
    }

    androidResources {
        localeFilters += listOf("en", "pt-rBR")
    }
}

dependencies {
    implementation(project(":domain"))
    implementation(project(":data"))
    implementation(project(":core:designsystem"))
    implementation(project(":core:ui"))
    implementation(project(":core:ads"))
    implementation(project(":feature:game"))
    implementation(project(":feature:scores"))
    implementation(project(":feature:settings"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.kotlinx.serialization.json)

    testImplementation(libs.androidx.test.core)
    testImplementation(libs.roborazzi)
    testImplementation(libs.roborazzi.compose)
    testImplementation(libs.roborazzi.junit.rule)
    testImplementation(libs.hilt.android.testing)
    kspTest(libs.hilt.compiler)

    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.hilt.android.testing)
    kspAndroidTest(libs.hilt.compiler)
    androidTestUtil(libs.androidx.test.orchestrator)
    androidTestUtil(libs.androidx.test.services)

    constraints {
        // AGP pins test classpaths to the app's own versions; androidx.test and Hilt testing need 1.2.0.
        implementation(libs.androidx.concurrent.futures)
        // The ads SDK's Guava asks for 2.11.0, below what the instrumented-test libraries (Espresso) need.
        implementation(libs.errorprone.annotations)
    }
}
