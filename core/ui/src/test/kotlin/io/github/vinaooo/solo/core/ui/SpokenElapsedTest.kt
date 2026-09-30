package io.github.vinaooo.solo.core.ui

import androidx.compose.ui.test.junit4.v2.createComposeRule
import io.kotest.matchers.collections.shouldContainExactly
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
class SpokenElapsedTest {

    @get:Rule
    val compose = createComposeRule()

    private fun spoken(vararg seconds: Long): List<String> {
        val texts = mutableListOf<String>()
        compose.setContent { seconds.forEach { texts += spokenElapsed(it) } }
        compose.waitForIdle()
        return texts
    }

    @Test
    fun `durations are spoken in words, leaving out the parts that are zero`() {
        spoken(0, 1, 65, 120, 3_600, 3_725) shouldContainExactly listOf(
            "0 seconds",
            "1 second",
            "1 minute 5 seconds",
            "2 minutes",
            "1 hour",
            "1 hour 2 minutes 5 seconds",
        )
    }

    @Test
    @Config(qualifiers = "pt-rBR")
    fun `durations are spoken in Brazilian Portuguese`() {
        spoken(1, 7_322) shouldContainExactly listOf("1 segundo", "2 horas 2 minutos 2 segundos")
    }
}
