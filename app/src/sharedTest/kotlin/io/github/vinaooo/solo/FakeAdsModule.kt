package io.github.vinaooo.solo

import android.app.Activity
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import io.github.vinaooo.solo.di.AdsModule
import io.github.vinaooo.vinkit.ads.AdBannerProvider
import io.github.vinaooo.vinkit.ads.AdConsent
import io.github.vinaooo.vinkit.ads.AdConsentState
import io.github.vinaooo.vinkit.ads.PlaceholderAdBanner
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow

/** Keeps the real ads and consent SDKs out of app tests. */
@Module
@TestInstallIn(components = [SingletonComponent::class], replaces = [AdsModule::class])
interface FakeAdsModule {
    @Binds
    fun adConsent(impl: FakeAdConsent): AdConsent

    companion object {
        @Provides
        fun adBannerProvider(): AdBannerProvider = PlaceholderAdBanner()
    }
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
