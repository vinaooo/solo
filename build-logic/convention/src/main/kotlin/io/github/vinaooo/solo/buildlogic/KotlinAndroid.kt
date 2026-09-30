package io.github.vinaooo.solo.buildlogic

import com.android.build.api.dsl.CommonExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmCompilerOptions
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask

internal val JAVA_VERSION = JavaVersion.VERSION_17
internal val JVM_TARGET = JvmTarget.JVM_17

internal fun Project.configureKotlinAndroid(commonExtension: CommonExtension) {
    commonExtension.apply {
        compileSdk = libs.version("compileSdk").toInt()
        defaultConfig.minSdk = libs.version("minSdk").toInt()
        compileOptions.sourceCompatibility = JAVA_VERSION
        compileOptions.targetCompatibility = JAVA_VERSION
        testOptions.unitTests.isIncludeAndroidResources = true
        testOptions.unitTests.isReturnDefaultValues = true
    }
    configureKotlinCompiler()
}

internal fun Project.configureKotlinCompiler() {
    tasks.withType<KotlinCompilationTask<*>>().configureEach {
        compilerOptions {
            allWarningsAsErrors.set(true)
            if (this is KotlinJvmCompilerOptions) jvmTarget.set(JVM_TARGET)
            // kotlinx-coroutines-test (setMain, runCurrent, advanceTimeBy) is still marked experimental.
            if (name.contains("Test")) optIn.add("kotlinx.coroutines.ExperimentalCoroutinesApi")
        }
    }
}

/** JUnit 5 on the JUnit Platform, with the vintage engine for Robolectric (JUnit 4) tests. */
internal fun Project.configureJUnitPlatform() {
    dependencies {
        "testImplementation"(platform(libs.library("junit5-bom")))
        "testImplementation"(libs.library("junit5-jupiter"))
        "testImplementation"(libs.library("kotest-assertions-core"))
        "testRuntimeOnly"(libs.library("junit5-vintage-engine"))
        "testRuntimeOnly"(libs.library("junit5-platform-launcher"))
    }
    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
        failOnNoDiscoveredTests.set(false)
        maxHeapSize = "1g"
        // Robolectric's SDK 36 runtime reaches into JDK internals.
        jvmArgs("--add-exports=java.base/jdk.internal.access=ALL-UNNAMED", "--add-opens=java.base/jdk.internal.access=ALL-UNNAMED")
        testLogging { events("failed") }
    }
}
