package io.github.vinaooo.solo.core.ads

/**
 * What the app passes in from its build: the banner ad unit, the hashed IDs of devices that always get test ads,
 * and whether to make the consent SDK behave as in the EEA (debug builds), so the consent form can be tried anywhere.
 */
data class AdsConfig(
    val bannerUnitId: String,
    val testDeviceIds: List<String> = emptyList(),
    val simulateEea: Boolean = false,
)
