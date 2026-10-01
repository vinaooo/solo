package io.github.vinaooo.solo

import android.app.Activity
import dagger.Binds
import dagger.Module
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import io.github.vinaooo.solo.core.ads.AdBannerProvider
import io.github.vinaooo.solo.core.ads.AdConsent
import io.github.vinaooo.solo.core.ads.AdConsentState
import io.github.vinaooo.solo.core.ads.AdsModule
import io.github.vinaooo.solo.core.ads.PlaceholderAdBanner
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow

/** Keeps the real ads and consent SDKs out of app tests. */
@Module
@TestInstallIn(components = [SingletonComponent::class], replaces = [AdsModule::class])
interface FakeAdsModule {
    @Binds
    fun adBannerProvider(impl: PlaceholderAdBanner): AdBannerProvider

    @Binds
    fun adConsent(impl: FakeAdConsent): AdConsent
}

@Singleton
class FakeAdConsent @Inject constructor() : AdConsent {
    override val state = MutableStateFlow(AdConsentState())
    var gathered = 0

    override fun gather(activity: Activity) {
        gathered++
    }

    override fun showPrivacyOptions(activity: Activity) = Unit
}
