package org.mromichov.guessthesong.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.io.IOException
import javax.inject.Inject

class AppDataStore @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    private companion object {
        val KEY_ROUNDS_NUMBER = intPreferencesKey("rounds_number")
        val KEY_SNIPPET_LENGTH = longPreferencesKey("snippet_length")
    }

    val roundsNumber = dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences -> preferences[KEY_ROUNDS_NUMBER] ?: 5 }

    val snippetLength = dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            preferences[KEY_SNIPPET_LENGTH] ?: 3000L
        }

    suspend fun setRoundsNumber(minimalRoundsNumber: Int) {
        dataStore.edit { preferences ->
            preferences[KEY_ROUNDS_NUMBER] = minimalRoundsNumber
        }
    }

    suspend fun setSnippetLength(ms: Long) {
        dataStore.edit { preferences ->
            preferences[KEY_SNIPPET_LENGTH] = ms
        }
    }
}