package io.github.vinaooo.solo.core.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.ump.ConsentDebugSettings
import com.google.android.ump.ConsentInformation.PrivacyOptionsRequirementStatus
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private const val TAG = "SoloAds"

/** [ConsentClient] on Google's User Messaging Platform. */
class UmpConsentClient @Inject constructor(@ApplicationContext context: Context) : ConsentClient {
    // Holds what was answered in earlier sessions, so it can be read before this session's update.
    private val information = UserMessagingPlatform.getConsentInformation(context)

    override fun canRequestAds(): Boolean = information.canRequestAds()

    override fun privacyOptionsRequired(): Boolean =
        information.privacyOptionsRequirementStatus == PrivacyOptionsRequirementStatus.REQUIRED

    override fun requestUpdate(activity: Activity, config: AdsConfig, onDone: () -> Unit) {
        val debug = ConsentDebugSettings.Builder(activity).apply {
            if (config.simulateEea) setDebugGeography(ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_EEA)
            config.testDeviceIds.forEach(::addTestDeviceHashedId)
        }.build()
        val parameters = ConsentRequestParameters.Builder().setConsentDebugSettings(debug).build()
        information.requestConsentInfoUpdate(
            activity,
            parameters,
            { onDone() },
            { error ->
                Log.w(TAG, "Consent update failed: ${error.message}")
                onDone()
            },
        )
    }

    override fun showFormIfRequired(activity: Activity, onDone: () -> Unit) {
        UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { error ->
            error?.let { Log.w(TAG, "Consent form failed: ${it.message}") }
            onDone()
        }
    }

    override fun showPrivacyOptions(activity: Activity, onDone: () -> Unit) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity) { error ->
            error?.let { Log.w(TAG, "Privacy options form failed: ${it.message}") }
            onDone()
        }
    }
}

/** [AdsSdk] on the Google Mobile Ads SDK, started off the main thread as Google recommends. */
class MobileAdsSdk @Inject constructor() : AdsSdk {
    override fun initialize(activity: Activity, testDeviceIds: List<String>) {
        val context = activity.applicationContext
        MobileAds.setRequestConfiguration(RequestConfiguration.Builder().setTestDeviceIds(testDeviceIds).build())
        CoroutineScope(Dispatchers.IO).launch { MobileAds.initialize(context) }
    }
}
