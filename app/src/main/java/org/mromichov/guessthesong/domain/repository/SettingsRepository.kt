package org.mromichov.guessthesong.domain.repository

import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val roundsNumber: Flow<Int>
    val snippetLength: Flow<Long>

    suspend fun setRoundsNumber(roundsNumber: Int)
    suspend fun setSnippetLength(ms: Long)
}