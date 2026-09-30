package io.github.vinaooo.solo.buildlogic

import java.io.File

/**
 * The upload key that signs release builds. It comes from `local.properties` on a developer machine or from
 * environment variables on CI (GitHub Secrets), never from the repository.
 */
data class ReleaseSigning(val storeFile: File, val storePassword: String, val keyAlias: String, val keyPassword: String) {

    override fun toString() = "ReleaseSigning(storeFile=$storeFile, keyAlias=$keyAlias)"

    private enum class Setting(val property: String, val environmentVariable: String) {
        STORE_FILE("solo.signing.storeFile", "SOLO_SIGNING_STORE_FILE"),
        STORE_PASSWORD("solo.signing.storePassword", "SOLO_SIGNING_STORE_PASSWORD"),
        KEY_ALIAS("solo.signing.keyAlias", "SOLO_SIGNING_KEY_ALIAS"),
        KEY_PASSWORD("solo.signing.keyPassword", "SOLO_SIGNING_KEY_PASSWORD"),
    }

    companion object {
        /** Every environment variable [resolve] reads, so callers can look up only those. */
        val environmentVariables: List<String> = Setting.entries.map { it.environmentVariable }

        /**
         * The signing setup, or null when none is configured, which leaves release builds unsigned. Each setting is
         * read from [environment] first, then [properties]. A half-done setup fails instead of quietly producing an
         * unsigned build. A relative keystore path is resolved against [rootDir].
         */
        fun resolve(properties: Map<String, String>, environment: Map<String, String>, rootDir: File): ReleaseSigning? {
            val values = Setting.entries.associateWith { setting ->
                (environment[setting.environmentVariable] ?: properties[setting.property])?.takeIf { it.isNotBlank() }
            }
            if (values.values.all { it == null }) return null
            val missing = values.filterValues { it == null }.keys
            check(missing.isEmpty()) {
                "Release signing is only partly configured. Missing: " +
                    missing.joinToString { "${it.property} (or ${it.environmentVariable})" }
            }
            val storeFile = File(values.getValue(Setting.STORE_FILE)!!).let { if (it.isAbsolute) it else File(rootDir, it.path) }
            check(storeFile.isFile) { "Release keystore not found: $storeFile" }
            return ReleaseSigning(
                storeFile = storeFile,
                storePassword = values.getValue(Setting.STORE_PASSWORD)!!,
                keyAlias = values.getValue(Setting.KEY_ALIAS)!!,
                keyPassword = values.getValue(Setting.KEY_PASSWORD)!!,
            )
        }
    }
}
