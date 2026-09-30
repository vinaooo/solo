package io.github.vinaooo.solo.core.ads

import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import io.github.vinaooo.solo.core.designsystem.theme.SoloTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PlaceholderAdBannerTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `placeholder takes the standard banner height and is labelled as an ad`() {
        compose.setContent { SoloTheme { PlaceholderAdBanner().Banner() } }

        compose.onNodeWithTag(AdBannerProvider.TEST_TAG).assertHeightIsEqualTo(50.dp)
        compose.onNodeWithText("Ad").assertExists()
    }
}
