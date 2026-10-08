package io.github.vinaooo.solo.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import io.github.vinaooo.solo.domain.repository.VegasBankRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Cumulative Vegas's balance, in the settings DataStore (vinkit's stats keep no game-specific values). */
class DataStoreVegasBankRepository @Inject constructor(private val dataStore: DataStore<Preferences>) :
    VegasBankRepository {

    override val bank: Flow<Int> = dataStore.data.map { it[KEY] ?: 0 }

    override suspend fun set(dollars: Int) {
        dataStore.edit { it[KEY] = dollars }
    }

    private companion object {
        val KEY = intPreferencesKey("vegas_bank")
    }
}
