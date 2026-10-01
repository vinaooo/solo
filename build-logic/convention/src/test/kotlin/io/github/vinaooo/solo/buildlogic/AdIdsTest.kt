package io.github.vinaooo.solo.buildlogic

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.Test

class AdIdsTest {

    private val appId = "ca-app-pub-1234567890123456~1234567890"
    private val bannerId = "ca-app-pub-1234567890123456/0987654321"
    private val fromProperties = mapOf("solo.ads.appId" to appId, "solo.ads.bannerId" to bannerId)

    @Test
    fun `nothing configured leaves the release build on Google's test ads`() {
        AdIds.resolve(properties = emptyMap(), environment = emptyMap()).shouldBeNull()
    }

    @Test
    fun `the release IDs come from local properties`() {
        AdIds.resolve(fromProperties, environment = emptyMap()) shouldBe AdIds(appId, bannerId)
    }

    @Test
    fun `environment variables win over local properties`() {
        val ciBanner = "ca-app-pub-1234567890123456/1111111111"
        AdIds.resolve(fromProperties, mapOf("SOLO_ADS_BANNER_ID" to ciBanner)) shouldBe AdIds(appId, ciBanner)
    }

    @Test
    fun `blank values count as missing`() {
        AdIds.resolve(mapOf("solo.ads.appId" to " ", "solo.ads.bannerId" to ""), emptyMap()).shouldBeNull()
    }

    @Test
    fun `a partial setup fails and names what is missing`() {
        shouldThrow<IllegalStateException> {
            AdIds.resolve(mapOf("solo.ads.appId" to appId), emptyMap())
        }.message shouldContain "solo.ads.bannerId"
    }

    @Test
    fun `an ad unit ID given as the app ID fails, so swapped IDs are caught`() {
        shouldThrow<IllegalStateException> {
            AdIds.resolve(mapOf("solo.ads.appId" to bannerId, "solo.ads.bannerId" to bannerId), emptyMap())
        }.message shouldContain "~"
    }

    @Test
    fun `an app ID given as the banner ID fails`() {
        shouldThrow<IllegalStateException> {
            AdIds.resolve(mapOf("solo.ads.appId" to appId, "solo.ads.bannerId" to appId), emptyMap())
        }.message shouldContain "/"
    }

    @Test
    fun `Google's test IDs have the shape of real ones`() {
        AdIds.resolve(
            mapOf("solo.ads.appId" to AdIds.TEST.appId, "solo.ads.bannerId" to AdIds.TEST.bannerId),
            emptyMap(),
        ) shouldBe AdIds.TEST
    }

    @Test
    fun `test devices are a comma-separated list`() {
        AdIds.testDevices(mapOf("solo.ads.testDeviceIds" to " ABC123, DEF456 ,")) shouldBe listOf("ABC123", "DEF456")
        AdIds.testDevices(emptyMap()).shouldBeEmpty()
    }
}
