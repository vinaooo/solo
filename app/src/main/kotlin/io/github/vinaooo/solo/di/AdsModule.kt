package io.github.vinaooo.solo.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.vinaooo.solo.BuildConfig
import io.github.vinaooo.vinkit.ads.AdBannerProvider
import io.github.vinaooo.vinkit.ads.AdConsent
import io.github.vinaooo.vinkit.ads.AdMobBanner
import io.github.vinaooo.vinkit.ads.AdsConfig
import io.github.vinaooo.vinkit.ads.DefaultAdConsent
import io.github.vinaooo.vinkit.ads.MobileAdsSdk
import io.github.vinaooo.vinkit.ads.UmpConsentClient
import javax.inject.Singleton

/** vinkit's banner and consent, with the ad IDs from the build (test IDs in debug builds). App tests replace it. */
@Module
@InstallIn(SingletonComponent::class)
object AdsModule {
    @Provides
    fun adsConfig() = AdsConfig(
        bannerUnitId = BuildConfig.AD_BANNER_ID,
        testDeviceIds = BuildConfig.AD_TEST_DEVICE_IDS.split(',').filter(String::isNotEmpty),
        simulateEea = BuildConfig.DEBUG,
    )

    @Provides
    @Singleton
    fun adConsent(@ApplicationContext context: Context, config: AdsConfig): AdConsent =
        DefaultAdConsent(UmpConsentClient(context), MobileAdsSdk(), config)

    @Provides
    fun adBanner(config: AdsConfig, consent: AdConsent): AdBannerProvider = AdMobBanner(config, consent)
}
