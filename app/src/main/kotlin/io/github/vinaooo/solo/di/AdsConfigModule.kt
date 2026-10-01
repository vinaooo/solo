package io.github.vinaooo.solo.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.vinaooo.solo.BuildConfig
import io.github.vinaooo.solo.core.ads.AdsConfig

/** The ad IDs from the build (test IDs in debug builds); see `AdIds` in build-logic. */
@Module
@InstallIn(SingletonComponent::class)
object AdsConfigModule {
    @Provides
    fun adsConfig() = AdsConfig(
        bannerUnitId = BuildConfig.AD_BANNER_ID,
        testDeviceIds = BuildConfig.AD_TEST_DEVICE_IDS.split(',').filter(String::isNotEmpty),
        simulateEea = BuildConfig.DEBUG,
    )
}
