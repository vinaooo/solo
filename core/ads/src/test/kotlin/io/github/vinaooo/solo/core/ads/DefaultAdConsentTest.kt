package io.github.vinaooo.solo.core.ads

import android.app.Activity
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DefaultAdConsentTest {

    private val activity: Activity = Robolectric.buildActivity(Activity::class.java).get()
    private val config = AdsConfig(bannerUnitId = "banner", testDeviceIds = listOf("PHONE"))

    /** Records the calls; the test decides what the user answered and when each step finishes. */
    private class FakeConsentClient(var canRequestAds: Boolean = false, var privacyOptions: Boolean = false) :
        ConsentClient {
        val calls = mutableListOf<String>()
        var pendingUpdate: (() -> Unit)? = null
        var pendingForm: (() -> Unit)? = null
        var pendingPrivacyOptions: (() -> Unit)? = null

        override fun canRequestAds() = canRequestAds

        override fun privacyOptionsRequired() = privacyOptions

        override fun requestUpdate(activity: Activity, config: AdsConfig, onDone: () -> Unit) {
            calls += "update"
            pendingUpdate = onDone
        }

        override fun showFormIfRequired(activity: Activity, onDone: () -> Unit) {
            calls += "form"
            pendingForm = onDone
        }

        override fun showPrivacyOptions(activity: Activity, onDone: () -> Unit) {
            calls += "privacy options"
            pendingPrivacyOptions = onDone
        }
    }

    private class FakeAdsSdk : AdsSdk {
        val starts = mutableListOf<List<String>>()

        override fun initialize(activity: Activity, testDeviceIds: List<String>) {
            starts += testDeviceIds
        }
    }

    private val client = FakeConsentClient()
    private val sdk = FakeAdsSdk()
    private val consent = DefaultAdConsent(client, sdk, config)

    @Test
    fun `ads wait until the user answers the consent form`() {
        consent.gather(activity)
        client.pendingUpdate!!.invoke()

        client.calls shouldContainExactly listOf("update", "form")
        consent.state.value.canRequestAds shouldBe false
        sdk.starts shouldBe emptyList()

        client.canRequestAds = true
        client.privacyOptions = true
        client.pendingForm!!.invoke()

        consent.state.value shouldBe AdConsentState(canRequestAds = true, privacyOptionsRequired = true)
        sdk.starts shouldContainExactly listOf(listOf("PHONE"))
    }

    @Test
    fun `consent from an earlier session starts ads before the update returns`() {
        client.canRequestAds = true

        consent.gather(activity)

        consent.state.value.canRequestAds shouldBe true
        sdk.starts.size shouldBe 1
    }

    @Test
    fun `the ads SDK starts only once`() {
        client.canRequestAds = true

        consent.gather(activity)
        client.pendingUpdate!!.invoke()
        client.pendingForm!!.invoke()
        consent.gather(activity)

        sdk.starts.size shouldBe 1
    }

    @Test
    fun `refusing consent keeps ads off`() {
        consent.gather(activity)
        client.pendingUpdate!!.invoke()
        client.pendingForm!!.invoke()

        consent.state.value.canRequestAds shouldBe false
        sdk.starts shouldBe emptyList()
    }

    @Test
    fun `changing the choice in privacy options updates the state`() {
        client.canRequestAds = true
        client.privacyOptions = true
        consent.gather(activity)

        consent.showPrivacyOptions(activity)
        client.canRequestAds = false
        client.pendingPrivacyOptions!!.invoke()

        client.calls.last() shouldBe "privacy options"
        consent.state.value shouldBe AdConsentState(canRequestAds = false, privacyOptionsRequired = true)
    }
}
