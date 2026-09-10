package org.mromichov.guessthesong.data.repository

import kotlinx.coroutines.flow.Flow
import org.mromichov.guessthesong.data.local.AppDataStore
import org.mromichov.guessthesong.domain.repository.SettingsRepository
import javax.inject.Inject

class SettingsRepositoryImpl @Inject constructor(
    private val appDataStore: AppDataStore
) : SettingsRepository {
    override val minimalRoundsNumber: Flow<Int> = appDataStore.minimalRoundsNumber
    override val snippetLength: Flow<Long> = appDataStore.snippetLength

    override suspend fun setMinimalRoundsNumber(minimalRoundsNumber: Int) {
        appDataStore.setMinimalRoundsNumber(minimalRoundsNumber)
    }

    override suspend fun setSnippetLength(ms: Long) {
        appDataStore.setSnippetLength(ms)
    }
}