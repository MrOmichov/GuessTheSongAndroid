package org.mromichov.guessthesong.data.repository

import kotlinx.coroutines.flow.Flow
import org.mromichov.guessthesong.data.local.AppDataStore
import org.mromichov.guessthesong.domain.repository.SettingsRepository
import javax.inject.Inject

class SettingsRepositoryImpl @Inject constructor(
    private val appDataStore: AppDataStore
) : SettingsRepository {
    override val roundsNumber: Flow<Int> = appDataStore.roundsNumber
    override val snippetLength: Flow<Long> = appDataStore.snippetLength

    override suspend fun setRoundsNumber(roundsNumber: Int) {
        appDataStore.setRoundsNumber(roundsNumber)
    }

    override suspend fun setSnippetLength(ms: Long) {
        appDataStore.setSnippetLength(ms)
    }
}