package io.github.vinaooo.solo.feature.settings

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import io.kotest.matchers.shouldBe
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
class PrivacyPolicyUrlTest {

    private fun url() = ApplicationProvider.getApplicationContext<Context>().getString(R.string.privacy_policy_url)

    @Test
    fun `the privacy policy opens at the English text`() {
        url() shouldBe "https://vinaooo.github.io/solo/privacy.html#en"
    }

    @Test
    @Config(qualifiers = "pt-rBR")
    fun `in Brazilian Portuguese the privacy policy opens at the Portuguese text`() {
        url() shouldBe "https://vinaooo.github.io/solo/privacy.html#pt-br"
    }
}
