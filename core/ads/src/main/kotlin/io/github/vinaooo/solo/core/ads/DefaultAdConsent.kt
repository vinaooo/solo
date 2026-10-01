package io.github.vinaooo.solo.core.ads

import android.app.Activity
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Google's recommended order: update the consent status, show the form if needed, and only then start the ads SDK.
 * Consent given in an earlier session lets ads start right away, before the update returns.
 */
@Singleton
class DefaultAdConsent @Inject constructor(
    private val client: ConsentClient,
    private val sdk: AdsSdk,
    private val config: AdsConfig,
) : AdConsent {
    private val mutableState = MutableStateFlow(AdConsentState())
    override val state: StateFlow<AdConsentState> = mutableState.asStateFlow()
    private var sdkStarted = false

    override fun gather(activity: Activity) {
        refresh(activity)
        client.requestUpdate(activity, config) {
            client.showFormIfRequired(activity) { refresh(activity) }
        }
    }

    override fun showPrivacyOptions(activity: Activity) {
        client.showPrivacyOptions(activity) { refresh(activity) }
    }

    private fun refresh(activity: Activity) {
        val canRequestAds = client.canRequestAds()
        mutableState.value = AdConsentState(canRequestAds, client.privacyOptionsRequired())
        if (canRequestAds && !sdkStarted) {
            sdkStarted = true
            sdk.initialize(activity, config.testDeviceIds)
        }
    }
}
