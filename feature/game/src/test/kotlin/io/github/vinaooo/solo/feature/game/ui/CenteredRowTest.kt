package io.github.vinaooo.solo.feature.game.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import io.kotest.matchers.shouldBe
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CenteredRowTest {

    @get:Rule
    val compose = createComposeRule()

    private fun show(startWidth: Int, endWidth: Int) {
        compose.setContent {
            CenteredRow(
                modifier = Modifier.width(300.dp).testTag("row"),
                start = { Box(Modifier.size(startWidth.dp, 20.dp).testTag("start")) },
                center = { Box(Modifier.fillMaxSize().testTag("center")) },
                end = { Box(Modifier.size(endWidth.dp, 20.dp).testTag("end")) },
            )
        }
    }

    private fun bounds(tag: String) = compose.onNodeWithTag(tag).getBoundsInRoot()

    @Test
    fun `the center is centered when the start side is wider`() {
        show(startWidth = 80, endWidth = 40)

        bounds("center").left shouldBe 80.dp
        bounds("center").right shouldBe 220.dp
    }

    @Test
    fun `the center is centered when the end side is wider`() {
        show(startWidth = 40, endWidth = 100)

        bounds("center").left shouldBe 100.dp
        bounds("center").right shouldBe 200.dp
    }

    @Test
    fun `the sides hug the edges and are vertically centered`() {
        show(startWidth = 40, endWidth = 60)
        val row = bounds("row")

        bounds("start").left shouldBe 0.dp
        bounds("end").right shouldBe 300.dp
        bounds("start").top shouldBe (row.bottom - row.top - 20.dp) / 2
        bounds("end").top shouldBe (row.bottom - row.top - 20.dp) / 2
    }
}
