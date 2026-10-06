package io.github.vinaooo.solo.domain.deal

import io.github.vinaooo.solo.domain.fake.FakeSettingsRepository
import io.github.vinaooo.solo.domain.model.Difficulty
import io.github.vinaooo.solo.domain.model.DrawMode
import io.github.vinaooo.solo.domain.model.GameMode
import io.github.vinaooo.solo.domain.model.Settings
import io.kotest.matchers.collections.shouldBeUnique
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class DealPickerTest {

    private val settings = FakeSettingsRepository()
    private val picker = DealPicker(seedSource = { 1_234 }, settings)

    @Test
    fun `hard deals a random seed and leaves the cursor alone`() = runTest {
        picker.next(DrawMode.ONE, GameMode.STANDARD, Difficulty.HARD) shouldBe 1_234
        settings.current.value.dealCursor shouldBe null
    }

    @Test
    fun `the first listed deal starts at a random place, then the cursor moves on`() = runTest {
        picker.next(DrawMode.ONE, GameMode.STANDARD, Difficulty.NORMAL) shouldBe
            WinnableDeals.seedAt(1_234, DrawMode.ONE, GameMode.STANDARD, Difficulty.NORMAL)
        settings.current.value.dealCursor shouldBe 1_235
    }

    @Test
    fun `no deal comes back before the list has gone round`() = runTest {
        settings.current.value = Settings(dealCursor = 0)
        val size = WinnableDeals.seeds(DrawMode.ONE, limitedPasses = true, Difficulty.EASY).size

        val seeds = List(size) { picker.next(DrawMode.ONE, GameMode.VEGAS, Difficulty.EASY) }

        seeds.shouldBeUnique()
        picker.next(DrawMode.ONE, GameMode.VEGAS, Difficulty.EASY) shouldBe seeds.first()
    }
}
