package io.github.vinaooo.solo

import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.github.vinaooo.vinkit.ads.AdBannerProvider
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule

/**
 * The real app on a device or emulator (with fake ads, see [FakeAdsModule]). The orchestrator clears the app's data
 * before each test, so every test starts on a fresh deal with Draw 1: 24 cards in the stock.
 */
@HiltAndroidTest
class GameOnDeviceTest {

    private val hilt = HiltAndroidRule(this)
    private val compose = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val rules: TestRule = RuleChain.outerRule(hilt).around(compose)

    private fun stockShows(cards: Int) {
        compose.onNodeWithContentDescription("Stock, $cards cards").assertExists()
    }

    private fun drawCard(cardsBefore: Int) {
        compose.onNodeWithContentDescription("Stock, $cardsBefore cards").performClick()
        compose.waitForIdle()
    }

    private fun pressBack() {
        compose.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        compose.waitForIdle()
    }

    @Test
    fun a_fresh_start_shows_a_new_deal_with_the_banner_below() {
        compose.onNodeWithContentDescription("Score, 0").assertExists()
        compose.onNodeWithContentDescription("Moves, 0").assertExists()
        stockShows(24)
        compose.onNodeWithTag(AdBannerProvider.TEST_TAG).assertExists()
    }

    @Test
    fun drawing_a_card_counts_a_move_and_undo_puts_it_back() {
        drawCard(cardsBefore = 24)
        stockShows(23)
        compose.onNodeWithContentDescription("Moves, 1").assertExists()

        compose.onNodeWithContentDescription("Undo").performClick()
        compose.waitForIdle()

        stockShows(24)
    }

    @Test
    fun the_game_survives_the_activity_being_recreated() {
        drawCard(cardsBefore = 24)

        // What rotation, a theme change or the system reclaiming the activity does.
        compose.activityRule.scenario.recreate()

        stockShows(23)
        compose.onNodeWithContentDescription("Moves, 1").assertExists()
    }

    @Test
    fun scores_and_settings_open_without_the_banner_and_back_returns_to_the_same_game() {
        drawCard(cardsBefore = 24)

        compose.onNodeWithContentDescription("Scores").performClick()
        compose.onNodeWithText("Scores").assertExists()
        compose.onNodeWithTag(AdBannerProvider.TEST_TAG).assertDoesNotExist()
        pressBack()

        compose.onNodeWithContentDescription("Settings").performClick()
        compose.onNodeWithText("Settings").assertExists()
        compose.onNodeWithText("Privacy policy").assertExists()
        compose.onNodeWithTag(AdBannerProvider.TEST_TAG).assertDoesNotExist()
        pressBack()

        stockShows(23)
        compose.onNodeWithTag(AdBannerProvider.TEST_TAG).assertExists()
    }
}
