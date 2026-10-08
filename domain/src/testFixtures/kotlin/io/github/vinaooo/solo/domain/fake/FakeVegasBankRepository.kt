package io.github.vinaooo.solo.domain.fake

import io.github.vinaooo.solo.domain.repository.VegasBankRepository
import kotlinx.coroutines.flow.MutableStateFlow

class FakeVegasBankRepository(initial: Int = 0) : VegasBankRepository {
    override val bank = MutableStateFlow(initial)

    override suspend fun set(dollars: Int) {
        bank.value = dollars
    }
}
