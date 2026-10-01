package io.github.vinaooo.solo.core.ads

import android.app.Activity

/** The consent SDK, behind an interface so [DefaultAdConsent]'s order of calls can be tested with a fake. */
interface ConsentClient {
    fun canRequestAds(): Boolean

    fun privacyOptionsRequired(): Boolean

    /** Updates the consent status; [onDone] runs whether or not the update succeeded. */
    fun requestUpdate(activity: Activity, config: AdsConfig, onDone: () -> Unit)

    /** Shows the consent form if the user still has to answer it; [onDone] runs when it closes or isn't needed. */
    fun showFormIfRequired(activity: Activity, onDone: () -> Unit)

    fun showPrivacyOptions(activity: Activity, onDone: () -> Unit)
}

/** The ads SDK's start-up, behind an interface for the same reason as [ConsentClient]. */
interface AdsSdk {
    fun initialize(activity: Activity, testDeviceIds: List<String>)
}
