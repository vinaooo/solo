package io.github.vinaooo.solo.buildlogic

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import java.io.File
import java.nio.file.Files
import org.junit.jupiter.api.Test

class ReleaseSigningTest {

    private val rootDir: File = Files.createTempDirectory("solo-root").toFile()
    private val keystore = File(rootDir, "keys/upload.jks").apply {
        parentFile.mkdirs()
        writeText("not a real keystore")
    }

    private val fromProperties = mapOf(
        "solo.signing.storeFile" to "keys/upload.jks",
        "solo.signing.storePassword" to "store-secret",
        "solo.signing.keyAlias" to "upload",
        "solo.signing.keyPassword" to "key-secret",
    )

    @Test
    fun `nothing configured leaves the release build unsigned`() {
        ReleaseSigning.resolve(properties = emptyMap(), environment = emptyMap(), rootDir = rootDir).shouldBeNull()
    }

    @Test
    fun `local properties configure the upload key, with the keystore path relative to the project`() {
        ReleaseSigning.resolve(fromProperties, environment = emptyMap(), rootDir = rootDir) shouldBe
            ReleaseSigning(keystore, "store-secret", "upload", "key-secret")
    }

    @Test
    fun `environment variables configure it on CI, and win over local properties`() {
        val environment = mapOf(
            "SOLO_SIGNING_STORE_FILE" to keystore.absolutePath,
            "SOLO_SIGNING_STORE_PASSWORD" to "ci-store-secret",
            "SOLO_SIGNING_KEY_ALIAS" to "ci-upload",
            "SOLO_SIGNING_KEY_PASSWORD" to "ci-key-secret",
        )

        ReleaseSigning.resolve(properties = emptyMap(), environment = environment, rootDir = rootDir) shouldBe
            ReleaseSigning(keystore, "ci-store-secret", "ci-upload", "ci-key-secret")
        ReleaseSigning.resolve(fromProperties, mapOf("SOLO_SIGNING_KEY_ALIAS" to "ci-upload"), rootDir)?.keyAlias shouldBe
            "ci-upload"
    }

    @Test
    fun `a half-done setup fails and names what is missing`() {
        val error = shouldThrow<IllegalStateException> {
            ReleaseSigning.resolve(fromProperties - "solo.signing.keyPassword", emptyMap(), rootDir)
        }

        error.message shouldContain "solo.signing.keyPassword"
        error.message shouldContain "SOLO_SIGNING_KEY_PASSWORD"
    }

    @Test
    fun `a blank value counts as missing`() {
        shouldThrow<IllegalStateException> {
            ReleaseSigning.resolve(fromProperties + ("solo.signing.storePassword" to " "), emptyMap(), rootDir)
        }.message shouldContain "solo.signing.storePassword"
    }

    @Test
    fun `a keystore path that does not exist fails`() {
        shouldThrow<IllegalStateException> {
            ReleaseSigning.resolve(fromProperties + ("solo.signing.storeFile" to "keys/missing.jks"), emptyMap(), rootDir)
        }.message shouldContain "missing.jks"
    }

    @Test
    fun `passwords never show up when the signing setup is printed`() {
        val signing = ReleaseSigning(keystore, "store-secret", "upload", "key-secret").toString()

        (signing.contains("store-secret") || signing.contains("key-secret")) shouldBe false
    }
}
