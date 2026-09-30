package io.github.vinaooo.solo.core.ui

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class TimeFormatTest {

    @Test
    fun `formats minutes and seconds, adding hours only when needed`() {
        formatElapsed(0) shouldBe "0:00"
        formatElapsed(65) shouldBe "1:05"
        formatElapsed(599) shouldBe "9:59"
        formatElapsed(3_600) shouldBe "1:00:00"
        formatElapsed(3_725) shouldBe "1:02:05"
    }
}
