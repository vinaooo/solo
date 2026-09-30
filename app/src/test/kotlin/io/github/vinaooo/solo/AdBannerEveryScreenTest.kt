package io.github.vinaooo.solo

import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import io.github.vinaooo.solo.core.ads.AdBannerProvider
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** The whole app, wired by Hilt: the ad banner stays at the bottom whichever screen is open. */
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
@Config(application = HiltTestApplication::class)
class AdBannerEveryScreenTest {

    private val hilt = HiltAndroidRule(this)
    private val compose = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val rules: TestRule = RuleChain.outerRule(hilt).around(compose)

    private fun assertBannerShown() {
        compose.onNodeWithTag(AdBannerProvider.TEST_TAG).assertExists()
    }

    @Test
    fun `the banner is on the game, scores and settings screens`() {
        compose.onNodeWithContentDescription("Score, 0").assertExists()
        assertBannerShown()

        compose.onNodeWithContentDescription("Scores").performClick()
        compose.onNodeWithText("Scores").assertExists()
        assertBannerShown()

        compose.onNodeWithContentDescription("Back").performClick()
        compose.onNodeWithContentDescription("Settings").performClick()
        compose.onNodeWithText("Settings").assertExists()
        assertBannerShown()
    }
}
