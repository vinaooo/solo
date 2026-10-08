package io.github.vinaooo.solo

import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import io.github.vinaooo.vinkit.ads.AdBannerProvider
import io.kotest.matchers.shouldBe
import javax.inject.Inject
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** The whole app, wired by Hilt with fake ads (see [FakeAdsModule]): the banner belongs to the game screen only. */
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
@Config(application = HiltTestApplication::class)
class AdBannerGameScreenOnlyTest {

    private val hilt = HiltAndroidRule(this)
    private val compose = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val rules: TestRule = RuleChain.outerRule(hilt).around(compose)

    @Inject lateinit var consent: FakeAdConsent

    @Test
    fun `the banner is on the game screen and not on scores or settings`() {
        compose.onNodeWithContentDescription("Score, 0").assertExists()
        compose.onNodeWithTag(AdBannerProvider.TEST_TAG).assertExists()

        compose.onNodeWithContentDescription("Scores").performClick()
        compose.onNodeWithText("Scores").assertExists()
        compose.onNodeWithTag(AdBannerProvider.TEST_TAG).assertDoesNotExist()

        compose.onNodeWithContentDescription("Back").performClick()
        compose.onNodeWithContentDescription("Settings").performClick()
        compose.onNodeWithText("Settings").assertExists()
        compose.onNodeWithTag(AdBannerProvider.TEST_TAG).assertDoesNotExist()
    }

    @Test
    fun `consent is gathered once when the app starts`() {
        hilt.inject()

        consent.gathered shouldBe 1
    }
}
