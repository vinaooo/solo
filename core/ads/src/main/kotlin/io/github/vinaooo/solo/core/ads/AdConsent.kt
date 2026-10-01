package io.github.vinaooo.solo.core.ads

import android.app.Activity
import kotlinx.coroutines.flow.StateFlow

/** Whether ads may be requested yet, and whether the user must be offered a way to change their consent. */
data class AdConsentState(val canRequestAds: Boolean = false, val privacyOptionsRequired: Boolean = false)

/** Asks for ad consent where the law requires it (GDPR, US states) and starts the ads SDK once it is given. */
interface AdConsent {
    val state: StateFlow<AdConsentState>

    /** Refreshes the consent status and shows the consent form if one is required. Call on every app start. */
    fun gather(activity: Activity)

    /** Reopens the consent form from Settings, so the user can change their choice. */
    fun showPrivacyOptions(activity: Activity)
}
