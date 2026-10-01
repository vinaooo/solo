package io.github.vinaooo.solo.buildlogic

/**
 * AdMob IDs for one build: the [appId] (with a `~`) goes into the manifest, the [bannerId] (an ad unit, with a `/`)
 * into BuildConfig. Debug builds always use [TEST]; release builds use [resolve], or [TEST] when nothing is set.
 */
data class AdIds(val appId: String, val bannerId: String) {
    private enum class Setting(val property: String, val environmentVariable: String) {
        APP_ID("solo.ads.appId", "SOLO_ADS_APP_ID"),
        BANNER_ID("solo.ads.bannerId", "SOLO_ADS_BANNER_ID"),
    }

    companion object {
        /** Google's sample IDs, which always serve test ads. */
        val TEST = AdIds(
            appId = "ca-app-pub-3940256099942544~3347511713",
            bannerId = "ca-app-pub-3940256099942544/9214589741",
        )

        val environmentVariables = Setting.entries.map { it.environmentVariable }

        private val appIdFormat = Regex("""ca-app-pub-\d+~\d+""")
        private val adUnitFormat = Regex("""ca-app-pub-\d+/\d+""")
        private const val TEST_DEVICES_PROPERTY = "solo.ads.testDeviceIds"

        /**
         * The release IDs from [properties] (local.properties) or [environment] (CI), environment first; null when
         * neither is set. A partial or malformed setup fails, because a release with a wrong ID earns nothing.
         */
        fun resolve(properties: Map<String, String>, environment: Map<String, String>): AdIds? {
            val values = Setting.entries.associateWith { setting ->
                (environment[setting.environmentVariable] ?: properties[setting.property])?.trim()?.ifEmpty { null }
            }
            val missing = values.filterValues { it == null }.keys
            if (missing.size == Setting.entries.size) return null
            check(missing.isEmpty()) {
                "AdMob IDs are partly set; also set " +
                    missing.joinToString { "${it.property} (or ${it.environmentVariable})" } + "."
            }
            val appId = values.getValue(Setting.APP_ID)!!
            val bannerId = values.getValue(Setting.BANNER_ID)!!
            check(appIdFormat.matches(appId)) { "solo.ads.appId \"$appId\" is not an app ID (ca-app-pub-…~…)." }
            check(adUnitFormat.matches(bannerId)) {
                "solo.ads.bannerId \"$bannerId\" is not an ad unit ID (ca-app-pub-…/…)."
            }
            return AdIds(appId, bannerId)
        }

        /**
         * Hashed IDs of devices that always get test ads, even from a release build with real IDs, so the
         * developer's own taps never count. AdMob and the consent SDK print the hash in logcat.
         */
        fun testDevices(properties: Map<String, String>): List<String> =
            properties[TEST_DEVICES_PROPERTY].orEmpty().split(',').map(String::trim).filter(String::isNotEmpty)
    }
}
