package io.github.vinaooo.solo.data.system

import io.github.vinaooo.solo.domain.repository.Clock
import io.github.vinaooo.solo.domain.repository.SeedSource
import javax.inject.Inject
import kotlin.random.Random

class RandomSeedSource @Inject constructor() : SeedSource {
    override fun nextSeed(): Long = Random.nextLong()
}

class SystemClock @Inject constructor() : Clock {
    override fun nowMillis(): Long = System.currentTimeMillis()
}
