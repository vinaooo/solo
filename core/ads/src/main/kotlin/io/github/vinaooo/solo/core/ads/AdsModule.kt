package io.github.vinaooo.solo.core.ads

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** Public so app tests can swap it for fakes with `@TestInstallIn`; [AdsConfig] is provided by the app. */
@Module
@InstallIn(SingletonComponent::class)
interface AdsModule {
    @Binds
    fun adBannerProvider(impl: AdMobBanner): AdBannerProvider

    @Binds
    fun adConsent(impl: DefaultAdConsent): AdConsent

    @Binds
    fun consentClient(impl: UmpConsentClient): ConsentClient

    @Binds
    fun adsSdk(impl: MobileAdsSdk): AdsSdk
}
